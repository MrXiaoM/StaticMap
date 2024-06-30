package com.molean.staticmap.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.plugin.Plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public interface IVersion extends Listener {
    byte[] getColors(MapRenderer renderer);

    List<MapCursor> getCursors(Player player, MapRenderer renderer);

    @SuppressWarnings({"unchecked"})
    default <T> T fromComponent(Object component) throws ReflectiveOperationException {
        if (component == null) return null;
        Class<?> clazz = Class.forName("org.bukkit.craftbukkit." + getNMSVersion() + ".util.CraftChatMessage");
        Method fromComponent = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals("fromComponent") && method.getParameterCount() == 1) {
                fromComponent = method;
            }
        }
        return fromComponent == null ? null : (T)fromComponent.invoke(null, component);
    }

    static String getNMSVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }

    static void warn(Throwable t) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("StaticMap");
        if (plugin != null) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                t.printStackTrace(pw);
            }
            plugin.getLogger().warning(sw.toString());
        }
    }
}
