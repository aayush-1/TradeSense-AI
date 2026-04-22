package ai.tradesense.recommendation.strategy;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.RecommendationStrategy;
import ai.tradesense.recommendation.StrategyEvaluation;
import ai.tradesense.recommendation.technical.OhlcIndicators;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Breakout of prior N-session high with volume confirmation (daily bars).
 */
@Component
public class BreakoutVolumeRecommendationStrategy implements RecommendationStrategy {

    static final int PRIOR_HIGH_DAYS = 20;
    static final int VOLUME_AVG_DAYS = 20;
    static final double VOLUME_MULTIPLIER = 1.5;
    static final int MIN_BARS = 22;

    @Override
    public String strategyId() {
        return "breakout-volume-v1";
    }

    @Override
    public String displayName() {
        return "Breakout + volume";
    }

    @Override
    public double defaultWeight() {
        return 1.2;
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        List<Ohlc> bars = context.ohlcSeries();
        List<String> rationale = new ArrayList<>();
        if (bars.size() < MIN_BARS) {
            rationale.add("Need at least " + MIN_BARS + " bars (20 prior for high/volume + today); have " + bars.size() + ".");
            return StrategyEvaluation.skipped(rationale);
        }
        Ohlc last = bars.get(bars.size() - 1);
        double priorHigh = OhlcIndicators.maxHighExcludingLast(bars, PRIOR_HIGH_DAYS);
        double avgVolPrior = OhlcIndicators.avgVolumeExcludingLast(bars, VOLUME_AVG_DAYS);
        if (Double.isNaN(priorHigh) || Double.isNaN(avgVolPrior) || avgVolPrior <= 0) {
            rationale.add("Could not compute prior high or average volume.");
            return StrategyEvaluation.skipped(rationale);
        }
        boolean priceBreak = last.close() > priorHigh;
        boolean volSpike = last.volume() > VOLUME_MULTIPLIER * avgVolPrior;
        rationale.add(String.format("Prior ~%d-session high (excl. today)=%.4f; last close=%.4f.", PRIOR_HIGH_DAYS, priorHigh, last.close()));
        rationale.add(String.format("Avg volume prior %d sessions=%.0f; last volume=%d (need >%.1f×).", VOLUME_AVG_DAYS, avgVolPrior, last.volume(), VOLUME_MULTIPLIER));
        rationale.add(priceBreak ? "Close above prior high." : "Close not above prior high.");
        rationale.add(volSpike ? "Volume above threshold." : "Volume not above threshold.");
        return StrategyEvaluation.counted(priceBreak && volSpike, rationale);
    }
}
