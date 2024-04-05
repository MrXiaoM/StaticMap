package com.molean.staticmap.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.plugin.Plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1.8-1.16.5
 */
@SuppressWarnings({"deprecation"})
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
    @SuppressWarnings({"unchecked"})
    public List<MapCursor> getCursors(Player player, MapRenderer renderer) {
        try {
            Field worldMapField = renderer.getClass().getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            Field deco = worldMapField.getType().getDeclaredField("decorations");
            Object worldMap = worldMapField.get(renderer);
            Map<String, Object> mapIconMap = (Map<String, Object>) deco.get(worldMap);

            Field fieldType = null;
            Method methodTypeByte = null;
            Field fieldX = null;
            Field fieldY = null;
            Field fieldRotation = null;
            Method methodName = null;
            List<MapCursor> cursors = new ArrayList<>();
            for (String key : mapIconMap.keySet()) {
                Player other = Bukkit.getPlayerExact(key);
                if ((other != null && !player.canSee(other))) continue;
                Object decoration = mapIconMap.get(key);
                Class<?> clazz = decoration.getClass();
                if (fieldType == null) {
                    fieldType = clazz.getDeclaredField("type");
                    fieldType.setAccessible(true);
                    methodTypeByte = fieldType.getType().getDeclaredMethod("a");
                    methodTypeByte.setAccessible(true);
                }
                if (fieldX == null) {
                    fieldX = clazz.getDeclaredField("x");
                    fieldX.setAccessible(true);
                }
                if (fieldY == null) {
                    fieldY = clazz.getDeclaredField("y");
                    fieldY.setAccessible(true);
                }
                if (fieldRotation == null) {
                    fieldRotation = clazz.getDeclaredField("rotation");
                    fieldRotation.setAccessible(true);
                }
                if (methodName == null) {
                    methodName = clazz.getDeclaredMethod("g");

                }
                byte x = fieldX.getByte(decoration);
                byte y = fieldY.getByte(decoration);
                byte rotation = fieldRotation.getByte(decoration);
                Object type = fieldType.get(decoration);
                byte typeByte = (byte) methodTypeByte.invoke(type);
                Object name = methodName.invoke(decoration);
                name = fromComponent(name);
                cursors.add(new MapCursor(
                        x, y,
                        (byte) (rotation & 15),
                        typeByte,
                        true,
                        name == null ? null : name.toString()
                ));
            }
            return cursors;
        } catch (ReflectiveOperationException | NullPointerException e) {
            IVersion.warn(e);
        }
        return new ArrayList<>();
    }
}
