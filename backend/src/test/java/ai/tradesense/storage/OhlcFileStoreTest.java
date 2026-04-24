package ai.tradesense.storage;

import ai.tradesense.config.StorageProperties;
import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OhlcFileStoreTest {

    @Test
    void save_skipsWriteWhenSerializedBytesUnchanged(@TempDir Path dir) throws Exception {
        StorageProperties props = new StorageProperties();
        props.setOhlcDirectory(dir.toString());
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OhlcFileStore store = new OhlcFileStore(props, mapper);

        List<Ohlc> bars =
                List.of(new Ohlc("PETRONET", LocalDate.of(2025, 1, 2), 100, 105, 99, 103, 1_000_000L));
        store.save("PETRONET", bars);

        Path json = dir.resolve("PETRONET.json");
        Path tmp = dir.resolve("PETRONET.json.tmp");
        assertFalse(Files.exists(tmp), "no stale tmp after first save");
        long mtime1 = Files.getLastModifiedTime(json).toMillis();

        store.save("PETRONET", bars);

        assertFalse(Files.exists(tmp), "unchanged payload should not write tmp/replace");
        assertEquals(mtime1, Files.getLastModifiedTime(json).toMillis());
    }
}
