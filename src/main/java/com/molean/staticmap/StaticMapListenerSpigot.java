package com.molean.staticmap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class StaticMapListenerSpigot implements Listener {
    private final StaticMap plugin;
    private final StaticMapListener parent;
    public StaticMapListenerSpigot(StaticMap plugin, StaticMapListener parent) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin = plugin);
        this.parent = parent;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            onEntityAddToWorld(entity);
        }
    }

    public void onEntityAddToWorld(Entity entity) {
        if (!(entity instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) entity;
        parent.checkMapUpdate(itemFrame.getItem(),
                item -> Bukkit.getScheduler().runTask(plugin, () -> itemFrame.setItem(item)));
    }
}
