package ai.tradesense.domain;

import java.util.Objects;

/**
 * A position in the user's portfolio (quantity and average cost).
 */
public record Holding(
        String symbol,
        int quantity,
        double avgPrice
) {
    public Holding {
        Objects.requireNonNull(symbol, "symbol");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be non-negative");
        }
        if (avgPrice < 0) {
            throw new IllegalArgumentException("avgPrice must be non-negative");
        }
    }
}
