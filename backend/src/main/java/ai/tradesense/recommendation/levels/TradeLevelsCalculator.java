package ai.tradesense.recommendation.levels;

import java.util.Optional;

/**
 * Pluggable stop / target from daily OHLC (e.g. ATR bands, structure). Implementations are Spring beans; the
 * recommendation service runs all calculators and returns every non-empty result (sorted by {@link #methodId()}).
 */
public interface TradeLevelsCalculator {

    String methodId();

    /** Short plain-language summary for UI tooltips / expand panels. */
    String methodDescription();

    Optional<TradeLevelsSnapshot> compute(TradeLevelsInput input);
}
