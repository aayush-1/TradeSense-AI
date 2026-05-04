package ai.tradesense.market;

import ai.tradesense.domain.Ohlc;

import java.time.LocalDate;
import java.util.List;

public interface SegmentedMarketDataProvider {
    List<Ohlc> getHistoricalData(MarketSegment segment, String symbol, LocalDate from, LocalDate to);
}
