package ai.tradesense.recommendation.strategy;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.RecommendationStrategy;
import ai.tradesense.recommendation.StrategyEvaluation;
import ai.tradesense.recommendation.technical.OhlcIndicators;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Long-horizon trend: 50 SMA above 200 SMA and price above 50 SMA (daily). */
@Component
public class TrendMaCrossRecommendationStrategy implements RecommendationStrategy {

    static final int SMA_FAST = 50;
    static final int SMA_SLOW = 200;
    static final int MIN_BARS = 200;

    @Override
    public String strategyId() {
        return "trend-ma-cross-v1";
    }

    @Override
    public String displayName() {
        return "MA stack / golden trend";
    }

    @Override
    public double defaultWeight() {
        return 1.5;
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        List<Ohlc> bars = context.ohlcSeries();
        List<String> rationale = new ArrayList<>();
        if (bars.size() < MIN_BARS) {
            rationale.add("Insufficient history for " + SMA_SLOW + "-day SMA; have " + bars.size() + " bars (need " + MIN_BARS + ").");
            return StrategyEvaluation.skipped(rationale);
        }
        double[] closes = OhlcIndicators.closesAscending(bars);
        double sma50 = OhlcIndicators.smaLast(closes, SMA_FAST);
        double sma200 = OhlcIndicators.smaLast(closes, SMA_SLOW);
        double lastClose = closes[closes.length - 1];
        if (Double.isNaN(sma50) || Double.isNaN(sma200)) {
            rationale.add("SMA not computable.");
            return StrategyEvaluation.skipped(rationale);
        }
        boolean stack = sma50 > sma200;
        boolean above50 = lastClose > sma50;
        rationale.add(String.format("SMA%d=%.4f, SMA%d=%.4f, close=%.4f.", SMA_FAST, sma50, SMA_SLOW, sma200, lastClose));
        rationale.add(stack ? "50 above 200 (bullish stack)." : "50 not above 200.");
        rationale.add(above50 ? "Close above 50 SMA." : "Close not above 50 SMA.");
        return StrategyEvaluation.counted(stack && above50, rationale);
    }
}
