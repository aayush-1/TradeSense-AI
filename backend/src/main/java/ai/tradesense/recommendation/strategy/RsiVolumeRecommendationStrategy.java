package ai.tradesense.recommendation.strategy;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.RecommendationStrategy;
import ai.tradesense.recommendation.StrategyEvaluation;
import ai.tradesense.recommendation.technical.OhlcIndicators;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Oversold RSI with elevated volume and a mild “not free-falling” low anchor. */
@Component
public class RsiVolumeRecommendationStrategy implements RecommendationStrategy {

    static final int RSI_PERIOD = 14;
    static final double RSI_OVERSOLD = 32.0;
    static final int VOLUME_AVG_DAYS = 20;
    static final double VOLUME_MULTIPLIER = 1.2;
    static final int SWING_LOW_DAYS = 5;
    static final int MIN_BARS = 22;

    @Override
    public String strategyId() {
        return "rsi-volume-v1";
    }

    @Override
    public String displayName() {
        return "RSI + volume stress";
    }

    @Override
    public double defaultWeight() {
        return 1.0;
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        List<Ohlc> bars = context.ohlcSeries();
        List<String> rationale = new ArrayList<>();
        if (bars.size() < MIN_BARS) {
            rationale.add("Need at least " + MIN_BARS + " bars; have " + bars.size() + ".");
            return StrategyEvaluation.skipped(rationale);
        }
        double[] closes = OhlcIndicators.closesAscending(bars);
        double rsi = OhlcIndicators.rsiLast(closes, RSI_PERIOD);
        Ohlc last = bars.get(bars.size() - 1);
        double avgVol = OhlcIndicators.avgVolumeExcludingLast(bars, VOLUME_AVG_DAYS);
        double swingLow = OhlcIndicators.minLowExcludingLast(bars, SWING_LOW_DAYS);
        if (Double.isNaN(rsi) || Double.isNaN(avgVol) || avgVol <= 0 || Double.isNaN(swingLow)) {
            rationale.add("RSI, volume average, or swing low not computable.");
            return StrategyEvaluation.skipped(rationale);
        }
        boolean oversold = rsi < RSI_OVERSOLD;
        boolean volOk = last.volume() > VOLUME_MULTIPLIER * avgVol;
        boolean notFreeFall = last.close() >= swingLow;
        rationale.add(String.format("RSI(%d)=%.2f (oversold <%s → %b).", RSI_PERIOD, rsi, RSI_OVERSOLD, oversold));
        rationale.add(String.format("Volume vs prior %d avg: %d > %.1f×%.0f → %b.", VOLUME_AVG_DAYS, last.volume(), VOLUME_MULTIPLIER, avgVol, volOk));
        rationale.add(String.format("Close >= min low prior %d sessions (%.4f): %b.", SWING_LOW_DAYS, swingLow, notFreeFall));
        return StrategyEvaluation.counted(oversold && volOk && notFreeFall, rationale);
    }
}
