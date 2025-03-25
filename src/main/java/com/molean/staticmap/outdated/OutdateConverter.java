package com.molean.staticmap.outdated;

import com.molean.staticmap.StaticMap;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.function.Consumer;

import static com.molean.staticmap.StaticMapListener.*;

public class OutdateConverter implements Listener {
    private final StaticMap plugin;
    private boolean registered = false;
    public OutdateConverter(StaticMap plugin) {
        this.plugin = plugin;
    }

    public void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    public void register() {
        unregister();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    public void convert(ItemStack item, Consumer<ItemStack> setItem) {
        if (plugin.getListener().isNotMap(item)) return;
        DataSimplified data = DataSimplified.of(item);
        byte[] colors = data.has("colors") ? data.getAsBytes("colors") : null;
        byte[] cursors = data.has("cursors") ? data.getAsBytes("cursors") : null;
        if (colors != null || cursors != null) {
            data.remove("colors");
            data.remove("cursors");
            if (data.pdh != null) {
                item.setItemMeta(data.pdh.getHolder());
            } else {
                item = data.nbtToItemStack();
            }
            NBT.modify(item, nbt -> {
                if (colors != null) {
                    nbt.setByteArray(COLORS, colors);
                }
                if (cursors != null) {
                    nbt.setByteArray(CURSORS, cursors);
                }
            });
            if (setItem != null) {
                setItem.accept(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            final int index = i;
            convert(inv.getItem(index), item -> inv.setItem(index, item));
        }
    }

    public void onEntityAddToWorld(ItemFrame itemFrame) {
        if (!registered) return;
        convert(itemFrame.getItem(), itemFrame::setItem);
    }
}
