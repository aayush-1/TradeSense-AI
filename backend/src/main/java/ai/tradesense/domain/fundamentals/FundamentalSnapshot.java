package ai.tradesense.domain.fundamentals;

import java.time.Instant;

/**
 * One persisted fundamental view per symbol, suitable for JSON on disk and later recommendation inputs.
 * <p>
 * <b>Yahoo source:</b> {@code GET https://query2.finance.yahoo.com/v10/finance/quoteSummary/{ticker}.NS?modules=...}
 * (use your NSE Yahoo suffix, e.g. {@code TCS.NS}). Typical modules to request:
 * <ul>
 *   <li>{@code assetProfile}, {@code summaryProfile} → {@link FundamentalProfile}</li>
 *   <li>{@code defaultKeyStatistics}, {@code summaryDetail}, {@code financialData} → {@link FundamentalMetrics}</li>
 *   <li>{@code calendarEvents} → {@link FundamentalCalendar}</li>
 *   <li>Optional later: {@code incomeStatementHistory}, {@code balanceSheetHistory}, {@code cashflowStatementHistory},
 *       {@code earningsHistory}, {@code recommendationTrend}, holder modules</li>
 * </ul>
 * Not every field exists for every .NS ticker; parsers should leave nulls and still persist the snapshot.
 */
public record FundamentalSnapshot(
        String symbol,
        Instant fetchedAt,
        String dataSource,
        FundamentalProfile profile,
        FundamentalMetrics metrics,
        FundamentalCalendar calendar
) {
    public FundamentalSnapshot {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol required");
        }
    }
}
