package com.molean.staticmap.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class VersionManager {
    private static IVersion nms = null;
    public enum Status {
        OK, LEGACY, LEGACY_OLD, INVALID
    }
    public static Status init() {
        String ver = IVersion.getNMSVersion();
        if (matchVersions(ver, "v1_21", "1.21")) {
            nms = new Version_1_21();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_20_R4", "1.20.6", "1.20.5")) {
            nms = new Version_1_20_6();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_20_R2", "v1_20_R3")) {
            nms = new Version_1_20_2();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_20_R1")) {
            nms = new Version_1_20_1();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_19")) {
            nms = new Version_1_19();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_18")) {
            nms = new Version_1_18();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_17")) {
            nms = new Version_1_17();
            return Status.OK;
        }
        if (matchVersions(ver, "v1_13", "v1_14", "v1_15", "v1_16")) {
            nms = new VersionLegacy();
            return Status.LEGACY;
        }
        if (matchVersions(ver, "1_8", "v1_9", "v1_10", "v1_11", "v1_12")) {
            nms = new VersionLegacy();
            return Status.LEGACY_OLD;
        }
        return Status.INVALID;
    }

    public static void registerListener(JavaPlugin plugin) {
        if (nms != null) {
            Bukkit.getPluginManager().registerEvents(nms, plugin);
        }
    }

    private static boolean matchVersions(String ver, String... versions) {
        for (String v : versions) {
            if (ver.startsWith(v)) return true;
        }
        return false;
    }

    public static byte[] getColors(MapRenderer renderer) {
        return nms.getColors(renderer);
    }

    public static List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        return nms.getCursors(player, renderer);
    }
}
