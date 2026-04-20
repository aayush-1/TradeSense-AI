package ai.tradesense.config;

import ai.tradesense.market.FallbackMarketDataProvider;
import ai.tradesense.market.MarketDataProvider;
import ai.tradesense.market.breeze.IciciBreezeMarketDataProvider;
import ai.tradesense.market.yahoo.YahooFinanceMarketDataProvider;
import ai.tradesense.universe.FixedUniverseProvider;
import ai.tradesense.universe.UniverseProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties({
        YahooChartProperties.class,
        CorsProperties.class,
        BreezeProperties.class,
        MarketRoutingProperties.class,
        OhlcStorageProperties.class
})
public class AppConfiguration {

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; TradeSense-Backend/0.1; +https://github.com/)";

    @Bean
    public RestClient yahooRestClient(YahooChartProperties yahoo) {
        return RestClient.builder()
                .baseUrl(yahoo.chartBaseUrl())
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public UniverseProvider universeProvider() throws IOException {
        return FixedUniverseProvider.defaultUniverse();
    }

    @Bean
    public YahooFinanceMarketDataProvider yahooMarketDataProvider(
            RestClient yahooRestClient,
            UniverseProvider universeProvider,
            ObjectMapper objectMapper) {
        return new YahooFinanceMarketDataProvider(yahooRestClient, universeProvider, objectMapper);
    }

    @Bean
    @Primary
    public MarketDataProvider marketDataProvider(
            BreezeProperties breezeProperties,
            MarketRoutingProperties marketRoutingProperties,
            YahooFinanceMarketDataProvider yahooMarketDataProvider,
            UniverseProvider universeProvider,
            ObjectMapper objectMapper) {
        if (marketRoutingProperties.isYahooOnly()) {
            return yahooMarketDataProvider;
        }
        if (!breezeProperties.isRunnable()) {
            return yahooMarketDataProvider;
        }
        IciciBreezeMarketDataProvider breeze =
                new IciciBreezeMarketDataProvider(breezeProperties, universeProvider, objectMapper);
        if (marketRoutingProperties.isBreezeOnly()) {
            return breeze;
        }
        if (marketRoutingProperties.isBreezeThenYahooFallback()) {
            return new FallbackMarketDataProvider(breeze, yahooMarketDataProvider);
        }
        return breeze;
    }
}
