package io.github.xxvw.mcnativechunkengine.metrics;

import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded tick observations; resource samples and aggregation run off the tick thread. */
public final class MetricsSampler {
    private record Tick(long at, double ms) {}
    private final ArrayDeque<Tick> ticks = new ArrayDeque<>();
    private long overwritten;
    private volatile double[] tps = {Double.NaN, Double.NaN, Double.NaN};
    private long lastCpu = -1, lastTime = -1;
    private long collectorNs;
    public synchronized void tick(double ms, double[] rolling) {
        long started = System.nanoTime();
        if (ticks.size() == 32768) { ticks.removeFirst(); overwritten++; }
        ticks.addLast(new Tick(System.nanoTime(), ms)); tps = rolling.clone();
        collectorNs += System.nanoTime() - started;
    }
    public JsonObject sample(long since) {
        return sample(since, Long.MAX_VALUE);
    }
    public JsonObject sample(long since, long until) {
        long started = System.nanoTime();
        JsonObject j = new JsonObject(); j.addProperty("timestampUtc", Instant.now().toString());
        List<Tick> observed;
        synchronized (this) {
            observed = List.copyOf(ticks);
            j.addProperty("lostTickSamplesTotal", overwritten);
            j.addProperty("tickCollectorTotalOverheadNs", collectorNs);
        }
        List<Double> values = new ArrayList<>();
        for (Tick t : observed) if (t.at >= since && t.at <= until) values.add(t.ms);
        values.sort(Comparator.naturalOrder());
        j.addProperty("tickSampleCount", values.size());
        j.addProperty("tickSource", "Paper ServerTickEndEvent duration (ms)");
        j.addProperty("over50msCount", values.stream().filter(v -> v > 50).count());
        for (int i = 0; i < 3; i++) number(j, new String[]{"tps1m", "tps5m", "tps15m"}[i], tps[i]);
        number(j, "msptP50", quantile(values, .5)); number(j, "msptP95", quantile(values, .95));
        number(j, "msptP99", quantile(values, .99)); number(j, "msptMax", values.isEmpty() ? Double.NaN : values.getLast());
        j.addProperty("heapUsedBytes", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        long count = 0, millis = 0; boolean supported = true;
        for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() < 0 || bean.getCollectionTime() < 0) supported = false;
            else { count += bean.getCollectionCount(); millis += bean.getCollectionTime(); }
        }
        if (supported) { j.addProperty("gcCount", count); j.addProperty("gcCollectionTimeMs", millis); }
        else { j.add("gcCount", null); j.add("gcCollectionTimeMs", null); }
        long now = System.nanoTime();
        long cpu = ProcessHandle.current().info().totalCpuDuration().map(java.time.Duration::toNanos).orElse(-1L);
        double pct = lastTime >= 0 && lastCpu >= 0 && cpu >= lastCpu
            ? 100.0 * (cpu - lastCpu) / (now - lastTime) / Runtime.getRuntime().availableProcessors() : Double.NaN;
        number(j, "processCpuPct", pct); j.addProperty("cpuSource", "ProcessHandle totalCpuDuration delta; normalized by available processors");
        lastTime = now; lastCpu = cpu;
        j.add("rssBytes", null); j.add("diskBytesRead", null);
        j.addProperty("samplerOverheadNs", System.nanoTime() - started);
        return j;
    }
    public static double quantile(List<Double> sorted, double q) {
        return sorted.isEmpty() ? Double.NaN : sorted.get(Math.max(0, (int)Math.ceil(q * sorted.size()) - 1));
    }
    private static void number(JsonObject j, String name, double value) {
        if (Double.isFinite(value)) j.addProperty(name, value); else j.add(name, null);
    }
}
