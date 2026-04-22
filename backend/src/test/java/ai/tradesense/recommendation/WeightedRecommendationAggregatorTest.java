package ai.tradesense.recommendation;

import ai.tradesense.web.dto.OverallRecommendation;
import ai.tradesense.web.dto.StrategyRecommendation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedRecommendationAggregatorTest {

    private final WeightedRecommendationAggregator aggregator = new WeightedRecommendationAggregator();

    @Test
    void majorityBuyAtDefaultThreshold() {
        List<StrategyRecommendation> rows =
                List.of(
                        new StrategyRecommendation("a", "A", true, 1.0, List.of(), true),
                        new StrategyRecommendation("b", "B", true, 1.0, List.of(), true),
                        new StrategyRecommendation("c", "C", false, 1.0, List.of(), true));
        OverallRecommendation o = aggregator.aggregate(rows, 0.5);
        assertTrue(o.buy());
        assertEquals(2.0 / 3.0, o.weightedScore(), 1e-9);
    }

    @Test
    void belowThresholdIsNotBuy() {
        List<StrategyRecommendation> rows =
                List.of(
                        new StrategyRecommendation("a", "A", true, 1.0, List.of(), true),
                        new StrategyRecommendation("b", "B", false, 1.0, List.of(), true),
                        new StrategyRecommendation("c", "C", false, 1.0, List.of(), true));
        OverallRecommendation o = aggregator.aggregate(rows, 0.5);
        assertFalse(o.buy());
        assertEquals(1.0 / 3.0, o.weightedScore(), 1e-9);
    }

    @Test
    void weightsSkewTowardHeavyStrategy() {
        List<StrategyRecommendation> rows =
                List.of(
                        new StrategyRecommendation("a", "A", false, 3.0, List.of(), true),
                        new StrategyRecommendation("b", "B", true, 1.0, List.of(), true));
        OverallRecommendation o = aggregator.aggregate(rows, 0.5);
        assertFalse(o.buy());
        assertEquals(0.25, o.weightedScore(), 1e-9);
    }

    @Test
    void emptyStrategies() {
        OverallRecommendation o = aggregator.aggregate(List.of(), 0.5);
        assertFalse(o.buy());
        assertEquals(0.0, o.weightedScore());
    }

    @Test
    void excludedStrategiesOmitWeightFromDenominator() {
        List<StrategyRecommendation> rows =
                List.of(
                        new StrategyRecommendation("a", "A", true, 1.0, List.of(), true),
                        new StrategyRecommendation("b", "B", false, 1.0, List.of(), true),
                        new StrategyRecommendation("c", "C", false, 100.0, List.of(), false));
        OverallRecommendation o = aggregator.aggregate(rows, 0.5);
        assertTrue(o.buy());
        assertEquals(0.5, o.weightedScore(), 1e-9);
    }

    @Test
    void allExcludedYieldsNoBuy() {
        List<StrategyRecommendation> rows =
                List.of(
                        new StrategyRecommendation("a", "A", false, 1.0, List.of(), false),
                        new StrategyRecommendation("b", "B", false, 1.0, List.of(), false));
        OverallRecommendation o = aggregator.aggregate(rows, 0.5);
        assertFalse(o.buy());
        assertEquals(0.0, o.weightedScore(), 1e-9);
        assertTrue(o.rationale().get(0).contains("No strategies had enough data"));
    }
}
