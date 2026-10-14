package io.github.xxvw.mcnativechunkengine.benchmark;

import io.github.xxvw.mcnativechunkengine.config.BenchmarkConfiguration;
import io.github.xxvw.mcnativechunkengine.metrics.MetricsSampler;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkSafetyTest {
    @TempDir Path temp;
    BenchmarkConfiguration config(Path root) {
        return new BenchmarkConfiguration(true,root,root.resolveSibling("reports"),16,1089,900,10737418240L,60,1000,30,30,100,1000000,4,true);
    }
    @Test void worksetIsInclusiveUniqueAndBounded() {
        var s=Workset.square(16,-32,-32,16,1089); assertEquals(1089,s.size()); assertEquals(1089,s.stream().distinct().count());
        assertEquals(new Workset.Coordinate(-48,-48),s.getFirst()); assertEquals(new Workset.Coordinate(-16,-16),s.getLast());
        assertThrows(IllegalArgumentException.class,()->Workset.square(17,0,0,16,1089));
        assertThrows(IllegalArgumentException.class,()->Workset.square(-1,0,0,16,1089));
        assertThrows(IllegalArgumentException.class,()->Workset.square(1,Integer.MAX_VALUE,0,16,1089));
        assertThrows(IllegalArgumentException.class,()->Workset.square(1,1874999,0,16,1089));
    }
    @Test void refusesTraversalAndUnknownRoots() throws Exception {
        assertThrows(IllegalArgumentException.class,()->ManagedStorage.safeId("../world"));
        Path root=temp.toRealPath().resolve("unknown"); Files.createDirectory(root); Files.writeString(root.resolve("level.dat"),"ordinary");
        assertThrows(java.io.IOException.class,()->new DisposableWorldManager(config(root)).prepare("a"));
        assertEquals("ordinary",Files.readString(root.resolve("level.dat")));
    }
    @Test void rejectsSymlinksAndExistingLeases() throws Exception {
        Path real=temp.toRealPath(); Path outside=real.resolve("outside"); Files.createDirectory(outside);
        Path link=real.resolve("link"); Files.createSymbolicLink(link,outside);
        assertThrows(java.io.IOException.class,()->ManagedStorage.checkedRoot(link));
        var manager=new DisposableWorldManager(config(real.resolve("managed"))); var lease=manager.prepare("abc");
        manager.verify(lease); assertThrows(java.io.IOException.class,()->manager.prepare("abc"));
        Files.writeString(lease.directory().resolve(".mcne-managed.json"),"{\"owner\":\"other\",\"runId\":\"abc\"}");
        assertThrows(java.io.IOException.class,()->manager.verify(lease)); assertTrue(Files.exists(outside));
    }
    @Test void redirectsOnlyExplicitLease() throws Exception {
        Path ordinary=temp.toRealPath().resolve("world"); Path managed=temp.toRealPath().resolve("managed");
        assertEquals(ordinary,ManagedStorage.resolve("minecraft:overworld",ordinary));
        ManagedStorage.register("mcne:bench_test",managed);
        try { assertEquals(managed,ManagedStorage.resolve("mcne:bench_test",ordinary)); assertEquals(ordinary,ManagedStorage.resolve("mcne:bench_other",ordinary)); }
        finally { ManagedStorage.release("mcne:bench_test"); }
        assertEquals(ordinary,ManagedStorage.resolve("mcne:bench_test",ordinary));
    }
    @Test void missingTicksAreUnavailableAndQuantilesUseObservedSamples() {
        assertTrue(new MetricsSampler().sample(System.nanoTime()).get("msptP95").isJsonNull());
        assertEquals(90,MetricsSampler.quantile(List.of(1.,2.,90.),.95));
    }
}
