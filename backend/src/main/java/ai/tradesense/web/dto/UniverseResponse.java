package ai.tradesense.web.dto;

import java.util.List;

/** Symbols the app evaluates for recommendations (same source as {@code universe} on {@code GET /recommendations}). */
public record UniverseResponse(List<String> symbols) {
}
