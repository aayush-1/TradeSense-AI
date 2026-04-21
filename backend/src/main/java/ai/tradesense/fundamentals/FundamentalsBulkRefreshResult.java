package ai.tradesense.fundamentals;

import java.util.List;

/**
 * Outcome of forcing Yahoo quoteSummary for a batch of symbols (e.g. the full universe).
 */
public record FundamentalsBulkRefreshResult(List<String> succeeded, List<String> failures) {}
