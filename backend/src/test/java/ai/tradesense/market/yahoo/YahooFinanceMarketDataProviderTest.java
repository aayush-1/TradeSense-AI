package ai.tradesense.market.yahoo;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void chartPeriodStartInclusive_widensSingleDayWindow() {
        LocalDate d = LocalDate.of(2026, 4, 20);
        assertThat(YahooFinanceMarketDataProvider.chartPeriodStartInclusive(d, d)).isEqualTo(d.minusDays(1));
    }

    @Test
    void chartPeriodStartInclusive_unchangedForMultiDay() {
        LocalDate a = LocalDate.of(2026, 4, 1);
        LocalDate b = LocalDate.of(2026, 4, 20);
        assertThat(YahooFinanceMarketDataProvider.chartPeriodStartInclusive(a, b)).isEqualTo(a);
    }

    @Test
    void chartPeriodStartInclusive_rejectsInvertedRange() {
        LocalDate a = LocalDate.of(2026, 4, 20);
        LocalDate b = LocalDate.of(2026, 4, 1);
        assertThatThrownBy(() -> YahooFinanceMarketDataProvider.chartPeriodStartInclusive(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be <= to");
    }
}
