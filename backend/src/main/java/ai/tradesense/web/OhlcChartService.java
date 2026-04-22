package ai.tradesense.web;

import ai.tradesense.MarketDataConstants;
import ai.tradesense.domain.Ohlc;
import ai.tradesense.market.yahoo.YahooFinanceMarketDataProvider;
import ai.tradesense.storage.OhlcFileStore;
import ai.tradesense.universe.UniverseProvider;
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

    private final OhlcFileStore ohlcFileStore;
    private final UniverseProvider universeProvider;

    public OhlcChartService(OhlcFileStore ohlcFileStore, UniverseProvider universeProvider) {
        this.ohlcFileStore = ohlcFileStore;
        this.universeProvider = universeProvider;
    }

    public OhlcSeriesResponse loadChartSeries(String symbol) throws IOException {
        String sym = YahooFinanceMarketDataProvider.normalizeSymbol(symbol);
        if (!universeProvider.getSymbols().contains(sym)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not in universe: " + sym);
        }
        List<Ohlc> raw = new ArrayList<>(ohlcFileStore.load(sym));
        raw.sort(Comparator.comparing(Ohlc::date));
        LocalDate cutoff = LocalDate.now(NSE_CALENDAR).minusMonths(MarketDataConstants.ANALYSIS_HISTORY_MONTHS);
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
