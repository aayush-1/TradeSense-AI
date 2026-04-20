package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Local OHLC files (JSON per symbol). Later replaceable with DB or Redis.
 */
@ConfigurationProperties(prefix = "tradesense.storage")
public class OhlcStorageProperties {

    /**
     * Parent directory for per-symbol JSON files (each file is {@code <DIR>/<SYMBOL>.json}).
     * Relative paths resolve against {@link System#getProperty(String) user.dir} (the JVM working directory when the app starts),
     * not against the Maven module path — so run from {@code backend/} if you want files under {@code backend/data/ohlc/}.
     */
    private String ohlcDirectory = "data/ohlc";

    public String getOhlcDirectory() {
        return ohlcDirectory;
    }

    public void setOhlcDirectory(String ohlcDirectory) {
        this.ohlcDirectory = ohlcDirectory;
    }

    public Path resolvedOhlcDirectory() {
        Path p = Path.of(ohlcDirectory.trim());
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return Path.of(System.getProperty("user.dir")).resolve(p).normalize();
    }
}
