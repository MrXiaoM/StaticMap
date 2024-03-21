package com.molean.staticmap.nms;

import org.bukkit.event.Listener;
import org.bukkit.map.MapRenderer;

public interface IVersion extends Listener {
    byte[] getColors(MapRenderer renderer);
}
