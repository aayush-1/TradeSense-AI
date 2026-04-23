package ai.tradesense.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes via a temp file then replaces the destination. Tries an atomic rename first; falls back when the file system
 * does not support {@link StandardCopyOption#ATOMIC_MOVE} (seen on some macOS/APFS setups), which otherwise surfaces
 * as {@code SYMBOL: ... .tmp -> ... .json} in API error lists.
 */
final class AtomicReplacingMove {

    private AtomicReplacingMove() {}

    static void move(Path tmp, Path dest) throws IOException {
        try {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
