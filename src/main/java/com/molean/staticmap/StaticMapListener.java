package com.molean.staticmap;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

public class StaticMapListener implements Listener {
    private final StaticMap plugin;
    private final Material mapMaterial;
    public StaticMapListener(StaticMap plugin, boolean legacy) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin = plugin);
        mapMaterial = legacy ? Material.MAP : Material.FILLED_MAP;
    }

    @EventHandler
    public void on(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ITEM_FRAME)) {
            return;
        }
        ItemFrame itemFrame = (ItemFrame) entity;
        ItemStack item = itemFrame.getItem();
        if (!item.getType().equals(mapMaterial)) {
            return;
        }
        DataSimplified data = DataSimplified.of(item);
        if (data.has("colors")) {
            ItemMeta itemMeta = getItemMeta(item);
            byte[] bytes = data.getAsBytes("colors");
            if (bytes != null) {
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
            if (!itemStack.getType().equals(mapMaterial)) {
                return;
            }
            MapMeta itemMeta = getItemMeta(itemStack);
            byte[] colors = DataSimplified.of(itemStack).getAsBytes("colors");
            MapUtils.updateStaticMap(colors, itemMeta);
            itemStack.setItemMeta(itemMeta);
        }, 1L);
    }

    @EventHandler
    public void onPlayer(PlayerItemHeldEvent event) {
        int newSlot = event.getNewSlot();
        ItemStack itemStack = event.getPlayer().getInventory().getItem(newSlot);
        if (itemStack == null || !itemStack.getType().equals(mapMaterial)) {
            return;
        }
        byte[] colors = DataSimplified.of(itemStack).getAsBytes("colors");
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
        AnvilInventory inv = event.getInventory();

        ItemStack firstItem = inv.getItem(0);
        ItemStack secondItem = inv.getItem(1);
        if (secondItem != null) {
            return;
        }
        if (firstItem == null || !firstItem.getType().equals(mapMaterial)) {
            return;
        }

        if (DataSimplified.of(firstItem).has("colors")) {
            return;
        }
        ItemStack itemStack = new ItemStack(mapMaterial);
        ItemMeta itemMeta = getItemMeta(itemStack);

        MapMeta mapMeta = getItemMeta(firstItem);
        byte[] colors = MapUtils.getColors(mapMeta);

        String renameText = event.getInventory().getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            itemMeta.setDisplayName(renameText);
        }
        itemMeta.setLore(Lists.newArrayList(
                plugin.getConfig().getString("lore", "")
                        .replaceAll("&([0-9A-Fa-fLMNKORlmnkor])", "§$1")
        ));
        itemStack.setItemMeta(itemMeta);
        DataSimplified data = DataSimplified.of(itemStack);
        data.setBytes("colors", colors);
        if (!DataSimplified.isPDHAvailable()) {
            itemStack = data.nbtToItemStack();
        }
        else {
            itemStack.setItemMeta(itemMeta);
        }
        itemStack.setAmount(firstItem.getAmount());
        inv.setRepairCost(plugin.getConfig().getInt("cost"));
        event.setResult(itemStack);
    }

    @SuppressWarnings({"unchecked"})
    public static <T extends ItemMeta> T getItemMeta(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return (T) (meta == null ? Bukkit.getItemFactory().getItemMeta(item.getType()) : meta);
    }
}
