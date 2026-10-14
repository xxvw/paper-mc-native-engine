package io.github.xxvw.mcnativechunkengine.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class Workset {
    private Workset() {}
    public record Coordinate(int x, int z) {}
    public static List<Coordinate> square(int radius, int centerX, int centerZ, int maxRadius, int maxChunks) {
        if (radius < 0 || radius > maxRadius || (2L * radius + 1) * (2L * radius + 1) > maxChunks)
            throw new IllegalArgumentException("MCNE_RADIUS_LIMIT");
        List<Coordinate> result = new ArrayList<>();
        for (int z = -radius; z <= radius; z++) for (int x = -radius; x <= radius; x++) {
            int cx = Math.addExact(centerX, x), cz = Math.addExact(centerZ, z);
            if (Math.abs((long)cx * 16) > 29_999_984 || Math.abs((long)cz * 16) > 29_999_984)
                throw new IllegalArgumentException("MCNE_WORLD_BOUNDARY");
            result.add(new Coordinate(cx, cz));
        }
        return List.copyOf(result);
    }
}
