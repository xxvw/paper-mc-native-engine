package io.github.xxvw.mcnativechunkengine.benchmark;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.concurrentutil.util.Priority;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.craftbukkit.CraftWorld;
import java.util.concurrent.CompletableFuture;

/** Paper owns temporary load tickets and releases them in a finally block. */
public final class ChunkWorkScheduler {
    public CompletableFuture<Boolean> exists(CraftWorld world, Workset.Coordinate c) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        PlatformHooks.get().scheduleChunkLoad(world.getHandle(), c.x(), c.z(), false, ChunkStatus.EMPTY, true,
            Priority.NORMAL, chunk -> result.complete(chunk != null && chunk.getPersistedStatus() == ChunkStatus.FULL));
        return result;
    }
    public CompletableFuture<org.bukkit.Chunk> request(CraftWorld world, Workset.Coordinate c, boolean generate) {
        return world.getChunkAtAsync(c.x(), c.z(), generate, false);
    }
}
