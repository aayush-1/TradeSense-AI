package ai.tradesense.recommendation;

import ai.tradesense.web.dto.OverallRecommendation;
import ai.tradesense.web.dto.StrategyRecommendation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines per-strategy {@code buy} flags using a weighted fraction: {@code sum(weight × buy) / sum(weight)}.
 * Overall {@code buy} is true when that fraction is at or above the configured threshold (default 0.5).
 */
@Component
public class WeightedRecommendationAggregator {

    public static final String METHOD_WEIGHTED_FRACTION = "weighted_buy_fraction";

    public OverallRecommendation aggregate(List<StrategyRecommendation> strategies, double buyThreshold) {
        if (strategies == null || strategies.isEmpty()) {
            return new OverallRecommendation(
                    false,
                    0.0,
                    METHOD_WEIGHTED_FRACTION,
                    List.of("No strategies were executed for this symbol."));
        }

        double sumWeight = 0.0;
        double weightedBuys = 0.0;
        for (StrategyRecommendation s : strategies) {
            double w = s.weight();
            if (w <= 0.0 || Double.isNaN(w)) {
                continue;
            }
            sumWeight += w;
            if (s.buy()) {
                weightedBuys += w;
            }
        }

        if (sumWeight <= 0.0) {
            return new OverallRecommendation(
                    false,
                    0.0,
                    METHOD_WEIGHTED_FRACTION,
                    List.of("All strategy weights are zero or invalid; cannot aggregate."));
        }

        double score = weightedBuys / sumWeight;
        boolean buy = score >= buyThreshold;
        List<String> rationale = new ArrayList<>();
        rationale.add(
                String.format(
                        "Aggregation: %s — weighted buy fraction %.4f (threshold %.4f).",
                        METHOD_WEIGHTED_FRACTION, score, buyThreshold));
        return new OverallRecommendation(buy, score, METHOD_WEIGHTED_FRACTION, List.copyOf(rationale));
    }
}
