package ai.tradesense.market.breeze;

import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps Breeze {@code getHistoricalDatav2} JSON to {@link Ohlc} (daily bars).
 */
public final class BreezeHistoricalParser {

    private static final DateTimeFormatter[] DATE_TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private BreezeHistoricalParser() {
    }

    /**
     * @param response root JSON from Breeze (often produced via {@code objectMapper.readTree(breezeJson.toString())})
     */
    public static List<Ohlc> parse(JsonNode response, String canonicalSymbol, ObjectMapper mapper) {
        String sym = canonicalSymbol.trim().toUpperCase();
        if (response.path("Status").isNumber() && response.get("Status").asInt() != 200) {
            throw new IllegalStateException("Breeze Status "
                    + response.get("Status").asInt() + ": " + response.path("Error").asText(response.toString()));
        }
        JsonNode err = response.get("Error");
        if (err != null && !err.isNull() && err.isTextual() && !err.asText().isBlank()) {
            throw new IllegalStateException("Breeze Error: " + err.asText());
        }
        JsonNode rows = normalizeSuccessArray(response, mapper);
        List<Ohlc> out = new ArrayList<>();
        for (JsonNode row : rows) {
            if (!row.isObject()) {
                continue;
            }
            LocalDate date = parseRowDate(row.get("datetime").asText());
            double open = row.get("open").asDouble();
            double high = row.get("high").asDouble();
            double low = row.get("low").asDouble();
            double close = row.get("close").asDouble();
            long volume = row.path("volume").asLong(0L);
            try {
                out.add(new Ohlc(sym, date, open, high, low, close, volume));
            } catch (IllegalArgumentException ignored) {
                // skip malformed bar
            }
        }
        return List.copyOf(out);
    }

    private static JsonNode normalizeSuccessArray(JsonNode response, ObjectMapper mapper) {
        JsonNode succ = response.get("Success");
        if (succ == null || succ.isNull()) {
            return mapper.createArrayNode();
        }
        if (succ.isArray()) {
            return succ;
        }
        if (succ.isTextual()) {
            String t = succ.asText().trim();
            if (t.isEmpty() || "null".equalsIgnoreCase(t)) {
                return mapper.createArrayNode();
            }
            try {
                JsonNode parsed = mapper.readTree(t);
                if (parsed.isArray()) {
                    return parsed;
                }
                if (parsed.isObject()) {
                    return mapper.createArrayNode().add(parsed);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot parse Breeze Success string", e);
            }
        }
        throw new IllegalStateException("Unexpected Breeze Success type: " + succ.getNodeType());
    }

    private static LocalDate parseRowDate(String datetime) {
        String d = datetime.trim();
        for (DateTimeFormatter f : DATE_TIME_FORMATS) {
            try {
                return java.time.LocalDateTime.parse(d, f).toLocalDate();
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        if (d.length() >= 10) {
            return LocalDate.parse(d.substring(0, 10));
        }
        throw new IllegalArgumentException("Cannot parse Breeze datetime: " + datetime);
    }
}
