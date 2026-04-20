package ai.tradesense.market.yahoo;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.MarketDataProvider;
import ai.tradesense.universe.UniverseProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * OHLC from Yahoo Finance chart API. NSE symbols in the universe are requested as {@code SYMBOL.NS}.
 */
public final class YahooFinanceMarketDataProvider implements MarketDataProvider {

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
        long period1 = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
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
        List<Ohlc> recent = getHistoricalData(symbol, LocalDate.now().minusDays(14), LocalDate.now());
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
        } catch (RestClientException e) {
            throw new IllegalStateException("Yahoo request failed for " + yahooTicker, e);
        }
    }

    static String toYahooTicker(String nseSymbolUpper) {
        String s = nseSymbolUpper.trim().toUpperCase();
        if (s.endsWith(".NS")) {
            return s;
        }
        return s + ".NS";
    }

    static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol required");
        }
        String s = symbol.trim().toUpperCase();
        if (s.endsWith(".NS")) {
            return s.substring(0, s.length() - 3);
        }
        return s;
    }
}
