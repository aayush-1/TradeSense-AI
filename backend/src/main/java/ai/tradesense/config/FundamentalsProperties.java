package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fundamentals cache TTL and related knobs ({@code tradesense.fundamentals.*}).
 */
@ConfigurationProperties(prefix = "tradesense.fundamentals")
public class FundamentalsProperties {

    /**
     * Skip Yahoo quoteSummary if a snapshot on disk is newer than this many days (unless force refresh).
     */
    private int maxAgeDays = 90;

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }
}
