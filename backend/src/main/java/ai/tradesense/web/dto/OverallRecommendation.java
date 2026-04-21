package ai.tradesense.web.dto;

import java.util.List;

/**
 * Combined view across all {@link StrategyRecommendation} rows for one symbol (weighted aggregation).
 */
public record OverallRecommendation(
        boolean buy,
        double weightedScore,
        String aggregationMethod,
        List<String> rationale
) {
}
