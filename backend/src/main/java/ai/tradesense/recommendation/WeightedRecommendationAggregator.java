package ai.tradesense.recommendation;

import ai.tradesense.web.dto.OverallRecommendation;
import ai.tradesense.web.dto.StrategyRecommendation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines per-strategy {@code buy} flags using a weighted fraction over rows with {@code includedInAggregation}:
 * {@code sum(weight × buy) / sum(weight)}. Strategies that abstain (insufficient data, errors) do not add sell
 * pressure. Overall {@code buy} is true when that fraction is at or above the configured threshold (default 0.5).
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
        int skipped = 0;
        for (StrategyRecommendation s : strategies) {
            if (!s.includedInAggregation()) {
                skipped++;
                continue;
            }
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
            List<String> msg = new ArrayList<>();
            if (skipped == strategies.size()) {
                msg.add(
                        "No strategies had enough data to include in aggregation (all abstained); overall buy is false.");
            } else {
                msg.add("All strategy weights are zero or invalid among included strategies; cannot aggregate.");
            }
            return new OverallRecommendation(false, 0.0, METHOD_WEIGHTED_FRACTION, List.copyOf(msg));
        }

        double score = weightedBuys / sumWeight;
        boolean buy = score >= buyThreshold;
        List<String> rationale = new ArrayList<>();
        rationale.add(
                String.format(
                        "Aggregation: %s — weighted buy fraction %.4f over included strategies (threshold %.4f).",
                        METHOD_WEIGHTED_FRACTION, score, buyThreshold));
        if (skipped > 0) {
            rationale.add(
                    skipped == 1
                            ? "1 strategy was excluded (insufficient data or error)."
                            : String.format(
                                    "%d strategies were excluded (insufficient data or error).", skipped));
        }
        return new OverallRecommendation(buy, score, METHOD_WEIGHTED_FRACTION, List.copyOf(rationale));
    }
}
