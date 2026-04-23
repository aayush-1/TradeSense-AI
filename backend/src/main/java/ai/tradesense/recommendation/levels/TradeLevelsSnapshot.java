package ai.tradesense.recommendation.levels;

import java.util.List;

/** In-process trade level line before mapping to {@link ai.tradesense.web.dto.TradeLevelsSuggestion}. */
public record TradeLevelsSnapshot(
        String methodId,
        String methodLabel,
        String methodDescription,
        double entryPrice,
        double stopLoss,
        double takeProfit,
        double riskPerShare,
        double rewardPerShare,
        List<String> detailLines) {}
