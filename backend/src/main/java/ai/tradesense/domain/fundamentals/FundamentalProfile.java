package ai.tradesense.domain.fundamentals;

/**
 * Company context, mostly from Yahoo {@code quoteSummary} modules {@code assetProfile} / {@code summaryProfile}.
 */
public record FundamentalProfile(
        String sector,
        String industry,
        String country,
        String website,
        Long fullTimeEmployees,
        String longBusinessSummary
) {
}
