package ai.tradesense.web;

import ai.tradesense.MarketDataConstants;
import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.MarketSegment;
import ai.tradesense.storage.OhlcFileStore;
import ai.tradesense.universe.SegmentUniverseProvider;
import ai.tradesense.web.dto.OhlcBarResponse;
import ai.tradesense.web.dto.OhlcSeriesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Serves on-disk OHLC for charting; universe-gated. */
@Service
public class OhlcChartService {

    private static final ZoneId NSE_CALENDAR = ZoneId.of("Asia/Kolkata");
    private static final ZoneId CRYPTO_CALENDAR = ZoneId.of("UTC");
    private static final int CRYPTO_CHART_HISTORY_MONTHS = 12;

    private final OhlcFileStore ohlcFileStore;
    private final SegmentUniverseProvider segmentUniverseProvider;

    public OhlcChartService(OhlcFileStore ohlcFileStore, SegmentUniverseProvider segmentUniverseProvider) {
        this.ohlcFileStore = ohlcFileStore;
        this.segmentUniverseProvider = segmentUniverseProvider;
    }

    public OhlcSeriesResponse loadChartSeries(MarketSegment segment, String symbol) throws IOException {
        String sym = segmentUniverseProvider.normalizeSymbol(segment, symbol);
        if (!segmentUniverseProvider.getSymbols(segment).contains(sym)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not in universe: " + sym);
        }
        List<Ohlc> raw = new ArrayList<>(ohlcFileStore.load(segment, sym));
        raw.sort(Comparator.comparing(Ohlc::date));
        ZoneId zone = segment == MarketSegment.CRYPTO ? CRYPTO_CALENDAR : NSE_CALENDAR;
        int historyMonths = segment == MarketSegment.CRYPTO
                ? CRYPTO_CHART_HISTORY_MONTHS
                : MarketDataConstants.ANALYSIS_HISTORY_MONTHS;
        LocalDate cutoff = LocalDate.now(zone).minusMonths(historyMonths);
        List<OhlcBarResponse> bars = new ArrayList<>();
        for (Ohlc o : raw) {
            if (o.date().isBefore(cutoff)) {
                continue;
            }
            bars.add(new OhlcBarResponse(o.date(), o.open(), o.high(), o.low(), o.close(), o.volume()));
        }
        return new OhlcSeriesResponse(sym, List.copyOf(bars));
    }
}
