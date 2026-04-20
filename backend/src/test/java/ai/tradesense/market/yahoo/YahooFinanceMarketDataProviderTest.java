package ai.tradesense.market.yahoo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinanceMarketDataProviderTest {

    @Test
    void toYahooTickerAppendsNs() {
        assertThat(YahooFinanceMarketDataProvider.toYahooTicker("RELIANCE")).isEqualTo("RELIANCE.NS");
        assertThat(YahooFinanceMarketDataProvider.toYahooTicker("reliance")).isEqualTo("RELIANCE.NS");
        assertThat(YahooFinanceMarketDataProvider.toYahooTicker("TCS.NS")).isEqualTo("TCS.NS");
    }

    @Test
    void normalizeSymbolStripsNs() {
        assertThat(YahooFinanceMarketDataProvider.normalizeSymbol("RELIANCE.NS")).isEqualTo("RELIANCE");
        assertThat(YahooFinanceMarketDataProvider.normalizeSymbol("tcs")).isEqualTo("TCS");
    }
}
