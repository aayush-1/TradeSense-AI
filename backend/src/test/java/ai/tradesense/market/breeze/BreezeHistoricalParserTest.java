package ai.tradesense.market.breeze;

import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BreezeHistoricalParserTest {

    @Test
    void parsesSuccessArray() throws Exception {
        String json = "{\"Status\":200,\"Success\":[{\"close\":1344.1,\"datetime\":\"2024-01-15 00:00:00\","
                + "\"exchange_code\":\"NSE\",\"high\":1347.0,\"low\":1334.2,\"open\":1337.0,\"stock_code\":\"RELIANCE\","
                + "\"volume\":15313779}]}";
        ObjectMapper mapper = new ObjectMapper();
        List<Ohlc> bars = BreezeHistoricalParser.parse(mapper.readTree(json), "RELIANCE", mapper);
        assertThat(bars).hasSize(1);
        assertThat(bars.get(0).close()).isEqualTo(1344.1);
        assertThat(bars.get(0).volume()).isEqualTo(15313779L);
        assertThat(bars.get(0).date().toString()).isEqualTo("2024-01-15");
    }
}
