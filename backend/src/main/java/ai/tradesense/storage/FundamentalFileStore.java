package ai.tradesense.storage;

import ai.tradesense.config.StorageProperties;
import ai.tradesense.domain.fundamentals.FundamentalSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists one {@link FundamentalSnapshot} per symbol as pretty JSON (same filename convention as {@link OhlcFileStore}).
 * Used for slow-changing fundamentals; refresh on a schedule separate from daily OHLC.
 */
@Component
public class FundamentalFileStore {

    private final Path directory;
    private final ObjectMapper objectMapper;

    public FundamentalFileStore(StorageProperties properties, ObjectMapper objectMapper) {
        this.directory = properties.resolvedFundamentalsDirectory();
        this.objectMapper = objectMapper;
    }

    public FundamentalSnapshot load(String symbol) throws IOException {
        Path file = fileFor(symbol);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(bytes, FundamentalSnapshot.class);
    }

    public void save(String symbol, FundamentalSnapshot snapshot) throws IOException {
        Files.createDirectories(directory);
        Path file = fileFor(symbol);
        Path tmp = Path.of(file.toString() + ".tmp");
        byte[] data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(snapshot);
        Files.write(tmp, data);
        AtomicReplacingMove.move(tmp, file);
    }

    private Path fileFor(String symbol) {
        String safe = symbol.trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
        if (safe.isEmpty()) {
            throw new IllegalArgumentException("invalid symbol");
        }
        return directory.resolve(safe + ".json");
    }
}
