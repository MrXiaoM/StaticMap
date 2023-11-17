package com.molean.staticmap.nms;

import org.bukkit.map.MapRenderer;

public interface IVersion {
    byte[] getColors(MapRenderer renderer);
}
