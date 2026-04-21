package ai.tradesense.web.dto;

import java.util.List;

/** One registered strategy's recommendation for a symbol. */
public record StrategyRecommendation(
        String strategyId,
        String displayName,
        boolean buy,
        double weight,
        List<String> rationale
) {
}
