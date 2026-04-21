package ai.tradesense.recommendation;

/**
 * Pluggable recommendation logic (technical, fundamental, blended, etc.). Register as a Spring bean to participate in
 * {@code GET /recommendations}; all beans implementing this interface are invoked per symbol.
 */
public interface RecommendationStrategy {

    /** Stable id for JSON and {@link ai.tradesense.config.RecommendationStrategyProperties#getStrategyWeights()}. */
    String strategyId();

    /** Human-readable name for API consumers. */
    String displayName();

    /**
     * Default relative weight when no entry exists in {@code tradesense.recommendations.strategy-weights} for this id.
     */
    default double defaultWeight() {
        return 1.0;
    }

    StrategyEvaluation evaluate(RecommendationContext context);
}
