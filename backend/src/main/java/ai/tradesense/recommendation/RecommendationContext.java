package ai.tradesense.recommendation;

import ai.tradesense.domain.Ohlc;

import java.util.List;

/** Inputs passed to each {@link RecommendationStrategy} (OHLC is not exposed on the HTTP API). */
public record RecommendationContext(String symbol, List<Ohlc> ohlcSeries, Double referencePrice) {
    public RecommendationContext {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol required");
        }
        ohlcSeries = ohlcSeries == null ? List.of() : List.copyOf(ohlcSeries);
    }
}
