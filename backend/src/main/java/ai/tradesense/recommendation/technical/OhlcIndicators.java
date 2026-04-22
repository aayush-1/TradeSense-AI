package ai.tradesense.recommendation.technical;

import ai.tradesense.domain.Ohlc;

import java.util.List;

/**
 * Shared daily-bar math for recommendation strategies (SMA, EMA, RSI, ATR, volume, VWAP-style proxy, Chaikin-style flow).
 * All {@code *Last} methods refer to the <strong>last</strong> bar in the series (most recent). Inputs must be sorted
 * ascending by {@link Ohlc#date()}.
 */
public final class OhlcIndicators {

    private OhlcIndicators() {
    }

    /** Closing prices in bar order (same index as {@code bars}). */
    public static double[] closesAscending(List<Ohlc> bars) {
        double[] c = new double[bars.size()];
        for (int i = 0; i < bars.size(); i++) {
            c[i] = bars.get(i).close();
        }
        return c;
    }

    /** Simple moving average of the last {@code period} closes (inclusive of last bar). */
    public static double smaLast(double[] closes, int period) {
        if (closes.length < period || period <= 0) {
            return Double.NaN;
        }
        double s = 0;
        for (int i = closes.length - period; i < closes.length; i++) {
            s += closes[i];
        }
        return s / period;
    }

