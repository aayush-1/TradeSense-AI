package ai.tradesense.recommendation.technical;

import ai.tradesense.domain.Ohlc;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OhlcIndicatorsTest {

    private static Ohlc o(String sym, LocalDate d, double o, double h, double l, double c, long v) {
        return new Ohlc(sym, d, o, h, l, c, v);
    }

    @Test
    void smaLast_threeCloses() {
        double[] c = {10, 20, 30};
        assertEquals(20.0, OhlcIndicators.smaLast(c, 3), 1e-9);
        assertEquals(30.0, OhlcIndicators.smaLast(c, 1), 1e-9);
    }

    @Test
    void emaLast_matchesSmaSeedThenSmooths() {
        double[] c = {1, 2, 3, 4, 5};
        double ema = OhlcIndicators.emaLast(c, 3);
        assertTrue(Double.isFinite(ema));
        assertEquals(4.0, ema, 1e-9);
    }

    @Test
    void maxHighExcludingLast_ignoresFinalBar() {
        List<Ohlc> bars = new ArrayList<>();
        bars.add(o("X", LocalDate.of(2025, 1, 1), 1, 10, 1, 5, 100));
        bars.add(o("X", LocalDate.of(2025, 1, 2), 1, 20, 1, 5, 100));
        bars.add(o("X", LocalDate.of(2025, 1, 3), 1, 5, 1, 3, 100));
        assertEquals(20.0, OhlcIndicators.maxHighExcludingLast(bars, 20), 1e-9);
    }

    @Test
    void typicalPrice_andRollingVwapProxy() {
        List<Ohlc> bars = new ArrayList<>();
        bars.add(o("X", LocalDate.of(2025, 1, 1), 0, 10, 0, 6, 100));
        assertEquals(16.0 / 3.0, OhlcIndicators.typicalPrice(bars.get(0)), 1e-9);
        bars.add(o("X", LocalDate.of(2025, 1, 2), 0, 12, 2, 8, 200));
        double vwap = OhlcIndicators.rollingVwapProxyLast(bars, 2);
        double tp0 = 16.0 / 3.0;
        double tp1 = 22.0 / 3.0;
        double expected = (tp0 * 100 + tp1 * 200) / 300.0;
        assertEquals(expected, vwap, 1e-9);
    }

    @Test
    void chaikinMultiplier_zeroWhenNoRange() {
        Ohlc flat = o("X", LocalDate.of(2025, 1, 1), 5, 5, 5, 5, 1000);
        assertEquals(0.0, OhlcIndicators.chaikinMoneyFlowMultiplier(flat), 1e-9);
    }

    @Test
    void chaikinMoneyFlowRatio_closeAtHigh() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bars.add(o("X", LocalDate.of(2025, 1, 1 + i), 1, 10, 1, 10, 100));
        }
        double cmf = OhlcIndicators.chaikinMoneyFlowRatio(bars, 5);
        assertEquals(1.0, cmf, 1e-9);
    }

    @Test
    void rsiLast_flatSeriesNearFifty() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            bars.add(o("X", LocalDate.of(2025, 1, 1).plusDays(i), 10, 10, 10, 10, 100));
        }
        double[] c = OhlcIndicators.closesAscending(bars);
        double rsi = OhlcIndicators.rsiLast(c, 14);
        assertEquals(50.0, rsi, 1e-9);
    }

    @Test
    void atrLast_positiveWithWicks() {
        List<Ohlc> bars = new ArrayList<>();
        bars.add(o("X", LocalDate.of(2025, 1, 1), 10, 12, 9, 10, 100));
        bars.add(o("X", LocalDate.of(2025, 1, 2), 10, 15, 10, 11, 100));
        double atr = OhlcIndicators.atrLast(bars, 1);
        assertTrue(atr > 0);
    }

    @Test
    void avgVolumeExcludingLast_ignoresFinalBar() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            bars.add(o("X", LocalDate.of(2025, 1, 1).plusDays(i), 1, 2, 1, 1, 100));
        }
        bars.add(o("X", LocalDate.of(2025, 1, 1).plusDays(21), 1, 2, 1, 1, 9_999_999));
        assertEquals(100.0, OhlcIndicators.avgVolumeExcludingLast(bars, 20), 1e-9);
    }

    @Test
    void minLowExcludingLast_usesPriorBarsOnly() {
        List<Ohlc> bars = new ArrayList<>();
        bars.add(o("X", LocalDate.of(2025, 1, 1), 5, 10, 3, 5, 100));
        bars.add(o("X", LocalDate.of(2025, 1, 2), 5, 10, 7, 5, 100));
        bars.add(o("X", LocalDate.of(2025, 1, 3), 5, 10, 1, 5, 100));
        assertEquals(3.0, OhlcIndicators.minLowExcludingLast(bars, 5), 1e-9);
    }
}
