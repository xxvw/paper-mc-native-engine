package io.github.xxvw.mcnativechunkengine.benchmark;

import com.google.gson.JsonParser;
import io.github.xxvw.mcnativechunkengine.config.BenchmarkConfiguration;
import io.papermc.paper.math.Position;
import java.io.IOException;
import java.nio.file.*;
import org.bukkit.*;

/** Filesystem operations are performed by the orchestrator's bounded IO executor. */
public final class DisposableWorldManager {
    public record Lease(String id, String key, Path directory) {}
    private final BenchmarkConfiguration config;
    public DisposableWorldManager(BenchmarkConfiguration config) { this.config = config; }
    public Lease prepare(String id) throws IOException {
        ManagedStorage.safeId(id);
        Path root = ManagedStorage.ownedRoot(config.worlds(), "worlds");
        checkQuota(root);
        Path directory = root.resolve(id);
        Files.createDirectory(directory); // never adopt an existing directory
        Files.writeString(directory.resolve(".mcne-managed.json"), "{\"owner\":\"mcne\",\"runId\":\"" + id + "\"}", StandardOpenOption.CREATE_NEW);
        return new Lease(id, "mcne:bench_" + id, directory);
    }
    public void verify(Lease lease) throws IOException {
        Path root = ManagedStorage.ownedRoot(config.worlds(), "worlds");
        if (!lease.directory().equals(root.resolve(lease.id())) || Files.isSymbolicLink(lease.directory())
            || !lease.directory().toRealPath().equals(lease.directory())) throw new IOException("MCNE_INVALID_LEASE");
        Path sentinel = lease.directory().resolve(".mcne-managed.json");
        if (!Files.isRegularFile(sentinel, LinkOption.NOFOLLOW_LINKS)) throw new IOException("MCNE_INVALID_SENTINEL");
        var json = JsonParser.parseString(Files.readString(sentinel)).getAsJsonObject();
        if (!json.get("owner").getAsString().equals("mcne") || !json.get("runId").getAsString().equals(lease.id()))
            throw new IOException("MCNE_INVALID_SENTINEL");
        checkQuota(root);
    }
    public void checkQuota(Path root) throws IOException {
        long bytes = 0;
        try (var walk = Files.walk(root)) {
            var entries = walk.iterator(); int files = 0;
            while (entries.hasNext()) {
                Path p = entries.next();
                if (++files > 100000) throw new IOException("MCNE_FILE_COUNT_LIMIT");
                if (Files.isSymbolicLink(p)) throw new IOException("MCNE_SYMLINK_REFUSED");
                if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) bytes = Math.addExact(bytes, Files.size(p));
                if (bytes > config.diskBytes()) throw new IOException("MCNE_DISK_QUOTA");
            }
        }
        if (Files.getFileStore(root).getUsableSpace() < 64L * 1024 * 1024) throw new IOException("MCNE_DISK_SPACE");
    }
    public World create(Lease lease, long seed) {
        ManagedStorage.register(lease.key(), lease.directory());
        // Verified Paper API avoids synchronous spawn search and incidental bootstrap chunks.
        WorldCreator creator = new WorldCreator(java.util.Objects.requireNonNull(NamespacedKey.fromString(lease.key())))
            .seed(seed).environment(World.Environment.NORMAL).generateStructures(true)
            .forcedSpawnPosition(Position.block(0, 80, 0), 0, 0);
        World world = creator.createWorld();
        if (world == null || world.getSeed() != seed || !world.getWorldFolder().toPath().toAbsolutePath().normalize().equals(lease.directory()))
            throw new IllegalStateException("MCNE_STORAGE_ISOLATION_FAILED");
        return world;
    }
}
