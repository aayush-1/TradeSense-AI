package ai.tradesense.web;

import ai.tradesense.web.dto.RecommendationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * UI entry point: hydrates OHLC for the fixed universe (on-disk window per {@link ai.tradesense.MarketDataConstants}),
     * incrementally fetches missing trailing dates from Yahoo. Response is per-symbol {@code buy} plus optional stop/target/holding
     * days when the strategy is wired; no raw OHLC.
     */
    @GetMapping("/recommendations")
    public RecommendationResponse recommendations() {
        return recommendationService.buildResponse();
    }
}
