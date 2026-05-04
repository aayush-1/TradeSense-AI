package ai.tradesense.market;

import java.util.Locale;

public enum MarketSegment {
    INDIAN,
    CRYPTO;

    public static MarketSegment fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return INDIAN;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "INDIAN", "INDIA", "NSE" -> INDIAN;
            case "CRYPTO" -> CRYPTO;
            default -> throw new IllegalArgumentException("Unsupported segment: " + raw);
        };
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
