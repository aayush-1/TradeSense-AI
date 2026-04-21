package ai.tradesense.web;

import ai.tradesense.domain.fundamentals.FundamentalSnapshot;
import ai.tradesense.fundamentals.FundamentalCacheService;
import ai.tradesense.fundamentals.FundamentalsBulkRefreshResult;
import ai.tradesense.market.yahoo.YahooFinanceMarketDataProvider;
import ai.tradesense.universe.UniverseProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class FundamentalController {

    private final FundamentalCacheService fundamentalCacheService;
    private final UniverseProvider universeProvider;

    public FundamentalController(FundamentalCacheService fundamentalCacheService, UniverseProvider universeProvider) {
        this.fundamentalCacheService = fundamentalCacheService;
        this.universeProvider = universeProvider;
    }

    /**
     * Returns cached fundamentals if the on-disk snapshot is within TTL; otherwise fetches Yahoo quoteSummary and saves.
     */
    @GetMapping("/fundamentals/{symbol}")
    public FundamentalSnapshot getFundamentals(@PathVariable String symbol) {
        requireInUniverse(symbol);
        return fundamentalCacheService.getOrRefresh(symbol, false).snapshot();
    }

    /**
     * Forces a Yahoo quoteSummary fetch and overwrites the local snapshot (ignores TTL). Fails if Yahoo returns an error
     * and there is nothing useful to persist.
     */
    @PostMapping("/fundamentals/{symbol}/refresh")
    public FundamentalSnapshot refreshFundamentals(@PathVariable String symbol) {
        requireInUniverse(symbol);
        return fundamentalCacheService.getOrRefresh(symbol, true).snapshot();
    }

    /**
     * Forces Yahoo quoteSummary for every symbol in the configured universe. Partial success is allowed: check
     * {@code failures} for per-symbol errors (e.g. rate limits or missing Yahoo data).
     */
    @PostMapping("/fundamentals/refresh-all")
    public FundamentalsBulkRefreshResult refreshAllFundamentals() {
        return fundamentalCacheService.refreshAllForce(universeProvider.getSymbols());
    }

    private void requireInUniverse(String symbol) {
        String sym = YahooFinanceMarketDataProvider.normalizeSymbol(symbol);
        if (!universeProvider.getSymbols().contains(sym)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not in universe: " + sym);
        }
    }
}
