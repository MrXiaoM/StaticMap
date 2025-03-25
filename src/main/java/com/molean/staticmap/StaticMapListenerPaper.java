package com.molean.staticmap;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class StaticMapListenerPaper implements Listener {
    private final StaticMap plugin;
    private final StaticMapListener parent;
    public StaticMapListenerPaper(StaticMap plugin, StaticMapListener parent) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin = plugin);
        this.parent = parent;
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) entity;
        plugin.outdateConverter.onEntityAddToWorld(itemFrame);
        parent.checkMapUpdate(itemFrame.getItem(),
                item -> Bukkit.getScheduler().runTask(plugin, () -> itemFrame.setItem(item)));
    }
}
