package ai.tradesense.storage;

import ai.tradesense.config.StorageProperties;
import ai.tradesense.domain.Ohlc;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class OhlcFileStore {

    private final Path directory;
    private final ObjectMapper objectMapper;

    public OhlcFileStore(StorageProperties properties, ObjectMapper objectMapper) {
        this.directory = properties.resolvedOhlcDirectory();
        this.objectMapper = objectMapper;
    }

    public List<Ohlc> load(String symbol) throws IOException {
        Path file = fileFor(symbol);
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length == 0) {
            return List.of();
        }
        List<Ohlc> list = objectMapper.readValue(bytes, new TypeReference<List<Ohlc>>() {
        });
        return list != null ? list : List.of();
    }

    public void save(String symbol, List<Ohlc> bars) throws IOException {
        Files.createDirectories(directory);
        Path file = fileFor(symbol);
        byte[] data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(bars);
        if (AtomicReplacingMove.contentMatches(file, data)) {
            return;
        }
        Path tmp = Path.of(file.toString() + ".tmp");
        try {
            Files.write(tmp, data);
            AtomicReplacingMove.move(tmp, file);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw new IOException("Could not save OHLC for " + symbol + " (" + file + ")", e);
        }
    }

    private Path fileFor(String symbol) {
        String safe = symbol.trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
        if (safe.isEmpty()) {
            throw new IllegalArgumentException("invalid symbol");
        }
        return directory.resolve(safe + ".json");
    }
}
