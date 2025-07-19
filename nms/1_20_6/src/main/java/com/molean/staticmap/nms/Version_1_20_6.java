package com.molean.staticmap.nms;

import net.minecraft.resources.MinecraftKey;
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
 * 1.20.5-1.20.6
 */
public class Version_1_20_6 implements IVersion {

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
                MapCursor.Type iconType = getType(decoration.c().a().b());
                if (iconType == null) continue;
                // MapIcon 内已有 (byte) (var3 & 15)，无需额外处理
                byte direction = decoration.f();
                cursors.add(new MapCursor(
                        decoration.d(), // x
                        decoration.e(), // y
                        direction, // direction
                        iconType, // type
                        true, // visible
                        fromComponent(decoration.g().orElse(null)) // caption
                ));
            }
            return cursors;
        } catch (ReflectiveOperationException e) {
            IVersion.doCursorCatch(type, e);
            return new ArrayList<>();
        }
    }

    private MapCursor.Type getType(MinecraftKey key) {
        if (key == null) return null;
        for (MapCursor.Type type : MapCursor.Type.values()) {
            if (type.getKey().getKey().equals(key.a())) {
                return type;
            }
        }
        return null;
    }
}
