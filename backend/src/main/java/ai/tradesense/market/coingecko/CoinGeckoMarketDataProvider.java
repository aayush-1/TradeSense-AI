package ai.tradesense.market.coingecko;

import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CoinGeckoMarketDataProvider {
    private static final long MIN_REQUEST_GAP_MS = 2_500L;
    private static final int MAX_ATTEMPTS = 8;
    private static final AtomicLong LAST_REQUEST_MS = new AtomicLong(0L);

    private static final Map<String, String> SYMBOL_TO_COIN_ID = Map.ofEntries(
            Map.entry("BTC-USD", "bitcoin"),
            Map.entry("ETH-USD", "ethereum"),
            Map.entry("XRP-USD", "ripple"),
            Map.entry("BNB-USD", "binancecoin"),
            Map.entry("SOL-USD", "solana"),
            Map.entry("DOGE-USD", "dogecoin"),
            Map.entry("ADA-USD", "cardano"),
            Map.entry("TRX-USD", "tron"),
            Map.entry("AVAX-USD", "avalanche-2"),
            Map.entry("SHIB-USD", "shiba-inu"),
            Map.entry("DOT-USD", "polkadot"),
            Map.entry("LINK-USD", "chainlink"),
            Map.entry("BCH-USD", "bitcoin-cash"),
            Map.entry("NEAR-USD", "near"),
            Map.entry("LEO-USD", "leo-token"),
            Map.entry("LTC-USD", "litecoin"),
            Map.entry("SUI-USD", "sui"),
            Map.entry("UNI-USD", "uniswap"),
            Map.entry("PEPE-USD", "pepe"),
            Map.entry("APT-USD", "aptos"));

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CoinGeckoMarketDataProvider(
            @Qualifier("coinGeckoRestClient") RestClient restClient,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<Ohlc> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be <= to");
        }
        String normalizedSymbol = symbol.trim().toUpperCase();
        String coinId = SYMBOL_TO_COIN_ID.get(normalizedSymbol);
        if (coinId == null) {
            throw new IllegalArgumentException("Unsupported crypto symbol: " + normalizedSymbol);
        }
        long days = Math.max(1, Math.min(365, ChronoUnit.DAYS.between(from, to) + 1));
        String body = fetchOhlc(coinId, days);
        try {
            JsonNode root = objectMapper.readTree(body);
            List<Ohlc> allRows = CoinGeckoOhlcBarConverter.convert(normalizedSymbol, root);
            return allRows.stream()
                    .filter(bar -> !bar.date().isBefore(from) && !bar.date().isAfter(to))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CoinGecko response for " + normalizedSymbol, e);
        }
    }

    private String fetchOhlc(String coinId, long days) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            throttle();
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/coins/{coinId}/ohlc")
                                .queryParam("vs_currency", "usd")
                                .queryParam("days", days)
                                .build(coinId))
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                            throw new IllegalStateException(
                                    "CoinGecko HTTP " + res.getStatusCode() + " for " + coinId);
                        })
                        .body(String.class);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 429 && attempt < MAX_ATTEMPTS) {
                    sleep(retryDelayMs(ex, attempt));
                    continue;
                }
                throw new IllegalStateException(
                        "CoinGecko request failed for " + coinId + " (HTTP " + ex.getStatusCode().value() + ")",
                        ex);
            } catch (RestClientException ex) {
                if (attempt < MAX_ATTEMPTS) {
                    sleep(700L * attempt);
                    continue;
                }
                throw new IllegalStateException("CoinGecko request failed for " + coinId, ex);
            }
        }
        throw new IllegalStateException("CoinGecko request failed for " + coinId + " after retries");
    }

    private static void throttle() {
        while (true) {
            long now = System.currentTimeMillis();
            long prev = LAST_REQUEST_MS.get();
            long next = Math.max(now, prev + MIN_REQUEST_GAP_MS);
            if (LAST_REQUEST_MS.compareAndSet(prev, next)) {
                long wait = next - now;
                if (wait > 0) {
                    sleep(wait);
                }
                return;
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during CoinGecko backoff", ie);
        }
    }

    private static long retryDelayMs(RestClientResponseException ex, int attempt) {
        String retryAfter = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null;
        if (retryAfter != null) {
            try {
                long seconds = Long.parseLong(retryAfter.trim());
                if (seconds > 0) {
                    return seconds * 1_000L;
                }
            } catch (NumberFormatException ignored) {
                // ignore malformed header and use fallback
            }
        }
        return 8_000L + (2_000L * attempt);
    }
}
