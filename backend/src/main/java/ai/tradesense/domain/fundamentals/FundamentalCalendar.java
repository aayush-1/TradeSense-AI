package ai.tradesense.domain.fundamentals;

import java.time.LocalDate;

/**
 * Forward-looking corporate events from Yahoo {@code calendarEvents} (when present).
 */
public record FundamentalCalendar(
        // Next scheduled earnings release date (company or Yahoo estimate; may be a range collapsed to one day in parser).
        LocalDate earningsDateNext,
        // First date a new buyer of the stock does not receive the declared dividend (cutoff for dividend eligibility).
        LocalDate exDividendDate,
        // Date the dividend is paid or distributed to shareholders of record (per Yahoo / issuer calendar).
        LocalDate dividendDate
) {
}
