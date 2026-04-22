package ai.tradesense.recommendation.strategy;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.RecommendationStrategy;
import ai.tradesense.recommendation.StrategyEvaluation;
import ai.tradesense.recommendation.technical.OhlcIndicators;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Chaikin-style money flow over 20 sessions: positive and improving vs 5 sessions ago. */
@Component
public class AccumulationPressureRecommendationStrategy implements RecommendationStrategy {

    static final int CMF_PERIOD = 20;
    static final int CMF_LOOKBACK = 5;
    static final int MIN_BARS = 25;

    @Override
    public String strategyId() {
        return "accumulation-pressure-v1";
    }

    @Override
    public String displayName() {
        return "Accumulation / money flow";
    }

    @Override
    public double defaultWeight() {
        return 1.1;
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        List<Ohlc> bars = context.ohlcSeries();
        List<String> rationale = new ArrayList<>();
        if (bars.size() < MIN_BARS) {
            rationale.add("Need at least " + MIN_BARS + " bars for CMF vs prior window; have " + bars.size() + ".");
            return StrategyEvaluation.skipped(rationale);
        }
        double cmfNow = OhlcIndicators.chaikinMoneyFlowRatio(bars, CMF_PERIOD);
        List<Ohlc> priorWindow = bars.subList(0, bars.size() - CMF_LOOKBACK);
        double cmfPast = OhlcIndicators.chaikinMoneyFlowRatio(priorWindow, CMF_PERIOD);
        if (Double.isNaN(cmfNow) || Double.isNaN(cmfPast)) {
            rationale.add("CMF not computable on window.");
            return StrategyEvaluation.skipped(rationale);
        }
        boolean positive = cmfNow > 0;
        boolean rising = cmfNow > cmfPast;
        rationale.add(String.format("CMF(%d) now=%.4f; CMF as of %d sessions ago=%.4f.", CMF_PERIOD, cmfNow, CMF_LOOKBACK, cmfPast));
        rationale.add(positive ? "CMF > 0 (buy-side pressure)." : "CMF not > 0.");
        rationale.add(rising ? "CMF rising vs prior window." : "CMF not rising vs prior window.");
        return StrategyEvaluation.counted(positive && rising, rationale);
    }
}
