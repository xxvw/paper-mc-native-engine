package io.github.xxvw.mcnativechunkengine.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class McneConfigurationTest {
    @TempDir
    Path directory;

    @Test
    void defaultsToBaselineAndPersistsSafeConfiguration() {
        Path config = this.directory.resolve("config/mcne.yml");
        McneConfiguration result = McneConfiguration.load(config);
        assertFalse(result.nativeRequested());
        assertTrue(Files.isRegularFile(config));
        assertTrue(result.diagnostic().contains("native.enabled=false"));
    }

    @Test
    void nativeRequestDisclosesUnavailableBridge() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("native.enabled", true);
        McneConfiguration result = McneConfiguration.parse(yaml);
        assertTrue(result.nativeRequested());
        assertTrue(result.diagnostic().contains("JAVA_BASELINE retained"));
        assertTrue(result.diagnostic().contains("not implemented"));
    }

    @Test
    void rejectsUnsafeAbiSelfTestAndImplementationOptions() {
        for (String key : new String[]{"native.allowUnsupportedABI", "native.requireSelfTest", "native.forcedImplementation", "native.enabled"}) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(key, switch (key) {
                case "native.allowUnsupportedABI" -> true;
                case "native.requireSelfTest" -> false;
                case "native.forcedImplementation" -> "avx2";
                default -> "yes";
            });
            assertThrows(IllegalArgumentException.class, () -> McneConfiguration.parse(yaml));
        }
    }

    @Test
    void invalidFileFailsClosedWithDiagnostic() throws Exception {
        Path config = this.directory.resolve("mcne.yml");
        Files.writeString(config, "native: [unclosed\n");
        McneConfiguration result = McneConfiguration.load(config);
        assertFalse(result.nativeRequested());
        assertTrue(result.diagnostic().contains("Invalid configuration"));
    }
}
