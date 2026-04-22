package ai.tradesense.web.dto;

import java.util.List;

/** OHLC series read from disk for UI charts (no live Yahoo call). */
public record OhlcSeriesResponse(String symbol, List<OhlcBarResponse> bars) {}
