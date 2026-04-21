package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradesense.yahoo")
public record YahooChartProperties(
        String chartBaseUrl,
        String quoteSummaryBaseUrl,
        String quoteSummaryModules
) {
    public YahooChartProperties {
        if (chartBaseUrl == null || chartBaseUrl.isBlank()) {
            chartBaseUrl = "https://query1.finance.yahoo.com/v8/finance/chart";
        }
        chartBaseUrl = chartBaseUrl.replaceAll("/+$", "");
        if (quoteSummaryBaseUrl == null || quoteSummaryBaseUrl.isBlank()) {
            quoteSummaryBaseUrl = "https://query2.finance.yahoo.com/v10/finance/quoteSummary";
        }
        quoteSummaryBaseUrl = quoteSummaryBaseUrl.replaceAll("/+$", "");
        if (quoteSummaryModules == null || quoteSummaryModules.isBlank()) {
            quoteSummaryModules =
                    "assetProfile,summaryProfile,summaryDetail,defaultKeyStatistics,financialData,calendarEvents";
        }
    }
}
