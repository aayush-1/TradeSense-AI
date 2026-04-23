package ai.tradesense.web.dto;

import java.util.List;

/**
 * Per-symbol recommendation: each {@link StrategyRecommendation} plus an {@link OverallRecommendation} from the aggregator.
 * OHLC is not exposed; {@code referencePrice} is typically the latest close used for evaluation.
 * {@code tradeLevels} lists every placement method that produced levels when overall is a buy; otherwise empty.
 */
public record SymbolRecommendation(
        String symbol,
        Double referencePrice,
        OverallRecommendation overall,
        List<StrategyRecommendation> strategies,
        List<String> notes,
        List<TradeLevelsSuggestion> tradeLevels
) {
}
