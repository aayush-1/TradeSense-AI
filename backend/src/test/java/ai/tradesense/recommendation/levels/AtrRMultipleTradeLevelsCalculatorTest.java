package ai.tradesense.recommendation.levels;

import ai.tradesense.domain.Ohlc;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AtrRMultipleTradeLevelsCalculatorTest {

    @Test
    void compute_producesLongStopBelowEntryAndTargetAbove() {
        List<Ohlc> bars = new ArrayList<>();
        double p = 100.0;
        for (int i = 0; i < 20; i++) {
            LocalDate d = LocalDate.of(2024, 1, 1).plusDays(i);
            double c = p + i * 0.1;
            bars.add(new Ohlc("TEST", d, c - 0.5, c + 0.6, c - 0.4, c, 1_000_000L));
        }
        double entry = bars.get(bars.size() - 1).close();
        AtrRMultipleTradeLevelsCalculator calc = new AtrRMultipleTradeLevelsCalculator();
        Optional<TradeLevelsSnapshot> out = calc.compute(new TradeLevelsInput("TEST", bars, entry));
        assertTrue(out.isPresent());
        TradeLevelsSnapshot s = out.get();
        assertTrue(s.stopLoss() < entry && entry < s.takeProfit());
        assertTrue(s.riskPerShare() > 0 && s.rewardPerShare() > 0);
    }
}
