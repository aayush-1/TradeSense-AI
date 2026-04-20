package ai.tradesense;

/**
 * Fixed market-data horizon (not exposed as HTTP query params).
 * <p>
 * Nine calendar months is enough for roughly 200 trading sessions (e.g. 200-DMA style work)
 * without keeping years of files on disk.
 */
public final class MarketDataConstants {

    /** Minimum history kept on disk and used as the default Yahoo back-fill window. */
    public static final int ANALYSIS_HISTORY_MONTHS = 9;

    private MarketDataConstants() {
    }
}
