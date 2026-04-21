package ai.tradesense.market.yahoo;

import ai.tradesense.config.YahooChartProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches raw JSON from Yahoo {@code v10/finance/quoteSummary} for NSE symbols as {@code SYMBOL.NS}.
 */
@Component
public final class YahooQuoteSummaryClient {

    private final RestClient restClient;
    private final YahooChartProperties yahoo;

    public YahooQuoteSummaryClient(
            @Qualifier("yahooQuoteSummaryRestClient") RestClient restClient,
            YahooChartProperties yahoo) {
        this.restClient = restClient;
        this.yahoo = yahoo;
    }

    public String fetchQuoteSummaryJson(String nseSymbol) {
        String normalized = YahooFinanceMarketDataProvider.normalizeSymbol(nseSymbol);
        String yahooTicker = YahooFinanceMarketDataProvider.toYahooTicker(normalized);
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{ticker}")
                            .queryParam("modules", yahoo.quoteSummaryModules())
                            .build(yahooTicker))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        throw new IllegalStateException(
                                "Yahoo quoteSummary HTTP " + res.getStatusCode() + " for " + yahooTicker);
                    })
                    .body(String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Yahoo quoteSummary request failed for " + yahooTicker, e);
        }
    }
}
