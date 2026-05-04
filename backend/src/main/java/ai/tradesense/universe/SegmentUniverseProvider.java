package ai.tradesense.universe;

import ai.tradesense.market.MarketSegment;
import ai.tradesense.market.yahoo.YahooFinanceMarketDataProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SegmentUniverseProvider {

    private static final String INDIAN_UNIVERSE_RESOURCE = FixedUniverseProvider.DEFAULT_CLASSPATH_RESOURCE;
    private static final String CRYPTO_UNIVERSE_RESOURCE = "ai/tradesense/universe/crypto-universe.txt";

    private final Map<MarketSegment, List<String>> symbolsBySegment;

    public SegmentUniverseProvider() throws IOException {
        this.symbolsBySegment = loadSymbolMap();
    }

    public List<String> getSymbols(MarketSegment segment) {
        return symbolsBySegment.getOrDefault(segment, List.of());
    }

    public String normalizeSymbol(MarketSegment segment, String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            throw new IllegalArgumentException("symbol required");
        }
        if (segment == MarketSegment.CRYPTO) {
            return rawSymbol.trim().toUpperCase();
        }
        return YahooFinanceMarketDataProvider.normalizeSymbol(rawSymbol);
    }

    private static Map<MarketSegment, List<String>> loadSymbolMap() throws IOException {
        EnumMap<MarketSegment, List<String>> map = new EnumMap<>(MarketSegment.class);
        map.put(MarketSegment.INDIAN, FixedUniverseProvider.fromClasspathResource(INDIAN_UNIVERSE_RESOURCE).getSymbols());
        map.put(MarketSegment.CRYPTO, FixedUniverseProvider.fromClasspathResource(CRYPTO_UNIVERSE_RESOURCE).getSymbols());
        return Map.copyOf(map);
    }
}
