package ai.tradesense.web;

import ai.tradesense.universe.UniverseProvider;
import ai.tradesense.web.dto.UniverseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UniverseController {

    private final UniverseProvider universeProvider;

    public UniverseController(UniverseProvider universeProvider) {
        this.universeProvider = universeProvider;
    }

    /** Lists all stock symbols included in recommendation runs (fixed universe configuration). */
    @GetMapping("/universe")
    public UniverseResponse universe() {
        return new UniverseResponse(universeProvider.getSymbols());
    }
}
