package ai.tradesense.web;

import ai.tradesense.MarketDataConstants;
import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.MarketDataProvider;
import ai.tradesense.storage.OhlcFileStore;
import ai.tradesense.storage.OhlcSeriesMerge;
import ai.tradesense.universe.UniverseProvider;
import ai.tradesense.web.dto.RecommendationResponse;
import ai.tradesense.web.dto.SymbolRecommendation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RecommendationService {

    private final UniverseProvider universeProvider;
    private final MarketDataProvider marketDataProvider;
    private final OhlcFileStore ohlcFileStore;

    public RecommendationService(
            UniverseProvider universeProvider,
            MarketDataProvider marketDataProvider,
            OhlcFileStore ohlcFileStore) {
        this.universeProvider = universeProvider;
        this.marketDataProvider = marketDataProvider;
        this.ohlcFileStore = ohlcFileStore;
    }

    /**
     * Loads or incrementally refreshes OHLC for the fixed universe: keeps ~{@link MarketDataConstants#ANALYSIS_HISTORY_MONTHS}
     * months on disk, merges new Yahoo bars (incoming wins on same date). OHLC is used only internally; the API returns buy/skip
     * rows with optional trade levels once the strategy is implemented.
     */
    public RecommendationResponse buildResponse() {
        LocalDate toDate = LocalDate.now();
        LocalDate analysisStart = toDate.minusMonths(MarketDataConstants.ANALYSIS_HISTORY_MONTHS);
        List<String> universe = universeProvider.getSymbols();
        List<SymbolRecommendation> recommendations = new ArrayList<>();
        List<String> fetchErrors = new ArrayList<>();

        for (String symbol : universe) {
            try {
                HydrationSummary h = hydrateAndPersist(symbol, analysisStart, toDate);
                List<Ohlc> series = h.series();
                Double referencePrice =
                        series.isEmpty() ? null : series.get(series.size() - 1).close();
                recommendations.add(
                        new SymbolRecommendation(
                                symbol,
                                false,
                                referencePrice,
                                null,
                                null,
                                null,
                                List.of(
                                        "Recommendation strategy not implemented yet.",
                                        h.marketDataNote())));
            } catch (Exception e) {
                fetchErrors.add(symbol + ": " + e.getMessage());
            }
        }

        recommendations.sort(Comparator.comparing(SymbolRecommendation::symbol));

        return RecommendationResponse.of(universe, recommendations, fetchErrors);
    }

    private HydrationSummary hydrateAndPersist(String symbol, LocalDate analysisStart, LocalDate toDate)
            throws Exception {
        List<Ohlc> loaded = new ArrayList<>(ohlcFileStore.load(symbol));
        loaded.sort(Comparator.comparing(Ohlc::date));
        LocalDate lastOnDisk = loaded.isEmpty() ? null : loaded.get(loaded.size() - 1).date();

        LocalDate fetchFrom;
        LocalDate fetchTo = toDate;
        if (loaded.isEmpty()) {
            fetchFrom = analysisStart;
        } else if (loaded.get(0).date().isAfter(analysisStart)) {
            // Earliest bar is after the analysis window start (e.g. weekend vs first session). Backfill only the
            // prefix [analysisStart, day before first on disk], not through toDate — avoids re-downloading months
            // that are already on disk.
            fetchFrom = analysisStart;
            fetchTo = loaded.get(0).date().minusDays(1);
            if (fetchTo.isAfter(toDate)) {
                fetchTo = toDate;
            }
        } else {
            LocalDate last = loaded.get(loaded.size() - 1).date();
            if (last.isBefore(toDate)) {
                fetchFrom = last.plusDays(1);
            } else {
                // Same calendar day as "to" (or newer on disk): still ask the provider for [toDate, toDate] so the
                // latest candle can replace the prior row for that date when saved.
                fetchFrom = toDate;
                fetchTo = toDate;
            }
        }

        boolean providerInvoked = !fetchFrom.isAfter(fetchTo);
        List<Ohlc> fetched = List.of();
        if (providerInvoked) {
            fetched = marketDataProvider.getHistoricalData(symbol, fetchFrom, fetchTo);
        }

        List<Ohlc> merged = OhlcSeriesMerge.mergePreferIncoming(loaded, fetched);
        merged = OhlcSeriesMerge.trimNotBefore(merged, analysisStart);
        ohlcFileStore.save(symbol, merged);

        String marketDataNote =
                describeHydration(lastOnDisk, fetchFrom, fetchTo, toDate, providerInvoked, fetched);
        return new HydrationSummary(merged, marketDataNote);
    }

    private static String describeHydration(
            LocalDate lastOnDisk,
            LocalDate fetchFrom,
            LocalDate fetchTo,
            LocalDate toDate,
            boolean providerInvoked,
            List<Ohlc> fetched) {
        if (!providerInvoked) {
            return "No live market request (fetch range "
                    + fetchFrom
                    + "–"
                    + fetchTo
                    + " is empty); persisted OHLC unchanged by this step.";
        }
        if (fetched.isEmpty()) {
            if (lastOnDisk != null && lastOnDisk.equals(toDate) && fetchFrom.equals(toDate) && fetchTo.equals(toDate)) {
                return "Last bar on disk is already "
                        + toDate
                        + "; provider returned no row for that date yet (e.g. before market close or holiday).";
            }
            return "Provider returned no bars for "
                    + fetchFrom
                    + "–"
                    + fetchTo
                    + "; series unchanged aside from trim/save.";
        }
        if (fetchFrom.equals(toDate)
                && fetchTo.equals(toDate)
                && lastOnDisk != null
                && !lastOnDisk.isBefore(toDate)
                && fetched.size() <= 2) {
            return "Replaced/updated the "
                    + toDate
                    + " bar from the provider (same calendar date as the previous last bar on disk is intentional).";
        }
        return "Merged "
                + fetched.size()
                + " provider bar(s) for "
                + fetchFrom
                + "–"
                + fetchTo
                + " into persisted OHLC.";
    }

    private record HydrationSummary(List<Ohlc> series, String marketDataNote) {}
}
