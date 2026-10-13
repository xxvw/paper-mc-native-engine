package io.github.xxvw.mcnativechunkengine.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Startup-only native configuration. No native bridge is loaded in Phase 1. */
public record McneConfiguration(boolean nativeRequested, String diagnostic) {
    public static McneConfiguration load(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "native:\n  enabled: false\n  requireSelfTest: true\n  allowUnsupportedABI: false\n  forcedImplementation: auto\n");
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(path.toFile());
            return parse(yaml);
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException exception) {
            return new McneConfiguration(false, "Invalid configuration; JAVA_BASELINE retained (" + exception.getClass().getSimpleName() + ")");
        }
    }

    public static McneConfiguration parse(YamlConfiguration yaml) {
        boolean requested = booleanValue(yaml, "native.enabled", false);
        if (!booleanValue(yaml, "native.requireSelfTest", true)
            || booleanValue(yaml, "native.allowUnsupportedABI", false)) {
            throw new IllegalArgumentException("Unsafe native qualification settings");
        }
        String implementation = yaml.getString("native.forcedImplementation", "auto");
        if (!"auto".equals(implementation) && !"scalar".equals(implementation)) {
            throw new IllegalArgumentException("Unknown native implementation");
        }
        return new McneConfiguration(requested, requested
            ? "Native unavailable: Phase 4 bridge is not implemented; JAVA_BASELINE retained"
            : "native.enabled=false; JAVA_BASELINE retained");
    }

    private static boolean booleanValue(YamlConfiguration yaml, String key, boolean defaultValue) {
        if (yaml.contains(key) && !yaml.isBoolean(key)) {
            throw new IllegalArgumentException("Expected boolean: " + key);
        }
        return yaml.getBoolean(key, defaultValue);
    }
}
