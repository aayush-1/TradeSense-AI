package ai.tradesense.market.yahoo;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.MarketDataProvider;
import ai.tradesense.universe.UniverseProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OHLC from Yahoo Finance chart API. NSE symbols in the universe are requested as {@code SYMBOL.NS}.
 */
public final class YahooFinanceMarketDataProvider implements MarketDataProvider {

    private static final ZoneId NSE_CALENDAR = ZoneId.of("Asia/Kolkata");
    private static final long MIN_REQUEST_GAP_MS = 350L;
    private static final int MAX_ATTEMPTS = 6;
    private static final AtomicLong LAST_REQUEST_MS = new AtomicLong(0L);

    private final RestClient restClient;
    private final UniverseProvider universeProvider;
    private final ObjectMapper objectMapper;

    public YahooFinanceMarketDataProvider(
            RestClient restClient,
            UniverseProvider universeProvider,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.universeProvider = universeProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Ohlc> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        String sym = normalizeSymbol(symbol);
        String yahooTicker = toYahooTicker(sym);
        LocalDate chartFrom = chartPeriodStartInclusive(from, to);
        long period1 = chartFrom.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long period2 = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;
        if (period1 > period2) {
            throw new IllegalArgumentException("from must be <= to");
        }
        String body = fetchChartBody(yahooTicker, period1, period2);
        try {
            JsonNode root = objectMapper.readTree(body);
            return YahooChartJsonParser.parseBars(root, sym);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Yahoo response for " + sym, e);
        }
    }

    @Override
    public Ohlc getLatestData(String symbol) {
        LocalDate today = LocalDate.now(NSE_CALENDAR);
        List<Ohlc> recent = getHistoricalData(symbol, today.minusDays(14), today);
        if (recent.isEmpty()) {
            throw new IllegalStateException("No recent bars for " + normalizeSymbol(symbol));
        }
        return recent.get(recent.size() - 1);
    }

    @Override
    public List<String> getSupportedSymbols() {
        return universeProvider.getSymbols();
    }

    private String fetchChartBody(String yahooTicker, long period1, long period2) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            throttle();
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/{ticker}")
                                .queryParam("period1", period1)
                                .queryParam("period2", period2)
                                .queryParam("interval", "1d")
                                .build(yahooTicker))
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                            throw new IllegalStateException(
                                    "Yahoo HTTP " + res.getStatusCode() + " for " + yahooTicker);
                        })
                        .body(String.class);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 429 && attempt < MAX_ATTEMPTS) {
                    sleep(retryDelayMs(ex, attempt));
                    continue;
                }
                throw new IllegalStateException(
                        "Yahoo request failed for " + yahooTicker + " (HTTP " + ex.getStatusCode().value() + ")",
                        ex);
            } catch (RestClientException ex) {
                if (attempt < MAX_ATTEMPTS) {
                    sleep(400L * attempt);
                    continue;
                }
                throw new IllegalStateException("Yahoo request failed for " + yahooTicker, ex);
            }
        }
        throw new IllegalStateException("Yahoo request failed for " + yahooTicker + " after retries");
    }

    public static String toYahooTicker(String nseSymbolUpper) {
        String s = nseSymbolUpper.trim().toUpperCase();
        if (s.endsWith(".NS")) {
            return s;
        }
        return s + ".NS";
    }

    /**
     * Inclusive start date for Yahoo {@code period1} (UTC midnight). When {@code from} equals {@code to}, asks from
     * one day earlier so the chart window is not a single UTC day (Yahoo has returned HTTP 400 for some tickers).
     */
    static LocalDate chartPeriodStartInclusive(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be <= to");
        }
        if (from.equals(to) && from.isAfter(LocalDate.MIN)) {
            return from.minusDays(1);
        }
        return from;
    }

    public static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol required");
        }
        String s = symbol.trim().toUpperCase();
        if (s.endsWith(".NS")) {
            return s.substring(0, s.length() - 3);
        }
        return s;
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

    private static long retryDelayMs(RestClientResponseException ex, int attempt) {
        String retryAfter = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null;
        if (retryAfter != null) {
            try {
                long seconds = Long.parseLong(retryAfter.trim());
                if (seconds > 0) {
                    return seconds * 1_000L;
                }
            } catch (NumberFormatException ignored) {
                // fallback below
            }
        }
        return 2_000L + (1_000L * attempt);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Yahoo backoff", ie);
        }
    }
}
