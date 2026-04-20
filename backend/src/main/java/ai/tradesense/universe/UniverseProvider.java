package ai.tradesense.universe;

import java.util.List;

/**
 * Symbols eligible for market-wide analysis (e.g. buy scoring).
 * <p>
 * MVP: use {@link FixedUniverseProvider} with a static list or file.
 * Later: exchange master, liquidity filters, or other {@link UniverseProvider} implementations.
 */
public interface UniverseProvider {

    /**
     * Snapshot of symbols to evaluate, typically NSE-style tickers (e.g. {@code RELIANCE}).
     * Implementations should return an unmodifiable or defensive copy if the backing data can change.
     */
    List<String> getSymbols();
}
