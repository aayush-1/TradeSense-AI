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
 * Early-style long setup: established uptrend (50/200 stack, price above 200 SMA), price has dipped into the zone
 * between the 20 EMA and 50 SMA (“ribbon”), RSI in a neutral/cooled band (not panic oversold, not hot overbought), with
 * an optional volume-contraction hint when today’s volume is below recent average.
 */
@Component
public class PullbackInUptrendRecommendationStrategy implements RecommendationStrategy {

    static final int SMA_FAST = 50;
    static final int SMA_SLOW = 200;
    static final int EMA_PULLBACK = 20;
    static final int RSI_PERIOD = 14;
    static final double RSI_BAND_LOW = 40.0;
    static final double RSI_BAND_HIGH = 55.0;
    static final int VOL_LOOKBACK = 20;
    /** Below this multiple of prior 20-day average volume counts as “contraction” (informational only). */
    static final double VOL_CONTRACTION_VS_AVG = 0.95;
    static final int MIN_BARS = 200;

    @Override
    public String strategyId() {
        return "pullback-uptrend-v1";
    }

    @Override
    public String displayName() {
        return "Pullback in uptrend";
    }

    @Override
    public double defaultWeight() {
        return 1.25;
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        List<Ohlc> bars = context.ohlcSeries();
        List<String> rationale = new ArrayList<>();
        if (bars.size() < MIN_BARS) {
            rationale.add(
                    "Need at least " + MIN_BARS + " bars for 200 SMA / ribbon context; have " + bars.size() + ".");
            return StrategyEvaluation.skipped(rationale);
        }
        double[] closes = OhlcIndicators.closesAscending(bars);
        double sma50 = OhlcIndicators.smaLast(closes, SMA_FAST);
        double sma200 = OhlcIndicators.smaLast(closes, SMA_SLOW);
        double ema20 = OhlcIndicators.emaLast(closes, EMA_PULLBACK);
        double rsi = OhlcIndicators.rsiLast(closes, RSI_PERIOD);
        Ohlc last = bars.get(bars.size() - 1);
        double lastClose = last.close();

        if (Double.isNaN(sma50)
                || Double.isNaN(sma200)
                || Double.isNaN(ema20)
                || Double.isNaN(rsi)) {
            rationale.add("SMA, EMA, or RSI not computable.");
            return StrategyEvaluation.skipped(rationale);
        }

        boolean stack = sma50 > sma200;
        boolean above200 = lastClose > sma200;
        double bandLo = Math.min(ema20, sma50);
        double bandHi = Math.max(ema20, sma50);
        boolean inRibbon = lastClose >= bandLo && lastClose <= bandHi;
        boolean rsiCooled = rsi >= RSI_BAND_LOW && rsi <= RSI_BAND_HIGH;

        rationale.add(String.format(
                "Trend: SMA%d=%.4f, SMA%d=%.4f → stack %b; close=%.4f vs SMA200 → above200 %b.",
                SMA_FAST, sma50, SMA_SLOW, sma200, stack, lastClose, above200));
        rationale.add(String.format(
                "Pullback ribbon: EMA%d=%.4f, SMA%d=%.4f → band [%.4f, %.4f]; close in band %s.",
                EMA_PULLBACK, ema20, SMA_FAST, sma50, bandLo, bandHi, inRibbon));
        rationale.add(String.format(
                "RSI(%d)=%.2f (cooled band [%.0f, %.0f]) → %b.",
                RSI_PERIOD, rsi, RSI_BAND_LOW, RSI_BAND_HIGH, rsiCooled));

        double avgVolPrior = OhlcIndicators.avgVolumeExcludingLast(bars, VOL_LOOKBACK);
        boolean volContracted =
                !Double.isNaN(avgVolPrior) && avgVolPrior > 0 && last.volume() < avgVolPrior * VOL_CONTRACTION_VS_AVG;
        if (!Double.isNaN(avgVolPrior) && avgVolPrior > 0) {
            rationale.add(String.format(
                    "Volume: last=%d vs prior %d avg=%.0f → contraction (optional) %b.",
                    last.volume(), VOL_LOOKBACK, avgVolPrior, volContracted));
        }

        boolean buy = stack && above200 && inRibbon && rsiCooled;
        return StrategyEvaluation.counted(buy, rationale);
    }
}
