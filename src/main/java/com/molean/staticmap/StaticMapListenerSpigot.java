package com.molean.staticmap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;

import java.util.List;

import static com.molean.staticmap.MapUtils.fromBytes;
import static com.molean.staticmap.StaticMapListener.getItemMeta;

public class StaticMapListenerSpigot implements Listener {
    private final StaticMap plugin;
    private final Material mapMaterial;
    public StaticMapListenerSpigot(StaticMap plugin, boolean legacy) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin = plugin);
        mapMaterial = legacy ? Material.MAP : Material.FILLED_MAP;
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
        ItemStack item = itemFrame.getItem();
        if (!item.getType().equals(mapMaterial)) {
            return;
        }
        DataSimplified data = DataSimplified.of(item);
        if (data.has("colors")) {
            ItemMeta itemMeta = getItemMeta(item);
            byte[] colors = data.getAsBytes("colors");
            List<MapCursor> cursors = fromBytes(data.getAsBytes("cursors"));
            if (colors != null) {
                MapUtils.updateStaticMap((MapMeta) itemMeta, colors, cursors);
                item.setItemMeta(itemMeta);
                Bukkit.getScheduler().runTask(plugin, () -> itemFrame.setItem(item));
            }
        }
    }
}
