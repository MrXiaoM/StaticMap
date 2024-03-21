package com.molean.staticmap.nms;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.8-1.16.5
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

    @Override
    public List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        // TODO
        return new ArrayList<>();
    }
}
