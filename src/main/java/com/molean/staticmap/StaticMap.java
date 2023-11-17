package com.molean.staticmap;

import com.molean.staticmap.nms.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.isOp()) {
            this.saveDefaultConfig();
            this.reloadConfig();
            sender.sendMessage("配置文件已重载");
        }
        return true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
