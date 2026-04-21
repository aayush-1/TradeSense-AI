package ai.tradesense.market.yahoo;

import ai.tradesense.domain.fundamentals.FundamentalCalendar;
import ai.tradesense.domain.fundamentals.FundamentalMetrics;
import ai.tradesense.domain.fundamentals.FundamentalProfile;
import ai.tradesense.domain.fundamentals.FundamentalSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Maps Yahoo {@code quoteSummary} JSON into {@link FundamentalSnapshot}. Tolerates missing modules/fields.
 */
@Component
public final class YahooQuoteSummaryMapper {

    private final ObjectMapper objectMapper;

    public YahooQuoteSummaryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FundamentalSnapshot map(String nseSymbol, String jsonBody, Instant fetchedAt)
            throws JsonProcessingException {
        String sym = YahooFinanceMarketDataProvider.normalizeSymbol(nseSymbol);
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode q = root.path("quoteSummary");
        if (q.has("error") && !q.path("error").isNull()) {
            throw new IllegalStateException("Yahoo quoteSummary error: " + q.path("error").toString());
        }
        JsonNode result = q.path("result");
        if (!result.isArray() || result.isEmpty()) {
            throw new IllegalStateException("Yahoo quoteSummary has no result for " + sym);
        }
        JsonNode r = result.get(0);

        JsonNode asset = r.path("assetProfile");
        JsonNode summaryProf = r.path("summaryProfile");
        JsonNode fd = r.path("financialData");
        JsonNode dk = r.path("defaultKeyStatistics");
        JsonNode sd = r.path("summaryDetail");
        JsonNode ce = r.path("calendarEvents");

        FundamentalProfile profile = mapProfile(asset, summaryProf);
        FundamentalMetrics metrics = mapMetrics(fd, dk, sd);
        FundamentalCalendar calendar = mapCalendar(ce);

        return new FundamentalSnapshot(sym, fetchedAt, "Yahoo", profile, metrics, calendar);
    }

    private static FundamentalProfile mapProfile(JsonNode asset, JsonNode summaryProf) {
        JsonNode a = asset.isMissingNode() || asset.isNull() ? summaryProf : asset;
        if (a.isMissingNode() || a.isNull()) {
            return null;
        }
        return new FundamentalProfile(
                text(a, "sector"),
                text(a, "industry"),
                text(a, "country"),
                text(a, "website"),
                longRaw(a, "fullTimeEmployees"),
                text(a, "longBusinessSummary"));
    }

    private static FundamentalMetrics mapMetrics(JsonNode fd, JsonNode dk, JsonNode sd) {
        return new FundamentalMetrics(
                firstD(num(sd, "marketCap"), num(dk, "marketCap")),
                firstD(num(dk, "enterpriseValue")),
                firstD(num(dk, "enterpriseToRevenue")),
                firstD(num(dk, "enterpriseToEbitda")),
                firstD(num(dk, "trailingPE"), num(sd, "trailingPE")),
                firstD(num(dk, "forwardPE"), num(sd, "forwardPE")),
                num(dk, "pegRatio"),
                firstD(num(dk, "priceToBook"), num(sd, "priceToBook")),
                firstD(num(dk, "priceToSalesTrailing12Months")),
                firstD(num(dk, "beta"), num(sd, "beta")),
                firstL(longRaw(dk, "sharesOutstanding")),
                firstL(longRaw(dk, "floatShares")),
                firstD(num(dk, "bookValue")),
                firstD(num(dk, "earningsQuarterlyGrowth")),
                firstD(num(fd, "revenueGrowth")),
                firstD(num(dk, "revenuePerShare")),
                firstD(num(dk, "trailingEps")),
                firstD(num(dk, "forwardEps")),
                firstD(num(fd, "totalRevenue")),
                firstD(num(fd, "ebitda")),
                firstD(num(fd, "grossMargins")),
                firstD(num(fd, "operatingMargins")),
                firstD(num(fd, "profitMargins")),
                firstD(num(fd, "returnOnEquity")),
                firstD(num(fd, "returnOnAssets")),
                firstD(num(fd, "totalDebt")),
                firstD(num(fd, "totalCash")),
                firstD(num(fd, "debtToEquity")),
                firstD(num(fd, "currentRatio")),
                firstD(num(fd, "quickRatio")),
                firstD(num(fd, "operatingCashflow")),
                firstD(num(fd, "freeCashflow")),
                firstD(num(sd, "dividendYield"), num(dk, "dividendYield")),
                firstD(num(sd, "payoutRatio"), num(dk, "payoutRatio")),
                firstD(num(fd, "targetMeanPrice")),
                firstD(num(fd, "targetLowPrice")),
                firstD(num(fd, "targetHighPrice")),
                firstD(num(fd, "recommendationMean")));
    }

