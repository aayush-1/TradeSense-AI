package ai.tradesense.web.dto;

import ai.tradesense.recommendation.levels.TradeLevelsSnapshot;

import java.util.List;

/** Optional suggested placement levels when overall signal is buy; method is implementation-defined (e.g. ATR). */
public record TradeLevelsSuggestion(
        String methodId,
        String methodLabel,
        String methodDescription,
        double entryPrice,
        double stopLoss,
        double takeProfit,
        double riskPerShare,
        double rewardPerShare,
        List<String> detailLines) {

    public static TradeLevelsSuggestion fromSnapshot(TradeLevelsSnapshot s) {
        return new TradeLevelsSuggestion(
                s.methodId(),
                s.methodLabel(),
                s.methodDescription(),
                s.entryPrice(),
                s.stopLoss(),
                s.takeProfit(),
                s.riskPerShare(),
                s.rewardPerShare(),
                List.copyOf(s.detailLines()));
    }
}
