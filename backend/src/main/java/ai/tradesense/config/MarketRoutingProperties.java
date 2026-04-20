package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How to combine Yahoo Finance and ICICI Breeze for {@link ai.tradesense.market.MarketDataProvider}.
 */
@ConfigurationProperties(prefix = "tradesense.market")
public class MarketRoutingProperties {

    /**
     * When true, always use Yahoo Finance and ignore Breeze (even if Breeze credentials are present).
     */
    private boolean yahooOnly = true;

    /**
     * When Breeze is enabled and configured, try Breeze first then Yahoo on failure.
     */
    private boolean breezeThenYahooFallback = false;

    /**
     * When true (and Breeze is runnable), use Breeze only.
     */
    private boolean breezeOnly = false;

    public boolean isYahooOnly() {
        return yahooOnly;
    }

    public void setYahooOnly(boolean yahooOnly) {
        this.yahooOnly = yahooOnly;
    }

    public boolean isBreezeThenYahooFallback() {
        return breezeThenYahooFallback;
    }

    public void setBreezeThenYahooFallback(boolean breezeThenYahooFallback) {
        this.breezeThenYahooFallback = breezeThenYahooFallback;
    }

    public boolean isBreezeOnly() {
        return breezeOnly;
    }

    public void setBreezeOnly(boolean breezeOnly) {
        this.breezeOnly = breezeOnly;
    }
}
