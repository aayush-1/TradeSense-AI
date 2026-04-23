package ai.tradesense.recommendation.levels;

import ai.tradesense.domain.Ohlc;

import java.util.List;

/** Inputs for {@link TradeLevelsCalculator}; bars must be sorted ascending by session date. */
public record TradeLevelsInput(String symbol, List<Ohlc> ohlcSeries, double entryPrice) {}
