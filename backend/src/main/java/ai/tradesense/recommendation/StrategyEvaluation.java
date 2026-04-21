package ai.tradesense.recommendation;

import java.util.List;

/** Raw outcome from a single strategy before aggregation and HTTP mapping. */
public record StrategyEvaluation(boolean buy, List<String> rationale) {
    public StrategyEvaluation {
        rationale = rationale == null ? List.of() : List.copyOf(rationale);
    }
}
