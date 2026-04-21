package ai.tradesense.domain.fundamentals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FundamentalSnapshotSerdeTest {

    @Test
    void jacksonRoundTrip_withNullNestedSections() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        FundamentalSnapshot original =
                new FundamentalSnapshot(
                        "TCS",
                        Instant.parse("2026-04-20T12:00:00Z"),
                        "Yahoo",
                        null,
                        null,
                        new FundamentalCalendar(LocalDate.of(2026, 5, 1), null, null));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(original);
        FundamentalSnapshot parsed = mapper.readValue(json, FundamentalSnapshot.class);

        assertEquals("TCS", parsed.symbol());
        assertEquals(original.fetchedAt(), parsed.fetchedAt());
        assertNull(parsed.profile());
        assertNull(parsed.metrics());
        assertEquals(LocalDate.of(2026, 5, 1), parsed.calendar().earningsDateNext());
    }
}
