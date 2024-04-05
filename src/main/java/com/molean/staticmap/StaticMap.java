package com.molean.staticmap;

import com.molean.staticmap.nms.IVersion;
import com.molean.staticmap.nms.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.map.MapCursor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class StaticMap extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("当前服务器版本: " + IVersion.getNMSVersion());
        VersionManager.Status init = VersionManager.init();
        if (!init.equals(VersionManager.Status.INVALID)) {
            getLogger().info("插件支持当前服务器版本");
        } else {
            getLogger().warning("插件不支持当前服务器版本");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!DataSimplified.isPDHAvailable() && !Bukkit.getPluginManager().isPluginEnabled("NBTAPI")) {
            getLogger().warning("当前服务端不支持 Persistence 特性，请安装 item-nbt-api-plugin 插件");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        VersionManager.registerListener(this);
        new StaticMapListener(this, init.equals(VersionManager.Status.LEGACY_OLD));
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
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration config = getConfig();

        MapUtils.hiddenCursors.clear();
        for (String s : config.getStringList("hidden-cursors")) {
            if (s.equals("*")) {
                MapUtils.hiddenCursors.addAll(Arrays.asList(MapCursor.Type.values()));
                break;
            }
            MapCursor.Type type = MapCursor.Type.valueOf(s.toUpperCase());
            MapUtils.hiddenCursors.add(type);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
