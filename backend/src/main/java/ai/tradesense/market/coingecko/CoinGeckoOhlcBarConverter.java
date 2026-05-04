package ai.tradesense.market.coingecko;

import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

final class CoinGeckoOhlcBarConverter {

    private CoinGeckoOhlcBarConverter() {
    }

    static List<Ohlc> convert(String symbol, JsonNode root) {
        if (root == null || !root.isArray()) {
            return List.of();
        }
        List<Ohlc> rows = new ArrayList<>();
        for (JsonNode row : root) {
            if (!row.isArray() || row.size() < 5) {
                continue;
            }
            long timestampMs = row.path(0).asLong();
            LocalDate date = Instant.ofEpochMilli(timestampMs).atZone(ZoneOffset.UTC).toLocalDate();
            rows.add(new Ohlc(
                    symbol,
                    date,
                    row.path(1).asDouble(),
                    row.path(2).asDouble(),
                    row.path(3).asDouble(),
                    row.path(4).asDouble(),
                    0L));
        }
        return List.copyOf(rows);
    }
}
