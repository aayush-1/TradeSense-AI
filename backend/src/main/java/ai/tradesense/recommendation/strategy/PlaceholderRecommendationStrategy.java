package ai.tradesense.recommendation.strategy;

import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.RecommendationStrategy;
import ai.tradesense.recommendation.StrategyEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default stub so the pipeline and API shape are valid before real strategies are added. Remove or replace with real
 * implementations.
 */
@Component
public class PlaceholderRecommendationStrategy implements RecommendationStrategy {

    public static final String STRATEGY_ID = "placeholder-v1";

    @Override
    public String strategyId() {
        return STRATEGY_ID;
    }

    @Override
    public String displayName() {
        return "Placeholder strategy";
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        return new StrategyEvaluation(
                false,
                List.of("Replace this bean with real recommendation strategies; no signal computed."));
    }
}
