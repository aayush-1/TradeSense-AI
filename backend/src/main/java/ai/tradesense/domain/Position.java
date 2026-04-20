package ai.tradesense.domain;

import java.util.Objects;

/**
 * Open or closed position with mark-to-market style P/L (provider-defined).
 */
public record Position(
        String symbol,
        int quantity,
        double pnl
) {
    public Position {
        Objects.requireNonNull(symbol, "symbol");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}
