package ai.tradesense.universe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Fixed symbol universe: from an in-memory list or a classpath text file (one symbol per line).
 */
public final class FixedUniverseProvider implements UniverseProvider {

    /** Classpath location of the bundled MVP symbol list. */
    public static final String DEFAULT_CLASSPATH_RESOURCE = "ai/tradesense/universe/fixed-universe.txt";

    private final List<String> symbols;

    public FixedUniverseProvider(Collection<String> symbols) {
        this.symbols = List.copyOf(normalize(symbols));
    }

    /** Loads {@value #DEFAULT_CLASSPATH_RESOURCE}. */
    public static FixedUniverseProvider defaultUniverse() throws IOException {
        return fromClasspathResource(DEFAULT_CLASSPATH_RESOURCE);
    }

    /**
     * Load symbols from a classpath resource. Lines starting with {@code #} and blank lines are ignored.
     *
     * @param resourcePath path suitable for {@link ClassLoader#getResourceAsStream(String)}, e.g.
     *                     {@code "ai/tradesense/universe/fixed-universe.txt"} (no leading slash)
     */
    public static FixedUniverseProvider fromClasspathResource(String resourcePath) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Classpath resource not found: " + resourcePath);
        }
        try (in; BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                lines.add(t);
            }
            return new FixedUniverseProvider(lines);
        }
    }

    private static List<String> normalize(Collection<String> raw) {
        Objects.requireNonNull(raw, "symbols");
        Set<String> ordered = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim().toUpperCase();
            if (!t.isEmpty()) {
                ordered.add(t);
            }
        }
        return List.copyOf(ordered);
    }

    @Override
    public List<String> getSymbols() {
        return symbols;
    }
}
