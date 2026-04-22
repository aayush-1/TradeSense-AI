package ai.tradesense.recommendation.strategy;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.StrategyEvaluation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiveRecommendationStrategiesTest {

    private static Ohlc bar(LocalDate d, double o, double h, double l, double c, long v) {
        return new Ohlc("X", d, o, h, l, c, v);
    }

    private static LocalDate d0() {
        return LocalDate.of(2025, 1, 2);
    }

    @Test
    void breakoutVolume_insufficientBars() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            bars.add(bar(d0().plusDays(i), 1, 2, 1, 1, 100));
        }
        var s = new BreakoutVolumeRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertFalse(e.buy());
        assertTrue(e.rationale().get(0).contains("Need at least"));
    }

    @Test
    void breakoutVolume_passesOnBreakoutAndVolumeSpike() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            bars.add(bar(d0().plusDays(i), 1, 10, 1, 9, 1000));
        }
        bars.add(bar(d0().plusDays(21), 1, 12, 1, 11, 3000));
        var s = new BreakoutVolumeRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertTrue(e.buy());
    }

    @Test
    void trendMaCross_insufficientBars() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 199; i++) {
            bars.add(bar(d0().plusDays(i), 1, 2, 1, 1 + i, 100));
        }
        var s = new TrendMaCrossRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertFalse(e.buy());
        assertTrue(e.rationale().get(0).contains("Insufficient history"));
    }

    @Test
    void trendMaCross_passesOnLinearUptrend() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            bars.add(bar(d0().plusDays(i), 1, 2, 1, 1 + i, 100));
        }
        var s = new TrendMaCrossRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertTrue(e.buy());
    }

    @Test
    void meanRevertVwapProxy_passesAfterLateBreakdownWithBullStack() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 170; i++) {
            bars.add(bar(d0().plusDays(i), 100, 100, 100, 100, 1_000_000));
        }
        for (int i = 170; i < 199; i++) {
            bars.add(bar(d0().plusDays(i), 200, 200, 200, 200, 1_000_000));
        }
        bars.add(bar(d0().plusDays(199), 200, 200, 100, 120, 1_000_000));
        var s = new MeanRevertVwapProxyRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertTrue(e.buy(), () -> String.join("; ", e.rationale()));
    }

    @Test
    void rsiVolume_passesOnOversoldVolumeAndCloseAboveSwingLow() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            bars.add(bar(d0().plusDays(i), 100, 100, 100, 100, 1000));
        }
        for (int i = 7; i < 21; i++) {
            double c = 100 - (i - 7) * 4.0;
            bars.add(bar(d0().plusDays(i), c, c, c - 0.5, c, 1000));
        }
        bars.add(bar(d0().plusDays(21), 30, 35, 28, 50, 5000));
        var s = new RsiVolumeRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertTrue(e.buy(), () -> String.join("; ", e.rationale()));
    }

    @Test
    void rsiVolume_failsWhenCloseBelowPriorSwingLow() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            bars.add(bar(d0().plusDays(i), 100, 100, 100, 100, 1000));
        }
        for (int i = 7; i < 21; i++) {
            double c = 100 - (i - 7) * 4.0;
            bars.add(bar(d0().plusDays(i), c, c, c - 0.5, c, 1000));
        }
        bars.add(bar(d0().plusDays(21), 30, 35, 10, 15, 5000));
        var s = new RsiVolumeRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertFalse(e.buy());
    }

    @Test
    void accumulationPressure_passesWhenCmfPositiveAndRising() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bars.add(bar(d0().plusDays(i), 10, 11, 9, 10, 1000));
        }
        for (int i = 5; i < 25; i++) {
            bars.add(bar(d0().plusDays(i), 10, 20, 10, 19, 1000 + i * 200L));
        }
        var s = new AccumulationPressureRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("X", bars, null));
        assertTrue(e.buy(), () -> String.join("; ", e.rationale()));
    }
}
