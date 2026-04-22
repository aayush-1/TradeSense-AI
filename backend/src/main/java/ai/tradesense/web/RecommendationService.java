package ai.tradesense.web;

import ai.tradesense.MarketDataConstants;
import ai.tradesense.config.RecommendationStrategyProperties;
import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.MarketDataProvider;
import ai.tradesense.recommendation.RecommendationContext;
import ai.tradesense.recommendation.RecommendationStrategy;
import ai.tradesense.recommendation.StrategyEvaluation;
import ai.tradesense.recommendation.WeightedRecommendationAggregator;
import ai.tradesense.storage.OhlcFileStore;
import ai.tradesense.storage.OhlcSeriesMerge;
import ai.tradesense.universe.UniverseProvider;
import ai.tradesense.web.dto.OverallRecommendation;
import ai.tradesense.web.dto.RecommendationResponse;
import ai.tradesense.web.dto.StrategyRecommendation;
import ai.tradesense.web.dto.SymbolRecommendation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds recommendation responses by hydrating OHLC and running all {@link RecommendationStrategy} beans. */
@Service
public class RecommendationService {

    /** NSE session dates on OHLC bars use this zone (see {@link ai.tradesense.market.yahoo.YahooChartJsonParser}). */
    private static final ZoneId NSE_CALENDAR = ZoneId.of("Asia/Kolkata");

    private final UniverseProvider universeProvider;
    private final MarketDataProvider marketDataProvider;
    private final OhlcFileStore ohlcFileStore;
    private final List<RecommendationStrategy> strategies;
    private final WeightedRecommendationAggregator aggregator;
    private final RecommendationStrategyProperties recommendationStrategyProperties;

    public RecommendationService(
            UniverseProvider universeProvider,
            MarketDataProvider marketDataProvider,
            OhlcFileStore ohlcFileStore,
            List<RecommendationStrategy> strategies,
            WeightedRecommendationAggregator aggregator,
            RecommendationStrategyProperties recommendationStrategyProperties) {
        this.universeProvider = universeProvider;
        this.marketDataProvider = marketDataProvider;
        this.ohlcFileStore = ohlcFileStore;
        this.strategies =
                strategies.stream().sorted(Comparator.comparing(RecommendationStrategy::strategyId)).toList();
        this.aggregator = aggregator;
        this.recommendationStrategyProperties = recommendationStrategyProperties;
    }

    /**
     * Loads or incrementally refreshes OHLC for the fixed universe: keeps ~{@link MarketDataConstants#ANALYSIS_HISTORY_MONTHS}
     * months on disk, merges new Yahoo bars (incoming wins on same date). Runs every {@link RecommendationStrategy} and
     * aggregates a weighted overall buy/skip; no raw OHLC in the response.
     */
    public RecommendationResponse buildResponse() {
        LocalDate toDate = LocalDate.now(NSE_CALENDAR);
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
                recommendations.add(buildSymbolRecommendation(symbol, series, referencePrice, h.marketDataNote()));
            } catch (Exception e) {
                fetchErrors.add(symbol + ": " + e.getMessage());
            }
        }

        recommendations.sort(Comparator.comparing(SymbolRecommendation::symbol));

        return RecommendationResponse.of(universe, recommendations, fetchErrors);
    }

    private SymbolRecommendation buildSymbolRecommendation(
            String symbol, List<Ohlc> series, Double referencePrice, String marketDataNote) {
        RecommendationContext ctx = new RecommendationContext(symbol, series, referencePrice);
        List<StrategyRecommendation> perStrategy = new ArrayList<>();
        for (RecommendationStrategy st : strategies) {
            double weight =
                    recommendationStrategyProperties.resolveWeight(st.strategyId(), st.defaultWeight());
            try {
                StrategyEvaluation ev = st.evaluate(ctx);
                perStrategy.add(
                        new StrategyRecommendation(
                                st.strategyId(),
                                st.displayName(),
                                ev.buy(),
                                weight,
                                ev.rationale(),
                                ev.includedInAggregation()));
            } catch (Exception e) {
                perStrategy.add(
                        new StrategyRecommendation(
                                st.strategyId(),
                                st.displayName(),
                                false,
                                weight,
                                List.of("Strategy failed: " + e.getMessage()),
                                false));
            }
        }
        OverallRecommendation overall =
                aggregator.aggregate(perStrategy, recommendationStrategyProperties.getAggregationBuyThreshold());
        List<String> notes = new ArrayList<>();
        if (marketDataNote != null && !marketDataNote.isBlank()) {
            notes.add(marketDataNote);
        }
        return new SymbolRecommendation(symbol, referencePrice, overall, List.copyOf(perStrategy), List.copyOf(notes));
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
