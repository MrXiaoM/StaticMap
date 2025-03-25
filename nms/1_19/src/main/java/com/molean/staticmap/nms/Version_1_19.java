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
 * 1.19
 */
@SuppressWarnings({"deprecation"})
public class Version_1_19 implements IVersion {

    @Override
    public byte[] getColors(MapRenderer renderer) {
        Class<?> type = renderer.getClass();
        try {
            Field worldMapField = renderer.getClass().getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            WorldMap worldMap = (WorldMap) worldMapField.get(renderer);
            return worldMap.g;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(type.getName(), e);
        }
    }

    @Override
    public List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        Class<?> type = renderer.getClass();
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
                byte iconType = decoration.b().a();
                byte direction = (byte) (decoration.e() & 15);
                cursors.add(new MapCursor(
                        decoration.c(), // x
                        decoration.d(), // y
                        direction, // direction
                        iconType, // type
                        true, // visible
                        fromComponent(decoration.g()) // caption
                ));
            }
            return cursors;
        } catch (ReflectiveOperationException e) {
            IVersion.warn(new RuntimeException(type.getName(), e));
            return new ArrayList<>();
        }
    }
}
