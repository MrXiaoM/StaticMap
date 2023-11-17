package com.molean.staticmap.nms;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;

public class VersionManager {
    private static IVersion nms = null;
    public static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }
    public enum Status {
        OK, LEGACY, LEGACY_OLD, INVALID
    }
    public static Status init() {
        String ver = getVersion();
        if (matchVersions(ver, "v1_17", "v1_18", "v1_19", "v1_20")) {
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

    private static boolean matchVersions(String ver, String... versions) {
        for (String v : versions) {
            if (ver.startsWith(v)) return true;
        }
        return false;
    }

    public static byte[] getColors(MapRenderer renderer) {
        return nms.getColors(renderer);
    }
}
