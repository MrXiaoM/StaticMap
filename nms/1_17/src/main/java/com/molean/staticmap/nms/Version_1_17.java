package com.molean.staticmap.nms;

import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.map.MapRenderer;

import java.lang.reflect.Field;

/**
 * 1.17-1.20.2
 */
public class Version_1_17 implements IVersion {
    @Override
    public byte[] getColors(MapRenderer renderer) {
        try {
            Field worldMapField = renderer.getClass().getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            WorldMap worldMap = (WorldMap) worldMapField.get(renderer);
            return worldMap.g;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
