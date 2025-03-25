package com.molean.staticmap;

import com.molean.staticmap.nms.IVersion;
import com.molean.staticmap.nms.VersionManager;
import com.molean.staticmap.outdated.OutdateConverter;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.map.MapCursor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StaticMap extends JavaPlugin {

    @Override
    public void onLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    final OutdateConverter outdateConverter = new OutdateConverter(this);
    private StaticMapListener listener;
    private final List<String> mapLore = new ArrayList<>();
    private Integer mapCost = 20;
    @Override
    public void onEnable() {
        getLogger().info("当前服务器版本: " + IVersion.getDisplayVersion());
        VersionManager.Status init = VersionManager.init();
        if (!init.equals(VersionManager.Status.INVALID)) {
            getLogger().info("插件支持当前服务器版本");
        } else {
            getLogger().warning("插件不支持当前服务器版本");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        VersionManager.registerListener(this);
        listener = new StaticMapListener(this, init.equals(VersionManager.Status.LEGACY_OLD));
        this.saveDefaultConfig();
        this.reloadConfig();
    }

    public List<String> getMapLore() {
        return mapLore;
    }

    public Integer getMapCost() {
        return mapCost;
    }

    public StaticMapListener getListener() {
        return listener;
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
        config.setDefaults(new MemoryConfiguration());

        mapLore.clear();
        if (config.isList("lore")) {
            for (String loreLine : config.getStringList("lore")) {
                mapLore.add(loreLine.replaceAll("&([0-9A-Fa-fLMNKXORlmnkxor])", "§$1"));
            }
        } else {
            String loreLine = config.getString("lore");
            if (loreLine != null) {
                mapLore.add(loreLine.replaceAll("&([0-9A-Fa-fLMNKXORlmnkxor])", "§$1"));
            }
        }
        mapCost = config.contains("cost") ? config.getInt("cost") : null;
        MapUtils.hiddenCursors.clear();
        for (String s : config.getStringList("hidden-cursors")) {
            if (s.equals("*")) {
                MapUtils.hiddenCursors.addAll(Arrays.asList(MapCursor.Type.values()));
                break;
            }
            MapCursor.Type type = MapCursor.Type.valueOf(s.toUpperCase());
            MapUtils.hiddenCursors.add(type);
        }
        if (config.getBoolean("disable-outdate-converter", false)) {
            outdateConverter.unregister();
        } else {
            outdateConverter.register();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
