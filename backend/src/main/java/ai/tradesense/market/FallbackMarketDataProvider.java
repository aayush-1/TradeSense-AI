package ai.tradesense.market;

import ai.tradesense.domain.Ohlc;

import java.time.LocalDate;
import java.util.List;

/**
 * Tries the primary provider first; on any exception uses the secondary provider.
 */
public final class FallbackMarketDataProvider implements MarketDataProvider {

    private final MarketDataProvider primary;
    private final MarketDataProvider secondary;

    public FallbackMarketDataProvider(MarketDataProvider primary, MarketDataProvider secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public List<Ohlc> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        try {
            return primary.getHistoricalData(symbol, from, to);
        } catch (RuntimeException ex) {
            return secondary.getHistoricalData(symbol, from, to);
        }
    }

    @Override
    public Ohlc getLatestData(String symbol) {
        try {
            return primary.getLatestData(symbol);
        } catch (RuntimeException ex) {
            return secondary.getLatestData(symbol);
        }
    }

    @Override
    public List<String> getSupportedSymbols() {
        return primary.getSupportedSymbols();
    }
}
