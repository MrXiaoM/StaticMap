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
 * 1.20.2-1.20.4
 */
@SuppressWarnings({"deprecation"})
public class Version_1_20_2 implements IVersion {

    @Override
    public byte[] getColors(MapRenderer renderer) {
        Class<?> type = renderer.getClass();
        try {
            Field worldMapField = type.getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            WorldMap worldMap = (WorldMap) worldMapField.get(renderer);
            return worldMap.g;
        } catch (ReflectiveOperationException e) {
            throw IVersion.doColorCatch(type, e);
        }
    }

    @Override
    public List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        Class<?> type = renderer.getClass();
        try {
            Field worldMapField = type.getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            WorldMap worldMap = (WorldMap) worldMapField.get(renderer);
            Map<String, MapIcon> mapIconMap = worldMap.q;
            List<MapCursor> cursors = new ArrayList<>();
            for (String key : mapIconMap.keySet()) {
                Player other = Bukkit.getPlayerExact(key);
                if ((other != null && !player.canSee(other))) continue;
                MapIcon decoration = mapIconMap.get(key);
                cursors.add(new MapCursor(
                        decoration.d(),
                        decoration.e(),
                        (byte) (decoration.f() & 15),
                        decoration.c().a(),
                        true,
                        fromComponent(decoration.g())
                ));
            }
            return cursors;
        } catch (ReflectiveOperationException e) {
            IVersion.doCursorCatch(type, e);
            return new ArrayList<>();
        }
    }
}
