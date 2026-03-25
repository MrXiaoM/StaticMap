package com.molean.staticmap.nms;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 26.1+
 */
public class Version_26_1 implements IVersion {

    @Override
    public byte[] getColors(MapRenderer renderer) {
        Class<?> type = renderer.getClass();
        try {
            Field worldMapField = type.getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            MapItemSavedData worldMap = (MapItemSavedData) worldMapField.get(renderer);
            return worldMap.colors;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw IVersion.doColorCatch(type, e);
        }
    }

    @Override
    public List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        Class<?> type = renderer.getClass();
        try {
            Field worldMapField = type.getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            MapItemSavedData worldMap = (MapItemSavedData) worldMapField.get(renderer);
            Map<String, MapDecoration> mapIconMap = worldMap.decorations;
            List<MapCursor> cursors = new ArrayList<>();
            for (String key : mapIconMap.keySet()) {
                Player other = Bukkit.getPlayerExact(key);
                if ((other != null && !player.canSee(other))) continue;
                MapDecoration decoration = mapIconMap.get(key);
                MapCursor.Type iconType = getType(decoration.type().value().assetId());
                if (iconType == null) continue;
                // MapDecoration 内已有 (byte) (var3 & 15)，无需额外处理
                byte direction = decoration.rot();
                cursors.add(new MapCursor(
                        decoration.x(), // x
                        decoration.y(), // y
                        direction, // direction
                        iconType, // type
                        true, // visible
                        CraftChatMessage.fromComponent(decoration.name().orElse(null)) // caption
                ));
            }
            return cursors;
        } catch (ReflectiveOperationException e) {
            IVersion.doCursorCatch(type, e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings({"deprecation"})
    private MapCursor.Type getType(Identifier key) {
        if (key == null) return null;
        for (MapCursor.Type type : Registry.MAP_DECORATION_TYPE) {
            try {
                NamespacedKey typeKey = type.getKey();
                if (typeKey.getKey().equals(key.getPath())) {
                    return type;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
