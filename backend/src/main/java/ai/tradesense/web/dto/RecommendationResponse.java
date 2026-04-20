package ai.tradesense.web.dto;

import java.util.List;

/**
 * Recommendations-only payload: one row per evaluated symbol plus fetch errors. No OHLC series or analysis window dates.
 * {@code universe} matches {@code GET /api/v1/universe} (symbols configured for prediction).
 */
public record RecommendationResponse(
        List<String> universe,
        List<SymbolRecommendation> recommendations,
        List<String> dataErrors
) {
    public static RecommendationResponse of(
            List<String> universe,
            List<SymbolRecommendation> recommendations,
            List<String> dataErrors) {
        return new RecommendationResponse(universe, recommendations, dataErrors);
    }
}
