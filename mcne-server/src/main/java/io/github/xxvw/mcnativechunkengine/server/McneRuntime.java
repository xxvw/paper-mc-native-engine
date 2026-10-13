package io.github.xxvw.mcnativechunkengine.server;

import io.github.xxvw.mcnativechunkengine.brand.UpstreamProvenance;
import io.github.xxvw.mcnativechunkengine.config.McneConfiguration;
import io.github.xxvw.mcnativechunkengine.nativebridge.EngineMode;
import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.permissions.DefaultPermissions;

/** Immutable startup diagnostics; Phase 1 makes no change to generation tasks. */
public final class McneRuntime {
    private static volatile Status status;

    private McneRuntime() {
    }

    public static synchronized void initialize() {
        if (status != null) {
            return;
        }
        for (String permission : new String[]{"mcne.admin", "mcne.benchmark", "mcne.metrics"}) {
            DefaultPermissions.registerPermission(permission, "MCNE management capability", PermissionDefault.OP);
        }
        McneConfiguration configuration = McneConfiguration.load(Path.of("config", "mcne.yml"));
        status = new Status(UpstreamProvenance.load(), configuration, EngineMode.JAVA_BASELINE);
        Bukkit.getLogger().info("[MCNE] " + UpstreamProvenance.BRAND + "; " + status.provenance().display()
            + "; " + configuration.diagnostic());
    }

    public static Status status() {
        Status current = status;
        if (current == null) {
            throw new IllegalStateException("MCNE startup initialization has not completed");
        }
        return current;
    }

    public record Status(UpstreamProvenance provenance, McneConfiguration configuration, EngineMode mode) {
    }
}
