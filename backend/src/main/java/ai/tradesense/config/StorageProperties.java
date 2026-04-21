package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Local file storage roots for OHLC bars and fundamental snapshots ({@code tradesense.storage.*} in YAML).
 * Later replaceable with DB or Redis.
 */
@ConfigurationProperties(prefix = "tradesense.storage")
public class StorageProperties {

    /**
     * Parent directory for per-symbol OHLC JSON files (each file is {@code <DIR>/<SYMBOL>.json}).
     * Relative paths resolve against {@link System#getProperty(String) user.dir} (the JVM working directory when the app starts),
     * not against the Maven module path — so run from {@code backend/} if you want files under {@code backend/data/ohlc/}.
     */
    private String ohlcDirectory = "data/ohlc";

    /**
     * Per-symbol fundamental snapshots ({@code <DIR>/<SYMBOL>.json}), e.g. from Yahoo {@code quoteSummary}.
     */
    private String fundamentalsDirectory = "data/fundamentals";

    public String getOhlcDirectory() {
        return ohlcDirectory;
    }

    public void setOhlcDirectory(String ohlcDirectory) {
        this.ohlcDirectory = ohlcDirectory;
    }

    public String getFundamentalsDirectory() {
        return fundamentalsDirectory;
    }

    public void setFundamentalsDirectory(String fundamentalsDirectory) {
        this.fundamentalsDirectory = fundamentalsDirectory;
    }

    public Path resolvedOhlcDirectory() {
        return resolveRelative(ohlcDirectory);
    }

    public Path resolvedFundamentalsDirectory() {
        return resolveRelative(fundamentalsDirectory);
    }

    private static Path resolveRelative(String dir) {
        Path p = Path.of(dir.trim());
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return Path.of(System.getProperty("user.dir")).resolve(p).normalize();
    }
}
