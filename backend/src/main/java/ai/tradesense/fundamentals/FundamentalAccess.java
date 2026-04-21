package ai.tradesense.fundamentals;

import ai.tradesense.domain.fundamentals.FundamentalSnapshot;

/** Result of a fundamentals read/refresh: snapshot plus whether it was served from disk without a Yahoo call. */
public record FundamentalAccess(FundamentalSnapshot snapshot, boolean servedFromCache) {
}
