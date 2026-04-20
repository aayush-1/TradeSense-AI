package ai.tradesense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ICICI Breeze credentials must come from environment (never commit real keys).
 * <p>
 * After login at ICICI, pass the {@code apisession} query value as {@code api-session}.
 * ICICI may require requests from your registered static IP (e.g. hotspot IP you registered with them).
 */
@ConfigurationProperties(prefix = "tradesense.breeze")
public record BreezeProperties(
        boolean enabled,
        String apiKey,
        String secretKey,
        String apiSession
) {
    public BreezeProperties {
        apiKey = blankToEmpty(apiKey);
        secretKey = blankToEmpty(secretKey);
        apiSession = blankToEmpty(apiSession);
    }

    public boolean isRunnable() {
        return enabled && !apiKey.isEmpty() && !secretKey.isEmpty() && !apiSession.isEmpty();
    }

    private static String blankToEmpty(String s) {
        return s == null || s.isBlank() ? "" : s.trim();
    }
}
