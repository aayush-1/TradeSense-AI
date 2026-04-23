package ai.tradesense.recommendation;

import ai.tradesense.config.AtrVolatilityGateProperties;
import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.technical.OhlcIndicators;

import java.util.ArrayList;
import java.util.List;

/** Appends human-readable ATR% context lines to symbol notes (no effect on strategy aggregation). */
public final class AtrVolatilityNotes {

    private AtrVolatilityNotes() {}

    /**
     * When enabled and the series is long enough, may add zero, one, or two lines about unusually high or low
     * ATR% (ATR divided by last close).
     */
    public static void maybeAppend(List<String> notes, List<Ohlc> bars, AtrVolatilityGateProperties props) {
        if (notes == null || props == null || !props.isEnabled() || bars == null || bars.isEmpty()) {
            return;
        }
        int p = props.getAtrPeriod();
        if (p < 1 || bars.size() < p + 1) {
            return;
        }
        Ohlc last = bars.get(bars.size() - 1);
        double close = last.close();
        if (close <= 0 || Double.isNaN(close)) {
            return;
        }
        double atr = OhlcIndicators.atrLast(bars, p);
        if (Double.isNaN(atr) || atr <= 0) {
            return;
        }
        double atrPct = atr / close;
        double highTh = props.getNoteHighAtrPct();
        double lowTh = props.getNoteLowAtrPct();
        List<String> add = new ArrayList<>();
        if (highTh > 0 && atrPct >= highTh) {
            add.add(String.format(
                    "Volatility note: ATR(%d)/close ≈ %.2f%% (≥%.2f%%) — daily ranges are large vs price; "
                            + "signals and stops can be noisier.",
                    p, 100.0 * atrPct, 100.0 * highTh));
        }
        if (lowTh > 0 && atrPct <= lowTh) {
            add.add(String.format(
                    "Volatility note: ATR(%d)/close ≈ %.2f%% (≤%.2f%%) — price is very quiet; "
                            + "breakout-style reads may matter less.",
                    p, 100.0 * atrPct, 100.0 * lowTh));
        }
        notes.addAll(add);
    }
}
