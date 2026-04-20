package ai.tradesense.market.yahoo;

import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Yahoo v8 finance/chart JSON into {@link Ohlc} (NSE calendar day in Asia/Kolkata).
 */
public final class YahooChartJsonParser {

    private static final ZoneId NSE_VIEW_ZONE = ZoneId.of("Asia/Kolkata");

    private YahooChartJsonParser() {
    }

    public static List<Ohlc> parseBarsFromJson(String json, String canonicalSymbol) throws JsonProcessingException {
        return parseBars(new ObjectMapper().readTree(json), canonicalSymbol);
    }

    public static List<Ohlc> parseBars(JsonNode root, String canonicalSymbol) {
        if (canonicalSymbol == null || canonicalSymbol.isBlank()) {
            throw new IllegalArgumentException("canonicalSymbol required");
        }
        String sym = canonicalSymbol.trim().toUpperCase();
        JsonNode chart = root.path("chart");
        JsonNode err = chart.path("error");
        if (!err.isNull() && !err.isMissingNode()) {
            String msg = err.path("description").asText(err.toString());
            throw new IllegalStateException("Yahoo chart error: " + msg);
        }
        JsonNode result = chart.path("result");
        if (!result.isArray() || result.isEmpty()) {
            throw new IllegalStateException("Yahoo chart: empty result for " + sym);
        }
        JsonNode series = result.get(0);
        JsonNode timestamps = series.path("timestamp");
        if (!timestamps.isArray() || timestamps.isEmpty()) {
            return List.of();
        }
        JsonNode quote = series.path("indicators").path("quote").path(0);
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        List<Ohlc> out = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            if (!timestamps.get(i).isNumber()) {
                continue;
            }
            long epoch = timestamps.get(i).asLong();
            LocalDate date = Instant.ofEpochSecond(epoch).atZone(NSE_VIEW_ZONE).toLocalDate();

            if (isNullNode(opens.get(i)) || isNullNode(highs.get(i))
                    || isNullNode(lows.get(i)) || isNullNode(closes.get(i))) {
                continue;
            }
            double open = opens.get(i).asDouble();
            double high = highs.get(i).asDouble();
            double low = lows.get(i).asDouble();
            double close = closes.get(i).asDouble();
            long volume = 0L;
            if (!isNullNode(volumes.get(i)) && volumes.get(i).isNumber()) {
                volume = volumes.get(i).asLong();
            }
            try {
                out.add(new Ohlc(sym, date, open, high, low, close, volume));
            } catch (IllegalArgumentException ignored) {
                // skip invalid bars
            }
        }
        return List.copyOf(out);
    }

    private static boolean isNullNode(JsonNode n) {
        return n == null || n.isNull() || n.isMissingNode();
    }
}
