package ai.tradesense.market;

import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.coingecko.CoinGeckoMarketDataProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DefaultSegmentedMarketDataProvider implements SegmentedMarketDataProvider {

    private final MarketDataProvider indianMarketDataProvider;
    private final CoinGeckoMarketDataProvider coinGeckoMarketDataProvider;

    public DefaultSegmentedMarketDataProvider(
            MarketDataProvider indianMarketDataProvider,
            CoinGeckoMarketDataProvider coinGeckoMarketDataProvider) {
        this.indianMarketDataProvider = indianMarketDataProvider;
        this.coinGeckoMarketDataProvider = coinGeckoMarketDataProvider;
    }

    @Override
    public List<Ohlc> getHistoricalData(MarketSegment segment, String symbol, LocalDate from, LocalDate to) {
        return switch (segment) {
            case INDIAN -> indianMarketDataProvider.getHistoricalData(symbol, from, to);
            case CRYPTO -> coinGeckoMarketDataProvider.getHistoricalData(symbol, from, to);
        };
    }
}
