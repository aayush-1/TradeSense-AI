package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradesense.yahoo")
public record YahooChartProperties(
        String chartBaseUrl
) {
    public YahooChartProperties {
        if (chartBaseUrl == null || chartBaseUrl.isBlank()) {
            chartBaseUrl = "https://query1.finance.yahoo.com/v8/finance/chart";
        }
        chartBaseUrl = chartBaseUrl.replaceAll("/+$", "");
    }
}
