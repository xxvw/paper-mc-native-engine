package io.github.xxvw.mcnativechunkengine.benchmark;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Only explicit leased dimension keys can redirect Paper storage. */
public final class ManagedStorage {
    private static final Map<String, Path> LEASES = new ConcurrentHashMap<>();
    private ManagedStorage() {}
    public static void safeId(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]{1,80}")) throw new IllegalArgumentException("MCNE_INVALID_ID");
    }
    public static Path checkedRoot(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        for (Path part = absolute; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("MCNE_SYMLINK_REFUSED");
        }
        Files.createDirectories(absolute);
        if (!absolute.toRealPath().equals(absolute)) throw new IOException("MCNE_NONCANONICAL_ROOT");
        return absolute;
    }
    public static void register(String key, Path path) {
        if (!key.startsWith("mcne:bench_")) throw new IllegalArgumentException("MCNE_INVALID_DIMENSION");
        if (LEASES.putIfAbsent(key, path) != null) throw new IllegalStateException("MCNE_DUPLICATE_LEASE");
    }
    public static Path ownedRoot(Path path, String purpose) throws IOException {
        Path root = checkedRoot(path);
        Path sentinel = root.resolve(".mcne-root");
        if (!Files.exists(sentinel, LinkOption.NOFOLLOW_LINKS)) {
            try (var entries = Files.list(root)) {
                if (entries.findAny().isPresent()) throw new IOException("MCNE_UNKNOWN_ROOT_REFUSED");
            }
            Files.writeString(sentinel, "mcne:" + purpose, StandardOpenOption.CREATE_NEW);
        }
        if (!Files.isRegularFile(sentinel, LinkOption.NOFOLLOW_LINKS)
            || !Files.readString(sentinel).equals("mcne:" + purpose)) throw new IOException("MCNE_INVALID_ROOT_OWNER");
        return root;
    }
    public static Path resolve(String key, Path ordinary) {
        Path leased = LEASES.get(key);
        return leased == null ? ordinary : leased;
    }
    public static void release(String key) { LEASES.remove(key); }
}
