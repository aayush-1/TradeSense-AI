package ai.tradesense.web.dto;

import java.util.List;

/**
 * Recommendations-only payload: one row per evaluated symbol (each with per-strategy rows plus a weighted overall) plus
 * fetch errors. No OHLC series. {@code universe} matches {@code GET /api/v1/universe}.
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
