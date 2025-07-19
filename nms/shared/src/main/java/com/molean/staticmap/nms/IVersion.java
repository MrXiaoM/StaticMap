package com.molean.staticmap.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.plugin.Plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface IVersion {

    byte[] getColors(MapRenderer renderer);

    List<MapCursor> getCursors(Player player, MapRenderer renderer);

    default Listener extraListener() {
        return null;
    }

    @SuppressWarnings({"unchecked"})
    default <T> T fromComponent(Object component) throws ReflectiveOperationException {
        if (component == null) return null;
        String nms = getNMSVersion();

        Class<?> clazz = nms.contains("_R")
                ? Class.forName("org.bukkit.craftbukkit." + nms + ".util.CraftChatMessage")
                : Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
        Method fromComponent = Internal.fromComponent;
        if (fromComponent == null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals("fromComponent")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(component)) {
                    Internal.fromComponent = fromComponent = method;
                }
            }
        }
        return fromComponent == null ? null : (T)fromComponent.invoke(null, component);
    }

    @SuppressWarnings({"unchecked"})
    default <T> T toComponent(String component) throws ReflectiveOperationException {
        if (component == null) return null;
        String nms = getNMSVersion();

        Class<?> clazz = nms.contains("_R")
                ? Class.forName("org.bukkit.craftbukkit." + nms + ".util.CraftChatMessage")
                : Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
        Method toComponent = Internal.fromString;
        if (toComponent == null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals("fromString")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(String.class)) {
                    Internal.fromString = toComponent = method;
                }
            }
        }
        return toComponent == null ? null : (T)toComponent.invoke(null, component);
    }

    static String getDisplayVersion() {
        String nms = getNMSVersion();
        return nms.isEmpty() ? Bukkit.getVersion() : nms;
    }

    static String getNMSVersion() {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        if (name.length() < 28) {
            Matcher m = Pattern.compile("MC: (1\\.2[0-9](\\.[0-9])?)").matcher(Bukkit.getVersion());
            return m.find() && m.groupCount() > 0 ? m.group(1) : "";
        }
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }

    static void warn(Throwable t) {
        Plugin plugin = Internal.plugin;
        if (plugin == null) {
            Internal.plugin = plugin = Bukkit.getPluginManager().getPlugin("StaticMap");
        }
        if (plugin != null) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                t.printStackTrace(pw);
            }
            plugin.getLogger().warning(sw.toString());
        }
    }

    static RuntimeException doColorCatch(Class<?> type, Throwable e) {
        return new RuntimeException("从 " + type.getName() + " 获取地图颜色数据时出错", e);
    }

    static void doCursorCatch(Class<?> type, Throwable e) {
        IVersion.warn(new RuntimeException("从 " + type.getName() + " 获取图标数据时出错", e));
    }

    class Internal {
        private static Method fromComponent;
        private static Method fromString;
        private static Plugin plugin;
    }
}
