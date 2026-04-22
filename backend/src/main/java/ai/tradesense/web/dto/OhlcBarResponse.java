package ai.tradesense.web.dto;

import java.time.LocalDate;

/** One daily bar for chart APIs (JSON-friendly). */
public record OhlcBarResponse(LocalDate date, double open, double high, double low, double close, long volume) {}
