package ai.tradesense.market;

import ai.tradesense.domain.Ohlc;

import java.time.LocalDate;
import java.util.List;

/**
 * Pluggable source of market OHLC data (e.g. NSE, Yahoo, broker APIs).
 * <p>
 * Symbols to score for buys come from {@link ai.tradesense.universe.UniverseProvider}; this type may
 * still expose {@link #getSupportedSymbols()} as the subset the data feed can serve.
 */
public interface MarketDataProvider {

    List<Ohlc> getHistoricalData(String symbol, LocalDate from, LocalDate to);

    Ohlc getLatestData(String symbol);

    List<String> getSupportedSymbols();
}
