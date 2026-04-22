package ai.tradesense;

/**
 * Fixed market-data horizon (not exposed as HTTP query params).
 * <p>
 * Strategies such as {@code trend-ma-cross-v1} need 200 daily bars (50/200 SMA). The product keeps several years of
 * daily history for charting and richer context while remaining bounded on disk.
 */
public final class MarketDataConstants {

    /**
     * Oldest session date kept when trimming OHLC (inclusive). Three calendar years (~750+ NSE sessions) for charts
     * and analysis; still comfortably above 200-bar indicators.
     */
    public static final int ANALYSIS_HISTORY_MONTHS = 36;

    private MarketDataConstants() {
    }
}
