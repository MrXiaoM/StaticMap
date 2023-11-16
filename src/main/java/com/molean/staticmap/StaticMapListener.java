package com.molean.staticmap;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public class StaticMapListener implements Listener {
    private final int cost;
    private final String lore;
    private final StaticMap plugin;
    public StaticMapListener(StaticMap plugin) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin = plugin);
        FileConfiguration config = plugin.getConfig();
        cost = config.getInt("cost");
        lore = config.getString("lore");
    }


    @EventHandler
    public void on(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ITEM_FRAME)) {
            return;
        }
        ItemFrame itemFrame = (ItemFrame) entity;
        ItemStack item = itemFrame.getItem();
        if (!item.getType().equals(Material.FILLED_MAP)) {
            return;
        }
        ;
        if (PDHSimplified.of(item.getItemMeta()).has("colors")) {
            byte[] bytes = PDHSimplified.of(item.getItemMeta()).getAsBytes("colors");
            if (bytes != null) {
                ItemMeta itemMeta = item.getItemMeta();
                MapUtils.updateStaticMap(bytes, (MapMeta) itemMeta);
                item.setItemMeta(itemMeta);
                itemFrame.setItem(item);
            }
        }

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!itemStack.getType().equals(Material.FILLED_MAP)) {
                return;
            }
            byte[] colors = PDHSimplified.of(itemStack.getItemMeta()).getAsBytes("colors");
            ItemMeta itemMeta = itemStack.getItemMeta();
            MapUtils.updateStaticMap(colors, (MapMeta) itemMeta);
            itemStack.setItemMeta(itemMeta);
        }, 1L);
    }

    @EventHandler
    public void onPlayer(PlayerItemHeldEvent event) {
        int newSlot = event.getNewSlot();
        ItemStack itemStack = event.getPlayer().getInventory().getItem(newSlot);
        if (itemStack == null || !itemStack.getType().equals(Material.FILLED_MAP)) {
            return;
        }
        byte[] colors = PDHSimplified.of(itemStack.getItemMeta()).getAsBytes("colors");
        ItemMeta itemMeta = itemStack.getItemMeta();
        MapUtils.updateStaticMap(colors, (MapMeta) itemMeta);
        itemStack.setItemMeta(itemMeta);
    }

    @EventHandler
    public void on(PrepareAnvilEvent event) {
        for (HumanEntity viewer : event.getViewers()) {
            if (!viewer.hasPermission("staticmap.use")) {
                return;
            }
        }
        ItemStack firstItem = event.getInventory().getFirstItem();
        ItemStack secondItem = event.getInventory().getSecondItem();
        if (secondItem != null) {
            return;
        }
        if (firstItem == null || !firstItem.getType().equals(Material.FILLED_MAP)) {
            return;
        }

        if (PDHSimplified.of(firstItem.getItemMeta()).has("colors")) {
            return;
        }
        ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) firstItem.getItemMeta();
        byte[] colors = MapUtils.getColors(mapMeta);
        ItemMeta itemMeta = itemStack.getItemMeta();
        String renameText = event.getInventory().getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            itemMeta.displayName(Component.text(Objects.requireNonNull(event.getInventory().getRenameText())));
        }
        itemMeta.lore(List.of(Component.text(lore)));
        PDHSimplified.of(itemMeta).setBytes("colors", colors);
        itemStack.setItemMeta(itemMeta);
        itemStack.setAmount(firstItem.getAmount());
        event.getInventory().setRepairCost(cost);
        event.setResult(itemStack);
    }


}
