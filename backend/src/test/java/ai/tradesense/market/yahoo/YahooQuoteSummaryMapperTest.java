package ai.tradesense.market.yahoo;

import ai.tradesense.domain.fundamentals.FundamentalSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class YahooQuoteSummaryMapperTest {

    @Test
    void mapsMinimalQuoteSummarySample() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        YahooQuoteSummaryMapper mapper = new YahooQuoteSummaryMapper(objectMapper);
        String json =
                new String(new ClassPathResource("yahoo-quotesummary-minimal.json")
                        .getInputStream()
                        .readAllBytes(), StandardCharsets.UTF_8);

        Instant fetchedAt = Instant.parse("2026-04-20T10:00:00Z");
        FundamentalSnapshot snap = mapper.map("TCS", json, fetchedAt);

        assertEquals("TCS", snap.symbol());
        assertEquals(fetchedAt, snap.fetchedAt());
        assertEquals("Yahoo", snap.dataSource());
        assertNotNull(snap.profile());
        assertEquals("Technology", snap.profile().sector());
        assertNotNull(snap.metrics());
        assertEquals(28.5, snap.metrics().trailingPe(), 0.01);
        assertEquals(58.2, snap.metrics().trailingEps(), 0.01);
        assertNotNull(snap.calendar());
        assertEquals(2025, snap.calendar().earningsDateNext().getYear());
    }
}
