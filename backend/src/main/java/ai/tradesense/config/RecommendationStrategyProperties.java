package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Weights per {@link ai.tradesense.recommendation.RecommendationStrategy#strategyId()} and overall aggregation threshold.
 */
@ConfigurationProperties(prefix = "tradesense.recommendations")
public class RecommendationStrategyProperties {

    /**
     * Weighted buy fraction must be at or above this value for {@link ai.tradesense.web.dto.OverallRecommendation#buy()}
     * to be true.
     */
    private double aggregationBuyThreshold = 0.5;

    /**
     * Optional weights keyed by {@code strategyId}; missing keys fall back to {@link
     * ai.tradesense.recommendation.RecommendationStrategy#defaultWeight()}.
     */
    private Map<String, Double> strategyWeights = new LinkedHashMap<>();

    public double getAggregationBuyThreshold() {
        return aggregationBuyThreshold;
    }

    public void setAggregationBuyThreshold(double aggregationBuyThreshold) {
        this.aggregationBuyThreshold = aggregationBuyThreshold;
    }

    public Map<String, Double> getStrategyWeights() {
        return strategyWeights;
    }

    public void setStrategyWeights(Map<String, Double> strategyWeights) {
        this.strategyWeights = strategyWeights != null ? strategyWeights : new LinkedHashMap<>();
    }

    public double resolveWeight(String strategyId, double defaultWeight) {
        Double w = strategyWeights.get(strategyId);
        if (w == null || w.isNaN()) {
            return defaultWeight;
        }
        return w;
    }
}
