package com.molean.staticmap.nms;

import net.minecraft.world.level.saveddata.maps.MapIcon;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1.17
 */
@SuppressWarnings({"deprecation"})
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

    @Override
    public List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        try {
            Field worldMapField = renderer.getClass().getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            WorldMap worldMap = (WorldMap) worldMapField.get(renderer);
            Map<String, MapIcon> mapIconMap = worldMap.q;
            List<MapCursor> cursors = new ArrayList<>();
            for (String key : mapIconMap.keySet()) {
                Player other = Bukkit.getPlayerExact(key);
                if ((other != null && !player.canSee(other))) continue;
                MapIcon decoration = mapIconMap.get(key);
                byte type = decoration.getType().a();
                byte direction = (byte) (decoration.getRotation() & 15);
                cursors.add(new MapCursor(
                        decoration.getX(), // x
                        decoration.getY(), // y
                        direction, // direction
                        type, // type
                        true, // visible
                        fromComponent(decoration.getName()) // caption
                ));
            }
            return cursors;
        } catch (ReflectiveOperationException | NullPointerException e) {
            IVersion.warn(e);
            return new ArrayList<>();
        }
    }
}
