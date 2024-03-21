package com.molean.staticmap.nms;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;

import java.util.List;

public interface IVersion extends Listener {
    byte[] getColors(MapRenderer renderer);

    List<MapCursor> getCursors(Player player, MapRenderer renderer);
}
