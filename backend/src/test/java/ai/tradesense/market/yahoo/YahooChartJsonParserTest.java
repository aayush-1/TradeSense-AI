package ai.tradesense.market.yahoo;

import ai.tradesense.domain.Ohlc;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YahooChartJsonParserTest {

    @Test
    void parsesSampleRelianceFiveDay() throws Exception {
        String json = new String(
                getClass().getResourceAsStream("/yahoo-reliance-5d-sample.json").readAllBytes(),
                StandardCharsets.UTF_8);
        List<Ohlc> bars = YahooChartJsonParser.parseBarsFromJson(json, "RELIANCE");

        assertThat(bars).hasSize(5);
        Ohlc last = bars.get(4);
        assertThat(last.symbol()).isEqualTo("RELIANCE");
        assertThat(last.close()).isBetween(1363.29, 1363.31);
        assertThat(last.volume()).isEqualTo(13611763L);
    }
}
