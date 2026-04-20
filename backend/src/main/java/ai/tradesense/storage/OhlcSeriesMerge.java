package ai.tradesense.storage;

import ai.tradesense.domain.Ohlc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge incremental Yahoo fetches with stored bars: same calendar {@link Ohlc#date()} → incoming replaces stored.
 */
public final class OhlcSeriesMerge {

    private OhlcSeriesMerge() {
    }

    public static List<Ohlc> mergePreferIncoming(List<Ohlc> existing, List<Ohlc> incoming) {
        Map<LocalDate, Ohlc> byDate = new LinkedHashMap<>();
        for (Ohlc o : existing) {
            byDate.put(o.date(), o);
        }
        for (Ohlc o : incoming) {
            byDate.put(o.date(), o);
        }
        List<Ohlc> out = new ArrayList<>(byDate.values());
        out.sort((a, b) -> a.date().compareTo(b.date()));
        return List.copyOf(out);
    }

    public static List<Ohlc> trimNotBefore(List<Ohlc> bars, LocalDate minDateInclusive) {
        return bars.stream().filter(b -> !b.date().isBefore(minDateInclusive)).toList();
    }
}
