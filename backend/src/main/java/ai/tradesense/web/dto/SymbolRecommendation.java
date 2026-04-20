package ai.tradesense.web.dto;

import java.util.List;

/**
 * Actionable swing-style view for one symbol. When {@code buy} is false, trade fields are typically null.
 * OHLC is not exposed; {@code referencePrice} is usually the latest close used for the call (if data exists).
 */
public record SymbolRecommendation(
        String symbol,
        boolean buy,
        Double referencePrice,
        Double stopLoss,
        Double target,
        Integer suggestedHoldingDays,
        List<String> rationale
) {
}
