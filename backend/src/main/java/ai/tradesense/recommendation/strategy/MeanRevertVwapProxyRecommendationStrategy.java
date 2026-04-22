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
 * Mean reversion vs rolling volume-weighted typical price (VWAP proxy on daily bars), with trend guard and ATR stretch.
 */
@Component
public class MeanRevertVwapProxyRecommendationStrategy implements RecommendationStrategy {

    static final int VWAP_SESSIONS = 20;
    static final int ATR_PERIOD = 14;
    static final double ATR_STRETCH_K = 1.0;
    static final int SMA_TREND_FAST = 50;
    static final int SMA_TREND_SLOW = 200;
    static final int MIN_BARS = 200;

    @Override
    public String strategyId() {
        return "mean-revert-vwap-proxy-v1";
    }

    @Override
    public String displayName() {
        return "Mean reversion (VWAP proxy)";
    }

    @Override
    public double defaultWeight() {
        return 0.9;
    }

    @Override
    public StrategyEvaluation evaluate(RecommendationContext context) {
        List<Ohlc> bars = context.ohlcSeries();
        List<String> rationale = new ArrayList<>();
        if (bars.size() < MIN_BARS) {
            rationale.add("Need " + MIN_BARS + " bars for trend guard + ATR; have " + bars.size() + ".");
            return StrategyEvaluation.skipped(rationale);
        }
        double[] closes = OhlcIndicators.closesAscending(bars);
        double proxy = OhlcIndicators.rollingVwapProxyLast(bars, VWAP_SESSIONS);
        double atr = OhlcIndicators.atrLast(bars, ATR_PERIOD);
        double sma50 = OhlcIndicators.smaLast(closes, SMA_TREND_FAST);
        double sma200 = OhlcIndicators.smaLast(closes, SMA_TREND_SLOW);
        double lastClose = closes[closes.length - 1];
        if (Double.isNaN(proxy) || Double.isNaN(atr) || atr <= 0) {
            rationale.add("VWAP proxy or ATR not computable.");
            return StrategyEvaluation.skipped(rationale);
        }
        boolean stretchedBelow = lastClose < proxy - ATR_STRETCH_K * atr;
        boolean trendGuard = (sma50 > sma200) || (lastClose > sma50);
        rationale.add(String.format("VWAP-proxy(%d)=%.4f, ATR(%d)=%.4f, k=%.2f.", VWAP_SESSIONS, proxy, ATR_PERIOD, atr, ATR_STRETCH_K));
        rationale.add(String.format("Stretch: close < proxy - k×ATR → %b (close=%.4f, threshold=%.4f).", stretchedBelow, lastClose, proxy - ATR_STRETCH_K * atr));
        rationale.add(String.format("Trend guard (SMA50>SMA200 or close>SMA50): %b.", trendGuard));
        return StrategyEvaluation.counted(stretchedBelow && trendGuard, rationale);
    }
}
