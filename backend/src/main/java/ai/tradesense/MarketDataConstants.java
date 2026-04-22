package ai.tradesense;

/**
 * Fixed market-data horizon (not exposed as HTTP query params).
 * <p>
 * Strategies such as {@code trend-ma-cross-v1} need 200 daily bars (50/200 SMA). Nine calendar months is only
 * ~180–190 trading sessions on NSE; use a longer window so trimmed series routinely clears 200 sessions while staying
 * bounded on disk.
 */
public final class MarketDataConstants {

    /**
     * Oldest session date kept when trimming OHLC (inclusive). ~14 calendar months is typically 210+ trading days,
     * enough headroom above 200 for holidays and short suspensions.
     */
    public static final int ANALYSIS_HISTORY_MONTHS = 14;

    private MarketDataConstants() {
    }
}
