package ai.tradesense.recommendation;

import java.util.List;

/** Raw outcome from a single strategy before aggregation and HTTP mapping. */
public record StrategyEvaluation(boolean buy, List<String> rationale, boolean includedInAggregation) {
    public StrategyEvaluation {
        rationale = rationale == null ? List.of() : List.copyOf(rationale);
    }

    /** Normal rule outcome: participates in {@link WeightedRecommendationAggregator}. */
    public static StrategyEvaluation counted(boolean buy, List<String> rationale) {
        return new StrategyEvaluation(buy, rationale, true);
    }

    /**
     * Shown on the API (e.g. insufficient OHLC history) but omitted from the weighted score so it does not count as a
     * sell vote.
     */
    public static StrategyEvaluation skipped(List<String> rationale) {
        return new StrategyEvaluation(false, rationale, false);
    }
}
