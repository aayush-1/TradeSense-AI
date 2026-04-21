package ai.tradesense.fundamentals;

import ai.tradesense.config.FundamentalsProperties;
import ai.tradesense.domain.fundamentals.FundamentalSnapshot;
import ai.tradesense.market.yahoo.YahooFinanceMarketDataProvider;
import ai.tradesense.market.yahoo.YahooQuoteSummaryClient;
import ai.tradesense.market.yahoo.YahooQuoteSummaryMapper;
import ai.tradesense.storage.FundamentalFileStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link FundamentalSnapshot} from disk and refreshes from Yahoo {@code quoteSummary} when missing or older than
 * {@link FundamentalsProperties#getMaxAgeDays()}. POST {@code /refresh} uses {@code force=true} and does not fall back to
 * stale disk on failure.
 */
@Service
public class FundamentalCacheService {

    private final FundamentalFileStore store;
    private final YahooQuoteSummaryClient quoteSummaryClient;
    private final YahooQuoteSummaryMapper mapper;
    private final FundamentalsProperties fundamentalsProperties;
    private final Clock clock;

    public FundamentalCacheService(
            FundamentalFileStore store,
            YahooQuoteSummaryClient quoteSummaryClient,
            YahooQuoteSummaryMapper mapper,
            FundamentalsProperties fundamentalsProperties,
            Clock clock) {
        this.store = store;
        this.quoteSummaryClient = quoteSummaryClient;
        this.mapper = mapper;
        this.fundamentalsProperties = fundamentalsProperties;
        this.clock = clock;
    }

    /**
     * @param force if true, always call Yahoo and overwrite disk; on failure throws (no stale fallback).
     */
    public FundamentalAccess getOrRefresh(String symbol, boolean force) {
        String sym = YahooFinanceMarketDataProvider.normalizeSymbol(symbol);
        FundamentalSnapshot existing;
        try {
            existing = store.load(sym);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!force && existing != null && isWithinTtl(existing)) {
            return new FundamentalAccess(existing, true);
        }

        try {
            String json = quoteSummaryClient.fetchQuoteSummaryJson(sym);
            FundamentalSnapshot snap = mapper.map(sym, json, clock.instant());
            store.save(sym, snap);
            return new FundamentalAccess(snap, false);
        } catch (Exception e) {
            if (!force && existing != null) {
                return new FundamentalAccess(existing, true);
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Fundamentals fetch failed for " + sym, e);
        }
    }

    /**
     * Force Yahoo fetch for each symbol; failures are collected per symbol so one bad ticker does not abort the batch.
     */
    public FundamentalsBulkRefreshResult refreshAllForce(List<String> symbols) {
        List<String> succeeded = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (String symbol : symbols) {
            String sym = YahooFinanceMarketDataProvider.normalizeSymbol(symbol);
            try {
                getOrRefresh(sym, true);
                succeeded.add(sym);
            } catch (Exception e) {
                failures.add(sym + ": " + e.getMessage());
            }
        }
        return new FundamentalsBulkRefreshResult(List.copyOf(succeeded), List.copyOf(failures));
    }

    private boolean isWithinTtl(FundamentalSnapshot s) {
        return s.fetchedAt()
                .plus(fundamentalsProperties.getMaxAgeDays(), ChronoUnit.DAYS)
                .isAfter(clock.instant());
    }

}
