package com.molean.staticmap.nms;

import org.bukkit.map.MapRenderer;
import java.lang.reflect.Field;

/**
 * 1.14-1.16.5
 */
public class VersionLegacy implements IVersion {
    @Override
    public byte[] getColors(MapRenderer renderer) {

        try {
            Field worldMapField = renderer.getClass().getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            Field colors = worldMapField.getType().getDeclaredField("colors");
            Object worldMapInst = worldMapField.get(renderer);
            return (byte[]) colors.get(worldMapInst);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
