package io.github.xxvw.mcnativechunkengine.config;

import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;

public record BenchmarkConfiguration(boolean enabled, Path worlds, Path reports, int maxRadius, int maxChunks,
    int runtimeSeconds, long diskBytes, int cooldownSeconds, int sampleMs, int beforeSeconds, int afterSeconds,
    int retainedRuns, int maxDistance, int concurrentRequests, boolean restoreOnRequest) {
    public static BenchmarkConfiguration load(Path file) throws Exception {
        YamlConfiguration y = new YamlConfiguration(); y.load(file.toFile());
        for (String key : java.util.List.of("benchmark.enabled", "benchmark.allowExistingWorlds", "reports.redactSensitiveEnvironment", "benchmark.restorePlayerOnRequest"))
            if (y.contains(key) && !y.isBoolean(key)) throw new IllegalArgumentException("MCNE_CONFIG_TYPE");
        if (y.contains("benchmark.maxDiskBytes") && !(y.get("benchmark.maxDiskBytes") instanceof Number))
            throw new IllegalArgumentException("MCNE_CONFIG_TYPE");
        if (y.contains("benchmark.managedWorldRoot") && !(y.get("benchmark.managedWorldRoot") instanceof String)
            || y.contains("reports.root") && !(y.get("reports.root") instanceof String)) throw new IllegalArgumentException("MCNE_CONFIG_TYPE");
        if (y.getBoolean("benchmark.allowExistingWorlds", false) || y.getInt("benchmark.maxConcurrentRuns", 1) != 1
            || !y.getBoolean("reports.redactSensitiveEnvironment", true)) throw new IllegalArgumentException("MCNE_UNSAFE_CONFIG");
        Path worlds=relative(y.getString("benchmark.managedWorldRoot", "bench-worlds"));
        Path reports=relative(y.getString("reports.root", "benchmarks/results"));
        if(worlds.startsWith(reports)||reports.startsWith(worlds)||worlds.startsWith(".mcne-private")||reports.startsWith(".mcne-private"))
            throw new IllegalArgumentException("MCNE_OVERLAPPING_ROOTS");
        return new BenchmarkConfiguration(y.getBoolean("benchmark.enabled", true),
            worlds, reports,
            integer(y, "benchmark.maxRadiusChunks", 16, 0, 16), integer(y, "benchmark.maxChunks", 1089, 1, 1089),
            integer(y, "benchmark.maxRuntimeSeconds", 900, 1, 900),
            Math.min(10_737_418_240L, positive(y.getLong("benchmark.maxDiskBytes", 10_737_418_240L))),
            integer(y, "benchmark.cooldownSeconds", 60, 0, 86400), integer(y, "benchmark.sampleIntervalMs", 1000, 250, 60000),
            integer(y, "benchmark.beforeSeconds", 30, 0, 300), integer(y, "benchmark.afterSeconds", 30, 0, 300),
            integer(y, "reports.maxRetainedRuns", 100, 1, 100), integer(y, "benchmark.maxTeleportDistanceBlocks", 1000000, 1, 1000000),
            integer(y, "benchmark.maxConcurrentRequests", 4, 1, 4), y.getBoolean("benchmark.restorePlayerOnRequest", true));
    }
    private static long positive(long v) { if (v <= 0) throw new IllegalArgumentException("MCNE_CONFIG_RANGE"); return v; }
    private static int integer(YamlConfiguration y, String key, int fallback, int min, int max) {
        if (y.contains(key) && !y.isInt(key)) throw new IllegalArgumentException("MCNE_CONFIG_TYPE: " + key);
        int v = y.getInt(key, fallback); if (v < min || v > max) throw new IllegalArgumentException("MCNE_CONFIG_RANGE: " + key); return v;
    }
    private static Path relative(String value) {
        Path p = Path.of(value).normalize();
        if (p.isAbsolute() || p.startsWith("..") || p.toString().isEmpty() || p.equals(Path.of(".")))
            throw new IllegalArgumentException("MCNE_CONFIG_PATH");
        return p;
    }
}