    private static FundamentalCalendar mapCalendar(JsonNode ce) {
        if (ce.isMissingNode() || ce.isNull()) {
            return null;
        }
        LocalDate earnings = firstCalendarEpoch(ce.path("earnings").path("earningsDate"));
        LocalDate exDiv = firstCalendarEpoch(ce.path("exDividendDate"));
        if (exDiv == null) {
            exDiv = firstCalendarEpoch(ce.path("exDividendDates"));
        }
        LocalDate div = firstCalendarEpoch(ce.path("dividendDate"));
        if (earnings == null && exDiv == null && div == null) {
            return null;
        }
        return new FundamentalCalendar(earnings, exDiv, div);
    }

    private static LocalDate firstCalendarEpoch(JsonNode node) {
        if (node.isArray() && node.size() > 0) {
            return epochToLocalDate(node.get(0));
        }
        if (node.isObject() && !node.isEmpty()) {
            return epochToLocalDate(node);
        }
        return null;
    }

    private static LocalDate epochToLocalDate(JsonNode rawFmt) {
        if (rawFmt == null || rawFmt.isMissingNode() || rawFmt.isNull()) {
            return null;
        }
        JsonNode raw = rawFmt.get("raw");
        if (raw == null || raw.isNull() || !raw.isNumber()) {
            return null;
        }
        long epoch = raw.asLong();
        return Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static Double firstD(Double... vals) {
        for (Double v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Long firstL(Long... vals) {
        for (Long v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static String text(JsonNode parent, String field) {
        if (parent == null || parent.isMissingNode() || !parent.has(field)) {
            return null;
        }
        JsonNode n = parent.get(field);
        if (n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            String s = n.asText();
            return s.isBlank() ? null : s;
        }
        if (n.isObject() && n.has("fmt")) {
            JsonNode fmt = n.get("fmt");
            if (fmt != null && fmt.isTextual()) {
                String s = fmt.asText();
                return s.isBlank() ? null : s;
            }
        }
        return null;
    }

    private static Double num(JsonNode parent, String field) {
        if (parent == null || parent.isMissingNode() || !parent.has(field)) {
            return null;
        }
        JsonNode n = parent.get(field);
        if (n.isNull() || n.isMissingNode()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asDouble();
        }
        if (n.isObject() && n.has("raw") && n.get("raw").isNumber()) {
            return n.get("raw").asDouble();
        }
        return null;
    }

    private static Long longRaw(JsonNode parent, String field) {
        if (parent == null || parent.isMissingNode() || !parent.has(field)) {
            return null;
        }
        JsonNode n = parent.get(field);
        if (n.isNull()) {
            return null;
        }
        if (n.isIntegralNumber()) {
            return n.asLong();
        }
        if (n.isObject() && n.has("raw") && n.get("raw").isIntegralNumber()) {
            return n.get("raw").asLong();
        }
        if (n.isObject() && n.has("raw") && n.get("raw").isNumber()) {
            return n.get("raw").asLong();
        }
        return null;
    }
}
