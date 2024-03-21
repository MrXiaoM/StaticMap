package com.molean.staticmap;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;

import java.util.List;

import static com.molean.staticmap.MapUtils.fromBytes;
import static com.molean.staticmap.MapUtils.toBytes;

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
            byte[] colors = data.getAsBytes("colors");
            List<MapCursor> cursors = fromBytes(data.getAsBytes("cursors"));
            if (colors != null) {
                MapUtils.updateStaticMap((MapMeta) itemMeta, colors, cursors);
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
            DataSimplified data = DataSimplified.of(itemStack);
            byte[] colors = data.getAsBytes("colors");
            List<MapCursor> cursors = fromBytes(data.getAsBytes("cursors"));
            MapUtils.updateStaticMap(itemMeta, colors, cursors);
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
        DataSimplified data = DataSimplified.of(itemStack);
        byte[] colors = data.getAsBytes("colors");
        List<MapCursor> cursors = fromBytes(data.getAsBytes("cursors"));
        ItemMeta itemMeta = itemStack.getItemMeta();
        MapUtils.updateStaticMap((MapMeta) itemMeta, colors, cursors);
        itemStack.setItemMeta(itemMeta);
    }

    @EventHandler
    public void on(PrepareAnvilEvent event) {
        Player player = null;
        for (HumanEntity viewer : event.getViewers()) {
            if (!viewer.hasPermission("staticmap.use")) {
                return;
            }
            if (player == null && viewer instanceof Player) {
                player = (Player) viewer;
            }
        }
        if (player == null) {
            return;
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
        List<MapCursor> cursors = MapUtils.getCursors(player, mapMeta);

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
        if (cursors != null && !cursors.isEmpty()) {
            byte[] bytes = toBytes(cursors);
            if (bytes != null) {
                data.setBytes("cursors", bytes);
            }
        }
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

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (item == null) continue;
            byte[] colors = DataSimplified.of(item).getAsBytes("colors");
            if (colors == null) continue;
            if (!e.getInventory().getViewers().stream().allMatch(it -> it.hasPermission("staticmap.copy"))) {
                e.setCancelled(true);
                for (HumanEntity viewer : e.getInventory().getViewers()) {
                    viewer.sendMessage("§7不能复制跨服地图画");
                }
                break;
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof CartographyInventory) {
            handleInv((CartographyInventory) e.getClickedInventory(), e.getCurrentItem(), e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory() instanceof CartographyInventory) {
            if (handleInv((CartographyInventory) e.getInventory(), e.getOldCursor(), e)) return;
            if (handleInv((CartographyInventory) e.getInventory(), e.getCursor(), e)) return;
            for (ItemStack item : e.getNewItems().values()) {
                if (handleInv((CartographyInventory) e.getInventory(), item, e)) return;
            }
        }
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent e) {
        if (e.getDestination() instanceof CartographyInventory) {
            handleInv((CartographyInventory) e.getDestination(), e.getItem(), e);
        }
    }

    public boolean handleInv(CartographyInventory inv, ItemStack item, Cancellable e) {
        if (item == null) return false;
        byte[] colors = DataSimplified.of(item).getAsBytes("colors");
        if (colors == null) return false;
        e.setCancelled(true);
        if (!inv.getViewers().stream().allMatch(it -> it.hasPermission("staticmap.copy"))) {
            for (HumanEntity viewer : inv.getViewers()) {
                viewer.sendMessage("§7不能复制跨服地图画");
            }
            return true;
        }
        return false;
    }


    @SuppressWarnings({"unchecked"})
    public static <T extends ItemMeta> T getItemMeta(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return (T) (meta == null ? Bukkit.getItemFactory().getItemMeta(item.getType()) : meta);
    }
}
