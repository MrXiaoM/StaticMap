package com.molean.staticmap;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static com.molean.staticmap.MapUtils.fromBytes;
import static com.molean.staticmap.MapUtils.toBytes;

public class StaticMapListener implements Listener {
    public static final String COLORS = "staticmap_colors";
    public static final String CURSORS = "staticmap_cursors";
    public static final String FLAG = "staticmap_flag";
    private final StaticMap plugin;
    private final Material mapMaterial;
    public StaticMapListener(StaticMap plugin, boolean legacy) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin = plugin);
        mapMaterial = legacy ? Material.MAP : Material.FILLED_MAP;
        if (StaticMap.isClassPresent("com.destroystokyo.paper.event.entity.EntityAddToWorldEvent")) {
            Bukkit.getPluginManager().registerEvents(new StaticMapListenerPaper(plugin, this), plugin);
        } else {
            plugin.getLogger().warning("当前非 Paper 服务端，正在使用备用方案，该方案未经测试，物品展示框上的跨服地图画可能会在区块重新加载后失效!");
            Bukkit.getPluginManager().registerEvents(new StaticMapListenerSpigot(plugin, this), plugin);
        }
    }

    @Contract("null -> true")
    public boolean isNotMap(ItemStack item) {
        return item == null || !item.getType().equals(mapMaterial);
    }

    public byte @Nullable [] bytes(ReadableNBT nbt, String key) {
        if (nbt.hasTag(key, NBTType.NBTTagByteArray)) {
            return nbt.getByteArray(key);
        } else {
            return null;
        }
    }

    public void checkMapUpdate(ItemStack item) {
        checkMapUpdate(item, null);
    }
    public void checkMapUpdate(ItemStack item, Consumer<ItemStack> setItem) {
        if (isNotMap(item)) return;
        NBT.get(item, nbt -> {
            MapMeta itemMeta = getItemMeta(item);
            byte[] colors = bytes(nbt, COLORS);
            List<MapCursor> cursors = fromBytes(bytes(nbt, CURSORS));
            MapUtils.updateStaticMap(itemMeta, colors, cursors);
            item.setItemMeta(itemMeta);
            if (setItem != null) {
                setItem.accept(item);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                checkMapUpdate(inv.getItem(i));
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        int newSlot = event.getNewSlot();
        PlayerInventory inv = event.getPlayer().getInventory();
        checkMapUpdate(inv.getItem(newSlot));
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();

        ItemStack firstItem = inv.getItem(0);
        ItemStack secondItem = inv.getItem(1);
        if (firstItem == null || secondItem != null || !firstItem.getType().equals(mapMaterial)) {
            return;
        }
        if (NBT.get(firstItem, nbt -> nbt.hasTag(COLORS) || nbt.hasTag(FLAG))) {
            return;
        }
        Player player = null;
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof Player) {
                if (!viewer.hasPermission("staticmap.use")) return;
                player = (Player) viewer;
                break;
            }
        }
        if (player == null) return;

        ItemStack itemStack = new ItemStack(mapMaterial);
        MapMeta mapMeta = getItemMeta(firstItem);
        MapMeta itemMeta = mapMeta.clone();

        String renameText = event.getInventory().getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            itemMeta.setDisplayName(renameText);
        }
        itemMeta.setLore(plugin.getMapLore());
        itemStack.setItemMeta(itemMeta);

        NBT.modify(itemStack, nbt -> { // 只添加 flag，地图数据之后再加，以免占用地图ID
            nbt.setBoolean(FLAG, true);
        });

        itemStack.setAmount(firstItem.getAmount());
        Integer cost = plugin.getMapCost();
        if (cost != null) {
            inv.setRepairCost(cost);
        }
        event.setResult(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTakeItem(InventoryClickEvent e) {
        if (e.isCancelled() || !(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.getType().equals(mapMaterial)) return;
        if (NBT.get(item, nbt -> { // 如果点击的物品有 flag，则将地图数据存入 nbt
            return nbt.hasTag(FLAG);
        })) {
            MapMeta mapMeta = getItemMeta(item);

            byte[] colors = MapUtils.getColors(mapMeta);
            List<MapCursor> cursors = MapUtils.getCursors(player, mapMeta);

            MapUtils.updateStaticMap(mapMeta, colors, cursors);
            item.setItemMeta(mapMeta);

            NBT.modify(item, nbt -> {
                nbt.removeKey(FLAG);
                nbt.setByteArray(COLORS, colors);
                if (cursors != null && !cursors.isEmpty()) {
                    byte[] bytes = toBytes(cursors);
                    if (bytes != null) {
                        nbt.setByteArray(CURSORS, bytes);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.isCancelled()) return;
        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof BlockInventoryHolder) {
            for (int i = 0; i < inv.getSize(); i++) {
                checkMapUpdate(inv.getItem(i));
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        CraftingInventory inv = e.getInventory();
        for (ItemStack item : inv.getMatrix()) {
            if (handleInv(inv, item, e)) break;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory().getClass().getName().contains("Cartography")) {
            handleInv(e.getClickedInventory(), e.getCurrentItem(), e);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getClass().getName().contains("Cartography")) {
            if (handleInv(e.getInventory(), e.getOldCursor(), e)) return;
            if (handleInv(e.getInventory(), e.getCursor(), e)) return;
            for (ItemStack item : e.getNewItems().values()) {
                if (handleInv(e.getInventory(), item, e)) return;
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        if (e.getDestination().getClass().getName().contains("Cartography")) {
            handleInv(e.getDestination(), e.getItem(), e);
        }
    }

    public boolean handleInv(Inventory inv, ItemStack item, Cancellable e) {
        if (isNotMap(item)) return false;
        byte[] colors = NBT.get(item, nbt -> {
            return bytes(nbt, COLORS);
        });
        if (colors == null) return false;
        if (!inv.getViewers().stream().allMatch(it -> it.hasPermission("staticmap.copy"))) {
            e.setCancelled(true);
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
