package com.molean.staticmap;

import com.molean.staticmap.nms.IVersion;
import com.molean.staticmap.nms.VersionManager;
import com.molean.staticmap.outdated.OutdateConverter;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        foliaLib = new FoliaLib(this);
    }

    final OutdateConverter outdateConverter = new OutdateConverter(this);
    private StaticMapListener listener;
    private String serverName;
    private String mapName = "";
    private final List<String> mapLore = new ArrayList<>();
    private Integer mapCost = 20;
    private int anvilDelayTicks;
    private FoliaLib foliaLib;

    public PlatformScheduler getScheduler() {
        return foliaLib.getScheduler();
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    @Override
    public void onEnable() {
        PAPI.init();
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

    public String getServerName() {
        return serverName;
    }

    public String getMapName() {
        return mapName;
    }

    public List<String> getMapLore() {
        return mapLore;
    }

    public Integer getMapCost() {
        return mapCost;
    }

    public int getAnvilDelayTicks() {
        return anvilDelayTicks;
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

    private int getAnvilDelayTicksAuto() {
        if (foliaLib.isFolia()) {
            return 0;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("EcoEnchants")) {
            return 1;
        }
        return 0;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration config = getConfig();
        config.setDefaults(new MemoryConfiguration());
        String serverName = config.getString("server-name", "server port");
        if ("server port".equalsIgnoreCase(serverName)) {
            this.serverName = String.valueOf(Bukkit.getServer().getPort());
        } else {
            this.serverName = serverName;
        }

        String anvilDelayStr = config.getString("anvil-delay-ticks", "auto");
        if ("auto".equals(anvilDelayStr) || anvilDelayStr == null) {
            anvilDelayTicks = getAnvilDelayTicksAuto();
        } else try {
            anvilDelayTicks = Integer.parseInt(anvilDelayStr);
        } catch (Throwable t) {
            anvilDelayTicks = getAnvilDelayTicksAuto();
        }

        mapName = config.getString("name", "&7跨服地图画");
        mapName = mapName == null ? "" : ChatColor.translateAlternateColorCodes('&', mapName);
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
