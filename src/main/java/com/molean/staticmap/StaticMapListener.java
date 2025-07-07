package com.molean.staticmap;

import com.google.common.collect.Iterables;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.molean.staticmap.MapUtils.fromBytes;
import static com.molean.staticmap.MapUtils.toBytes;

public class StaticMapListener implements Listener {
    public static final String COLORS = "staticmap_colors";
    public static final String CURSORS = "staticmap_cursors";
    public static final String FLAG = "staticmap_flag";
    public static final String IDS = "staticmap_ids";
    private final StaticMap plugin;
    private final Material mapMaterial;
    private final Set<UUID> preparing = new HashSet<>();
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

    public void checkMapUpdate(ItemStack item, Consumer<ItemStack> setItem) {
        if (isNotMap(item)) return;
        byte[][] pair = NBT.get(item, nbt -> {
            byte[] colors = bytes(nbt, COLORS);
            byte[] cursors = bytes(nbt, CURSORS);
            return new byte[][] { colors, cursors };
        });
        byte[] colors = pair[0];
        List<MapCursor> cursors = fromBytes(pair[1]);
        MapMeta itemMeta = getItemMeta(item);
        MapUtils.updateStaticMap(item, itemMeta, plugin.getServerName(), colors, cursors);
        if (setItem != null) {
            setItem.accept(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        preparing.remove(player.getUniqueId());
        plugin.getScheduler().runLater(() -> {
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                final int index = i;
                checkMapUpdate(inv.getItem(index), item -> inv.setItem(index, item));
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        int newSlot = event.getNewSlot();
        PlayerInventory inv = event.getPlayer().getInventory();
        checkMapUpdate(inv.getItem(newSlot), item -> inv.setItem(newSlot, item));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Player player = (Player) Iterables.find(event.getViewers(), it -> it instanceof Player, null);
        if (player == null || !player.hasPermission("staticmap.use")) return;
        UUID uuid = player.getUniqueId();
        AnvilInventory inv = event.getInventory();
        if (preparing.contains(uuid)) return;

        ItemStack firstItem = inv.getItem(0);
        ItemStack secondItem = inv.getItem(1);
        if (isNotMap(firstItem)) return;
        if (secondItem != null && !secondItem.getType().equals(Material.AIR)) return;
        if (NBT.get(firstItem, nbt -> nbt.hasTag(COLORS) || nbt.hasTag(FLAG))) return;
        preparing.add(uuid);

        plugin.getScheduler().runLater(() -> {
            preparing.remove(uuid);
            ItemStack itemStack = firstItem.clone();
            MapMeta itemMeta = getItemMeta(itemStack);

            String renameText = inv.getRenameText();
            if (renameText != null && !renameText.isEmpty()) {
                itemMeta.setDisplayName(renameText);
            } else {
                itemMeta.setDisplayName(PAPI.setPlaceholders(player, plugin.getMapName()));
            }
            itemMeta.setLore(PAPI.setPlaceholders(player, plugin.getMapLore()));
            itemStack.setItemMeta(itemMeta);

            NBT.modify(itemStack, nbt -> { // 只添加 flag，地图数据之后再加，以免占用地图ID
                nbt.setBoolean(FLAG, true);
            });
            Integer cost = plugin.getMapCost();
            if (cost != null) {
                inv.setRepairCost(cost);
            }
            event.setResult(itemStack);
            inv.setItem(2, itemStack);
        }, 1L); // 延时 1 tick，防止与 EcoEnchants 冲突
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTakeItem(InventoryClickEvent e) {
        if (e.isCancelled() || !(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (isNotMap(item)) return;
        if (NBT.get(item, nbt -> { // 如果点击的物品有 flag，则将地图数据存入 nbt
            return nbt.hasTag(FLAG);
        })) {
            MapMeta mapMeta = getItemMeta(item);

            byte[] colors = MapUtils.getColors(mapMeta);
            List<MapCursor> cursors = MapUtils.getCursors(player, mapMeta);

            MapUtils.updateStaticMap(item, mapMeta, plugin.getServerName(), colors, cursors);

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
                final int index = i;
                checkMapUpdate(inv.getItem(index), item -> inv.setItem(index, item));
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
