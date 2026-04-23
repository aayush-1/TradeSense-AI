package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional ATR% (ATR / last close) notes on each symbol recommendation — warns when daily volatility looks unusually
 * high or unusually low relative to price. Does not change strategy votes; see {@code enabled}.
 */
@ConfigurationProperties(prefix = "tradesense.recommendations.atr-gate")
public class AtrVolatilityGateProperties {

    private boolean enabled = true;
    private int atrPeriod = 14;
    /** When ATR/close is at or above this fraction, append a “high volatility” note (e.g. 0.07 = 7%). */
    private double noteHighAtrPct = 0.07;
    /** When ATR/close is at or below this fraction, append a “low volatility” note (e.g. 0.004 = 0.4%). */
    private double noteLowAtrPct = 0.004;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAtrPeriod() {
        return atrPeriod;
    }

    public void setAtrPeriod(int atrPeriod) {
        this.atrPeriod = atrPeriod;
    }

    public double getNoteHighAtrPct() {
        return noteHighAtrPct;
    }

    public void setNoteHighAtrPct(double noteHighAtrPct) {
        this.noteHighAtrPct = noteHighAtrPct;
    }

    public double getNoteLowAtrPct() {
        return noteLowAtrPct;
    }

    public void setNoteLowAtrPct(double noteLowAtrPct) {
        this.noteLowAtrPct = noteLowAtrPct;
    }
}
