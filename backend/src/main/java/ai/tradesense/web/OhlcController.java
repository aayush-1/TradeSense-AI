package ai.tradesense.web;

import ai.tradesense.market.MarketSegment;
import ai.tradesense.web.dto.OhlcSeriesResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
    public OhlcSeriesResponse ohlcForSymbol(
            @PathVariable String symbol,
            @RequestParam(required = false) String segment) throws IOException {
        try {
            return ohlcChartService.loadChartSeries(MarketSegment.fromNullable(segment), symbol);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
