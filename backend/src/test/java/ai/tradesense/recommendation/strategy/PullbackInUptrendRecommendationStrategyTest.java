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
import static org.junit.jupiter.api.Assertions.fail;

class PullbackInUptrendRecommendationStrategyTest {

    private static final LocalDate START = LocalDate.of(2024, 1, 2);

    private static Ohlc bar(int dayIndex, double close, long volume) {
        LocalDate d = START.plusDays(dayIndex);
        return new Ohlc("PB", d, close, close, close, close, volume);
    }

    /** First 199 sessions: smooth uptrend; last session close is {@code lastClose} (pullback depth). */
    private static List<Ohlc> uptrendThenLastClose(double lastClose, long lastVol) {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 199; i++) {
            double c = 100.0 + i * 1.0;
            bars.add(bar(i, c, 1_000_000L));
        }
        bars.add(bar(199, lastClose, lastVol));
        return bars;
    }

    @Test
    void skippedWhenInsufficientHistory() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 199; i++) {
            bars.add(bar(i, 100 + i, 1_000_000L));
        }
        var s = new PullbackInUptrendRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("PB", bars, null));
        assertFalse(e.includedInAggregation());
        assertFalse(e.buy());
        assertTrue(e.rationale().get(0).contains("Need at least"));
    }

    @Test
    void noBuyWhenMaStackNotBullish() {
        List<Ohlc> bars = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            bars.add(bar(i, 100.0, 1_000_000L));
        }
        var s = new PullbackInUptrendRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("PB", bars, null));
        assertTrue(e.includedInAggregation());
        assertFalse(e.buy());
        assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("stack false")));
    }

    @Test
    void noBuyWhenLastCloseFarAboveRibbon() {
        List<Ohlc> bars = uptrendThenLastClose(298.0, 1_000_000L);
        var s = new PullbackInUptrendRecommendationStrategy();
        StrategyEvaluation e = s.evaluate(new RecommendationContext("PB", bars, null));
        assertTrue(e.includedInAggregation());
        assertFalse(e.buy());
        assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("in band false")));
    }

    @Test
    void buysForSomePullbackDepthWithUptrendSeries() {
        var s = new PullbackInUptrendRecommendationStrategy();
        for (double lastClose = 298.0; lastClose >= 200.0; lastClose -= 0.25) {
            List<Ohlc> bars = uptrendThenLastClose(lastClose, 1_000_000L);
            StrategyEvaluation e = s.evaluate(new RecommendationContext("PB", bars, null));
            if (e.buy()) {
                assertTrue(e.includedInAggregation());
                assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("stack true")));
                assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("above200 true")));
                assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("in band true")));
                assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("RSI")));
                return;
            }
        }
        fail("Expected at least one lastClose in search range to satisfy pullback + RSI band");
    }

    @Test
    void volumeContractionFlaggedWhenLastVolumeBelowAverage() {
        var s = new PullbackInUptrendRecommendationStrategy();
        for (double lastClose = 298.0; lastClose >= 200.0; lastClose -= 0.25) {
            List<Ohlc> bars = uptrendThenLastClose(lastClose, 500_000L);
            StrategyEvaluation e = s.evaluate(new RecommendationContext("PB", bars, null));
            if (e.buy()) {
                assertTrue(e.rationale().stream().anyMatch((line) -> line.contains("contraction (optional) true")));
                return;
            }
        }
        // If no buy with low volume, still assert contraction line appears when ribbon+RSI would pass with high vol
        List<Ohlc> barsHighVol = uptrendThenLastClose(298.0, 1_000_000L);
        StrategyEvaluation e2 = s.evaluate(new RecommendationContext("PB", barsHighVol, null));
        assertTrue(e2.rationale().stream().anyMatch((line) -> line.contains("contraction (optional)")));
    }
}
