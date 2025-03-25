package com.molean.staticmap.outdated;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;

public class DataSimplified {
    private static boolean isPDHAvailable;
    static {
        try {
            Class.forName("org.bukkit.persistence.PersistentDataHolder");
            isPDHAvailable = true;
        } catch (ClassNotFoundException ignored) {
            isPDHAvailable = false;
        }
    }
    public static boolean isPDHNotAvailable() {
        return !isPDHAvailable;
    }
    final PDHSimplified pdh;
    final NBTContainer nbt;

    DataSimplified(ItemStack item) {
        if (isPDHAvailable){
            pdh = PDHSimplified.of(item.getItemMeta());
            nbt = null;
        } else {
            pdh = null;
            nbt = NBTItem.convertItemtoNBT(item);
        }
    }

    public static DataSimplified of(ItemStack item) {
        return new DataSimplified(item);
    }

    public ItemStack nbtToItemStack() {
        if (nbt != null) return NBTItem.convertNBTtoItem(nbt);
        return null;
    }

    public boolean has(String key) {
        if (pdh != null) return pdh.has(key);
        if (nbt != null && nbt.getCompound("tag") != null) {
            return nbt.getCompound("tag").hasTag("staticmap:" + key);
        }
        return false;
    }


    public void setBytes(String key, byte[] bytes) {
        if (pdh != null){
            pdh.setBytes(key, bytes);
            return;
        }
        if (nbt != null) {
            NBTCompound tag = nbt.getOrCreateCompound("tag");
            tag.setByteArray("staticmap:" + key, bytes);
        }
    }

    public byte[] getAsBytes(String key) {
        if (pdh != null){
            return pdh.getAsBytes(key);
        }
        if (nbt != null && nbt.getCompound("tag") != null) {
            return nbt.getCompound("tag").getByteArray("staticmap:" + key);
        }
        return null;
    }

    public void remove(String key) {
        if (pdh != null) {
            pdh.remove(key);
            return;
        }
        if (nbt != null && nbt.getCompound("tag") != null) {
            nbt.getCompound("tag").removeKey("staticmap:" + key);
        }
    }
}
