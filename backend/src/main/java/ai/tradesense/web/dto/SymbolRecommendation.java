package ai.tradesense.web.dto;

import java.util.List;

/**
 * Per-symbol recommendation: each {@link StrategyRecommendation} plus an {@link OverallRecommendation} from the aggregator.
 * OHLC is not exposed; {@code referencePrice} is typically the latest close used for evaluation.
 */
public record SymbolRecommendation(
        String symbol,
        Double referencePrice,
        OverallRecommendation overall,
        List<StrategyRecommendation> strategies,
        List<String> notes
) {
}
