package ai.tradesense.recommendation;

import ai.tradesense.config.AtrVolatilityGateProperties;
import ai.tradesense.domain.Ohlc;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtrVolatilityNotesTest {

    private static Ohlc o(LocalDate d, double c, double h, double l) {
        return new Ohlc("T", d, c, h, l, c, 1_000_000L);
    }

    @Test
    void addsHighNoteWhenAtrPctAboveThreshold() {
        List<Ohlc> bars = new ArrayList<>();
        LocalDate d0 = LocalDate.of(2025, 1, 2);
        for (int i = 0; i < 30; i++) {
            double c = 100 + i * 0.05;
            bars.add(o(d0.plusDays(i), c, c + 25, c - 2));
        }
        AtrVolatilityGateProperties p = new AtrVolatilityGateProperties();
        p.setEnabled(true);
        p.setAtrPeriod(14);
        p.setNoteHighAtrPct(0.02);
        p.setNoteLowAtrPct(0.0001);
        List<String> notes = new ArrayList<>();
        AtrVolatilityNotes.maybeAppend(notes, bars, p);
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("Volatility note"), notes.get(0));
        assertTrue(notes.get(0).contains("large vs price"), notes.get(0));
    }

    @Test
    void addsNothingWhenDisabled() {
        List<Ohlc> bars = new ArrayList<>();
        LocalDate d0 = LocalDate.of(2025, 1, 2);
        for (int i = 0; i < 30; i++) {
            bars.add(o(d0.plusDays(i), 100, 101, 99));
        }
        AtrVolatilityGateProperties p = new AtrVolatilityGateProperties();
        p.setEnabled(false);
        List<String> notes = new ArrayList<>();
        AtrVolatilityNotes.maybeAppend(notes, bars, p);
        assertTrue(notes.isEmpty());
    }
}
