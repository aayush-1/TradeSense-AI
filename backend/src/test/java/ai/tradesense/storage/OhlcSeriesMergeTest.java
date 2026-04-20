package ai.tradesense.storage;

import ai.tradesense.domain.Ohlc;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OhlcSeriesMergeTest {

    @Test
    void mergeIncomingOverwritesSameDate() {
        List<Ohlc> existing = List.of(
                new Ohlc("R", LocalDate.of(2024, 1, 1), 1, 2, 1, 1.5, 100),
                new Ohlc("R", LocalDate.of(2024, 1, 2), 2, 3, 2, 2.5, 200));
        List<Ohlc> incoming = List.of(
                new Ohlc("R", LocalDate.of(2024, 1, 2), 9, 9, 9, 9, 999));
        List<Ohlc> m = OhlcSeriesMerge.mergePreferIncoming(existing, incoming);
        assertThat(m).hasSize(2);
        assertThat(m.get(1).close()).isEqualTo(9.0);
    }

    @Test
    void trimNotBefore() {
        List<Ohlc> bars = List.of(
                new Ohlc("R", LocalDate.of(2024, 1, 1), 1, 1, 1, 1, 1),
                new Ohlc("R", LocalDate.of(2024, 2, 1), 2, 2, 2, 2, 2));
        List<Ohlc> t = OhlcSeriesMerge.trimNotBefore(bars, LocalDate.of(2024, 2, 1));
        assertThat(t).hasSize(1);
        assertThat(t.get(0).date()).isEqualTo(LocalDate.of(2024, 2, 1));
    }
}
