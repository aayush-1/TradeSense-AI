package ai.tradesense.domain.fundamentals;

/**
 * Valuation, profitability, and balance-sheet style metrics merged from Yahoo modules
 * {@code defaultKeyStatistics}, {@code summaryDetail}, and {@code financialData} (names differ slightly per ticker).
 * All monetary fields are as returned by Yahoo (typically raw numbers, not scaled); use with care across currencies.
 */
public record FundamentalMetrics(
        // Total equity market capitalization (common interpretation: price × diluted shares).
        Double marketCap,
        // Company value incl. debt: market cap + net debt (or equivalent per Yahoo’s definition).
        Double enterpriseValue,
        // EV divided by trailing-twelve-month revenue (valuation vs sales).
        Double enterpriseToRevenue,
        // EV divided by EBITDA (leverage-adjusted multiple vs operating cash generation proxy).
        Double enterpriseToEbitda,
        // Price divided by trailing twelve months EPS (TTM P/E).
        Double trailingPe,
        // Price divided by consensus forward EPS estimate (forward P/E).
        Double forwardPe,
        // P/E divided by expected EPS growth (growth-adjusted valuation; often null if growth missing).
        Double pegRatio,
        // Price per share divided by book value per share (P/B).
        Double priceToBook,
        // Price divided by trailing-twelve-month revenue per share (P/S TTM).
        Double priceToSalesTrailing12Months,
        // Sensitivity vs broad market index over Yahoo’s measurement window (not fundamental “quality”).
        Double beta,
        // All issued shares still outstanding (often used with price for market cap).
        Long sharesOutstanding,
        // Shares available for public trading (excludes tight holdings; often ≤ outstanding).
        Long floatShares,
        // Book value per share (equity / shares; anchor for P/B).
        Double bookValue,
        // YoY or sequential EPS growth rate as Yahoo exposes it for the latest quarter (check units in source).
        Double earningsQuarterlyGrowth,
        // Revenue growth rate as Yahoo exposes it (period basis varies by field in API).
        Double revenueGrowth,
        // Trailing-twelve-month revenue divided by diluted share count.
        Double revenuePerShare,
        // Diluted EPS over the trailing twelve months (basis for trailing P/E).
        Double trailingEps,
        // Consensus next-period EPS estimate (basis for forward P/E).
        Double forwardEps,
        // Total revenue (TTM or as-reported scale from Yahoo; confirm raw vs billions in parser).
        Double totalRevenue,
        // Earnings before interest, taxes, depreciation and amortization (operating proxy).
        Double ebitda,
        // Gross profit as a fraction of revenue (margin 0–1 or 0–100 per Yahoo; normalize in parser if needed).
        Double grossMargins,
        // Operating income as a fraction of revenue.
        Double operatingMargins,
        // Net income as a fraction of revenue (bottom-line margin).
        Double profitMargins,
        // Net income divided by shareholders’ equity (return on equity).
        Double returnOnEquity,
        // Net income divided by total assets (return on assets).
        Double returnOnAssets,
        // Total interest-bearing debt (or Yahoo’s “totalDebt” definition for the ticker).
        Double totalDebt,
        // Cash and near-cash (liquidity; used with debt for net debt / enterprise value narratives).
        Double totalCash,
        // Total debt divided by total equity (leverage).
        Double debtToEquity,
        // Current assets divided by current liabilities (short-term liquidity).
        Double currentRatio,
        // (Current assets − inventory) / current liabilities (stricter liquidity than current ratio).
        Double quickRatio,
        // Cash from operations (TTM or latest as Yahoo provides).
        Double operatingCashflow,
        // Operating cash flow minus capex (common FCF definition when both lines exist).
        Double freeCashflow,
        // Annual dividend per share / price (forward or trailing per Yahoo module).
        Double dividendYield,
        // Dividends as a fraction of earnings (sustainability of payout).
        Double payoutRatio,
        // Mean analyst price target (consensus).
        Double targetMeanPrice,
        // Low analyst price target in the sampled consensus.
        Double targetLowPrice,
        // High analyst price target in the sampled consensus.
        Double targetHighPrice,
        // Mean analyst recommendation score (Yahoo scale: e.g. 1 = strong buy, 5 = sell—confirm in UI/docs).
        Double recommendationMean
) {
}
