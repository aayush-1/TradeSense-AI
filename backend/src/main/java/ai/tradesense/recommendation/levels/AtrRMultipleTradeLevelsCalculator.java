package ai.tradesense.recommendation.levels;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.recommendation.technical.OhlcIndicators;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ATR-based stop and a fixed reward-multiple target: risk = kStop × ATR(14), stop = entry − risk, target = entry +
 * rMultiple × risk (long-only).
 */
@Component
public class AtrRMultipleTradeLevelsCalculator implements TradeLevelsCalculator {

    static final int ATR_PERIOD = 14;
    static final double K_STOP_ATR = 1.5;
    static final double R_REWARD = 2.0;

    private static final String DESCRIPTION =
            "Volatility-sized stop using Wilder ATR(14): risk scales with how wide recent sessions have traded. "
                    + "Target is a fixed 2R multiple of that risk. Fits orderly trends and pullback entries.";

    @Override
    public String methodId() {
        return "atr-2r-v1";
    }

    @Override
    public String methodDescription() {
        return DESCRIPTION;
    }

    @Override
    public Optional<TradeLevelsSnapshot> compute(TradeLevelsInput input) {
        List<Ohlc> bars = input.ohlcSeries();
        if (bars == null || bars.size() < ATR_PERIOD + 1) {
            return Optional.empty();
        }
        double entry = input.entryPrice();
        if (Double.isNaN(entry) || entry <= 0) {
            return Optional.empty();
        }
        double atr = OhlcIndicators.atrLast(bars, ATR_PERIOD);
        if (Double.isNaN(atr) || atr <= 0) {
            return Optional.empty();
        }
        double risk = K_STOP_ATR * atr;
        double stop = entry - risk;
        double reward = R_REWARD * risk;
        double target = entry + reward;
        if (!(stop < entry && entry < target)) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.format("ATR(%d) = %.4f (Wilder, daily).", ATR_PERIOD, atr));
        lines.add(String.format(
                "Stop ≈ entry − %.1f×ATR = %.4f; risk per share ≈ %.4f.", K_STOP_ATR, stop, risk));
        lines.add(String.format(
                "Target ≈ entry + %.0f×risk (%.1f×ATR) = %.4f for a ~%.0fR reward.", R_REWARD, R_REWARD * K_STOP_ATR, target, R_REWARD));
        lines.add("Indicative levels only — not a guarantee of fill or outcome.");
        return Optional.of(
                new TradeLevelsSnapshot(
                        methodId(),
                        "ATR risk bands (2R target)",
                        methodDescription(),
                        entry,
                        stop,
                        target,
                        risk,
                        reward,
                        List.copyOf(lines)));
    }
}
