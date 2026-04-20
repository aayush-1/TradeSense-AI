package ai.tradesense.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Single end-of-day (or bar) OHLCV observation for an instrument.
 */
public record Ohlc(
        String symbol,
        LocalDate date,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
    public Ohlc {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(date, "date");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (high < low) {
            throw new IllegalArgumentException("high must be >= low");
        }
        if (open < 0 || high < 0 || low < 0 || close < 0) {
            throw new IllegalArgumentException("OHLC prices must be non-negative");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must be non-negative");
        }
    }
}
