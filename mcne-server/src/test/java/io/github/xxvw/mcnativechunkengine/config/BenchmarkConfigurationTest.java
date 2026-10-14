package io.github.xxvw.mcnativechunkengine.config;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkConfigurationTest {
    @TempDir Path temp;
    @Test void safeDefaultsAndLimits() throws Exception {
        Path file=temp.resolve("config.yml"); Files.writeString(file,""); var c=BenchmarkConfiguration.load(file);
        assertEquals(16,c.maxRadius()); assertEquals(1089,c.maxChunks()); assertEquals(4,c.concurrentRequests());
        assertEquals(30,c.beforeSeconds()); assertEquals(1000,c.sampleMs());
        for(String invalid:new String[]{"benchmark:\n  allowExistingWorlds: true\n", "benchmark:\n  maxConcurrentRuns: 2\n",
            "benchmark:\n  managedWorldRoot: ../world\n", "benchmark:\n  maxRadiusChunks: 17\n",
            "benchmark:\n  sampleIntervalMs: 1\n", "reports:\n  redactSensitiveEnvironment: false\n"}) {
            Files.writeString(file,invalid); assertThrows(Exception.class,()->BenchmarkConfiguration.load(file));
        }
    }
}
