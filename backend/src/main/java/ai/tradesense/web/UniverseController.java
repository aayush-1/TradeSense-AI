package ai.tradesense.web;

import ai.tradesense.market.MarketSegment;
import ai.tradesense.universe.SegmentUniverseProvider;
import ai.tradesense.web.dto.UniverseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class UniverseController {

    private final SegmentUniverseProvider segmentUniverseProvider;

    public UniverseController(SegmentUniverseProvider segmentUniverseProvider) {
        this.segmentUniverseProvider = segmentUniverseProvider;
    }

    /** Lists all stock symbols included in recommendation runs (fixed universe configuration). */
    @GetMapping("/universe")
    public UniverseResponse universe(@RequestParam(required = false) String segment) {
        try {
            return new UniverseResponse(segmentUniverseProvider.getSymbols(MarketSegment.fromNullable(segment)));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
