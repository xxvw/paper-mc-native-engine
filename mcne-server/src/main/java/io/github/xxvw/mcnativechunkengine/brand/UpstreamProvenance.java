package io.github.xxvw.mcnativechunkengine.brand;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Locked upstream identity, independent of the fork's build number and Git commit. */
public record UpstreamProvenance(String minecraftVersion, String paperBuild, String paperCommit) {
    public static final String BRAND = "paper-chunk-native-engine";

    public static UpstreamProvenance load() {
        try (InputStream stream = UpstreamProvenance.class.getResourceAsStream("/mcne-upstream.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing locked MCNE upstream provenance");
            }
            JsonObject lock = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"STABLE".equals(lock.get("paperChannel").getAsString())) {
                throw new IllegalStateException("MCNE upstream baseline must be STABLE");
            }
            return new UpstreamProvenance(lock.get("minecraftVersion").getAsString(),
                lock.get("paperBuild").getAsString(), lock.get("paperSourceCommit").getAsString());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read locked MCNE provenance", exception);
        }
    }

    public String display() {
        return "Upstream: Paper " + this.minecraftVersion + " build " + this.paperBuild + " @ " + this.paperCommit;
    }
}
