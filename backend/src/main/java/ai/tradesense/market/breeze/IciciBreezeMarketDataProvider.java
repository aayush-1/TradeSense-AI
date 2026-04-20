package ai.tradesense.market.breeze;

import ai.tradesense.config.BreezeProperties;
import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.MarketDataProvider;
import ai.tradesense.universe.UniverseProvider;
import com.breeze.breezeconnect.BreezeConnect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NSE cash equity daily OHLC via ICICI Breeze {@code getHistoricalDatav2}.
 * <p>
 * Requires a fresh {@code api-session} from the ICICI browser login callback; session handling is ICICI-defined.
 */
public final class IciciBreezeMarketDataProvider implements MarketDataProvider {

    private static final DateTimeFormatter BREEZE_INSTANT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final BreezeProperties properties;
    private final UniverseProvider universeProvider;
    private final ObjectMapper objectMapper;
    private volatile BreezeConnect breeze;

    public IciciBreezeMarketDataProvider(
            BreezeProperties properties,
            UniverseProvider universeProvider,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.universeProvider = universeProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Ohlc> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be <= to");
        }
        try {
            var raw = connectAndFetch(symbol.trim().toUpperCase(), from, to);
            JsonNode root = objectMapper.readTree(raw.toString());
            return BreezeHistoricalParser.parse(root, symbol.trim().toUpperCase(), objectMapper);
        } catch (Exception e) {
            throw new IllegalStateException("Breeze request failed for " + symbol, e);
        }
    }

    @Override
    public Ohlc getLatestData(String symbol) {
        List<Ohlc> rows = getHistoricalData(symbol, LocalDate.now().minusDays(14), LocalDate.now());
        if (rows.isEmpty()) {
            throw new IllegalStateException("No Breeze bars for " + symbol);
        }
        return rows.get(rows.size() - 1);
    }

    @Override
    public List<String> getSupportedSymbols() {
        return universeProvider.getSymbols();
    }

    private org.json.JSONObject connectAndFetch(String stockCode, LocalDate from, LocalDate to) throws Exception {
        BreezeConnect client = ensureClient();
        String fromIso = BREEZE_INSTANT.format(from.atStartOfDay(ZoneOffset.UTC).toInstant());
        String toIso = BREEZE_INSTANT.format(LocalDateTime.of(to, LocalTime.of(23, 59, 59))
                .atZone(ZoneOffset.UTC)
                .toInstant());
        synchronized (client) {
            return client.getHistoricalDatav2(
                    "1day",
                    fromIso,
                    toIso,
                    stockCode,
                    "NSE",
                    "cash",
                    "",
                    "",
                    "");
        }
    }

    private BreezeConnect ensureClient() throws Exception {
        BreezeConnect c = breeze;
        if (c != null) {
            return c;
        }
        synchronized (this) {
            if (breeze != null) {
                return breeze;
            }
            if (!properties.isRunnable()) {
                throw new IllegalStateException("Breeze is not configured (set tradesense.breeze.* / env vars).");
            }
            BreezeConnect b = new BreezeConnect(properties.apiKey());
            b.generateSession(properties.secretKey(), properties.apiSession());
            breeze = b;
            return breeze;
        }
    }
}
