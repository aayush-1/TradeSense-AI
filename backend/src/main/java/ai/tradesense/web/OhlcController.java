package ai.tradesense.web;

import ai.tradesense.web.dto.OhlcSeriesResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Read-only OHLC from persisted files for UI charts. Does not call Yahoo; run {@code GET /recommendations} first to
 * hydrate disk. Window matches {@link ai.tradesense.MarketDataConstants#ANALYSIS_HISTORY_MONTHS}.
 */
@RestController
@RequestMapping("/api/v1")
public class OhlcController {

    private final OhlcChartService ohlcChartService;

    public OhlcController(OhlcChartService ohlcChartService) {
        this.ohlcChartService = ohlcChartService;
    }

    @GetMapping("/symbols/{symbol}/ohlc")
    public OhlcSeriesResponse ohlcForSymbol(@PathVariable String symbol) throws IOException {
        return ohlcChartService.loadChartSeries(symbol);
    }
}
