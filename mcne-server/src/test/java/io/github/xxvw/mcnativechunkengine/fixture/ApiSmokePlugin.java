package io.github.xxvw.mcnativechunkengine.fixture;

import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

/** Disposable API fixture; never included in the server runtime artifact. */
public final class ApiSmokePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        if (!Bukkit.getName().equals("paper-chunk-native-engine")
            || !Bukkit.getVersion().contains("26.3")
            || !Bukkit.getBukkitVersion().contains("26.3")
            || Bukkit.getCommandMap().getCommand("mcne") == null
            || Bukkit.getPluginManager().getPermission("mcne.admin").getDefault() != PermissionDefault.OP) {
            throw new IllegalStateException("MCNE API smoke failed");
        }
        getLogger().info("MCNE_API_SMOKE_PASS");
    }
}
