package com.molean.staticmap;

import com.molean.staticmap.nms.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaticMap extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("当前服务器版本: " + VersionManager.getVersion());
        VersionManager.Status init = VersionManager.init();
        if (!init.equals(VersionManager.Status.INVALID)) {
            getLogger().info("插件支持当前服务器版本");
        } else {
            getLogger().warning("插件不支持当前服务器版本");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (init.equals(VersionManager.Status.OK)) {
            new StaticMapListener(this);
        }
        this.saveDefaultConfig();
        this.reloadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