    /**
     * Exponential moving average at the last close; first value seeds with SMA of the first {@code period} closes.
     */
    public static double emaLast(double[] closes, int period) {
        if (closes.length < period || period <= 0) {
            return Double.NaN;
        }
        double alpha = 2.0 / (period + 1);
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += closes[i];
        }
        double ema = sum / period;
        for (int i = period; i < closes.length; i++) {
            ema = alpha * closes[i] + (1.0 - alpha) * ema;
        }
        return ema;
    }

    /** Wilder RSI at the last bar ({@code period} is commonly 14). */
    public static double rsiLast(double[] closes, int period) {
        if (closes.length < period + 1 || period <= 0) {
            return Double.NaN;
        }
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double ch = closes[i] - closes[i - 1];
            if (ch >= 0) {
                avgGain += ch;
            } else {
                avgLoss -= ch;
            }
        }
        avgGain /= period;
        avgLoss /= period;

        for (int i = period + 1; i < closes.length; i++) {
            double ch = closes[i] - closes[i - 1];
            double gain = ch > 0 ? ch : 0;
            double loss = ch < 0 ? -ch : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }
        if (avgLoss == 0) {
            return avgGain == 0 ? 50.0 : 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    /**
     * Wilder ATR at the last bar. True range uses previous close; first bar uses high-low only.
     */
    public static double atrLast(List<Ohlc> bars, int period) {
        if (bars == null || bars.size() < period + 1 || period <= 0) {
            return Double.NaN;
        }
        int n = bars.size();
        double[] tr = new double[n];
        tr[0] = bars.get(0).high() - bars.get(0).low();
        for (int i = 1; i < n; i++) {
            tr[i] = trueRange(bars.get(i), bars.get(i - 1).close());
        }
        double sum = 0;
        for (int i = 1; i <= period; i++) {
            sum += tr[i];
        }
        double atr = sum / period;
        for (int i = period + 1; i < n; i++) {
            atr = (atr * (period - 1) + tr[i]) / period;
        }
        return atr;
    }

    public static double trueRange(Ohlc bar, double previousClose) {
        double hl = bar.high() - bar.low();
        double hc = Math.abs(bar.high() - previousClose);
        double lc = Math.abs(bar.low() - previousClose);
        return Math.max(hl, Math.max(hc, lc));
    }

    /** Mean volume over the last {@code period} bars (inclusive of last). */
    public static double avgVolumeLast(List<Ohlc> bars, int period) {
        if (bars == null || bars.size() < period || period <= 0) {
            return Double.NaN;
        }
        long sum = 0;
        for (int i = bars.size() - period; i < bars.size(); i++) {
            sum += bars.get(i).volume();
        }
        return (double) sum / period;
    }

    /**
     * Mean volume over the {@code period} bars immediately <strong>before</strong> the final bar (excludes the most
     * recent session). Use with today’s volume for “vs 20-day average” style checks.
     */
    public static double avgVolumeExcludingLast(List<Ohlc> bars, int period) {
        if (bars == null || bars.size() < period + 1 || period < 1) {
            return Double.NaN;
        }
        long sum = 0;
        for (int i = bars.size() - 1 - period; i < bars.size() - 1; i++) {
            sum += bars.get(i).volume();
        }
        return (double) sum / period;
    }

    /**
     * Lowest low over up to {@code priorBarCount} bars immediately <strong>before</strong> the final bar (excludes last).
     */
    public static double minLowExcludingLast(List<Ohlc> bars, int priorBarCount) {
        if (bars == null || bars.size() < 2 || priorBarCount < 1) {
            return Double.NaN;
        }
        int lastPriorIdx = bars.size() - 2;
        int startIdx = lastPriorIdx - priorBarCount + 1;
        if (startIdx < 0) {
            startIdx = 0;
        }
        double min = Double.POSITIVE_INFINITY;
        for (int i = startIdx; i <= lastPriorIdx; i++) {
            min = Math.min(min, bars.get(i).low());
        }
        return min == Double.POSITIVE_INFINITY ? Double.NaN : min;
    }

    /**
     * Highest high over up to {@code priorBarCount} bars immediately <strong>before</strong> the final bar
     * (excludes the last bar — e.g. “20-day high” before today for breakout checks).
     */
    public static double maxHighExcludingLast(List<Ohlc> bars, int priorBarCount) {
        if (bars == null || bars.size() < 2 || priorBarCount < 1) {
            return Double.NaN;
        }
        int lastPriorIdx = bars.size() - 2;
        int startIdx = lastPriorIdx - priorBarCount + 1;
        if (startIdx < 0) {
            startIdx = 0;
        }
        double max = Double.NEGATIVE_INFINITY;
        for (int i = startIdx; i <= lastPriorIdx; i++) {
            max = Math.max(max, bars.get(i).high());
        }
        return max == Double.NEGATIVE_INFINITY ? Double.NaN : max;
    }

    /** Highest high over the last {@code period} bars including the most recent bar. */
    public static double maxHighLast(List<Ohlc> bars, int period) {
        if (bars == null || bars.size() < period || period < 1) {
            return Double.NaN;
        }
        double max = Double.NEGATIVE_INFINITY;
        for (int i = bars.size() - period; i < bars.size(); i++) {
            max = Math.max(max, bars.get(i).high());
        }
        return max;
    }

    /** Typical price (H+L+C)/3 for one bar. */
    public static double typicalPrice(Ohlc bar) {
        return (bar.high() + bar.low() + bar.close()) / 3.0;
    }

    /**
     * Session-style <strong>volume-weighted average price proxy</strong> over the last {@code sessions} bars
     * (includes the last bar): {@code sum(typical × volume) / sum(volume)}. Not exchange-time VWAP; daily bars only.
     */
    public static double rollingVwapProxyLast(List<Ohlc> bars, int sessions) {
        if (bars == null || bars.isEmpty() || sessions < 1) {
            return Double.NaN;
        }
        int from = Math.max(0, bars.size() - sessions);
        double numerator = 0;
        long denominator = 0;
        for (int i = from; i < bars.size(); i++) {
            Ohlc o = bars.get(i);
            long v = o.volume();
            if (v <= 0) {
                continue;
            }
            numerator += typicalPrice(o) * v;
            denominator += v;
        }
        return denominator > 0 ? numerator / denominator : Double.NaN;
    }

    /**
     * Chaikin money flow ratio over the last {@code period} bars: {@code sum(mf_volume) / sum(volume)} where
     * {@code mf_volume = moneyFlowMultiplier × volume} and multiplier is {@code (2×close - high - low) / (high - low)},
     * or {@code 0} when {@code high == low} (no division by zero).
     */
    public static double chaikinMoneyFlowRatio(List<Ohlc> bars, int period) {
        if (bars == null || bars.size() < period || period < 1) {
            return Double.NaN;
        }
        double sumMfv = 0;
        long sumVol = 0;
        for (int i = bars.size() - period; i < bars.size(); i++) {
            Ohlc o = bars.get(i);
            long v = o.volume();
            if (v <= 0) {
                continue;
            }
            sumMfv += chaikinMoneyFlowMultiplier(o) * v;
            sumVol += v;
        }
        return sumVol > 0 ? sumMfv / sumVol : Double.NaN;
    }

    /**
     * Money flow multiplier in [-1, 1]: {@code ((close - low) - (high - close)) / (high - low)} ≡ {@code (2C - H - L) / (H - L)}.
     * Returns {@code 0} when {@code high == low}.
     */
    public static double chaikinMoneyFlowMultiplier(Ohlc bar) {
        double range = bar.high() - bar.low();
        if (range <= 0) {
            return 0.0;
        }
        return ((bar.close() - bar.low()) - (bar.high() - bar.close())) / range;
    }
}
