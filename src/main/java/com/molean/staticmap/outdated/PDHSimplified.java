package com.molean.staticmap.outdated;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class PDHSimplified {

    @Nullable
    private final ItemMeta holder;

    private final static String NAMESPACE = "staticmap";


    private PDHSimplified(@Nullable ItemMeta holder) {
        this.holder = holder;
    }

    public static PDHSimplified of(ItemMeta persistentDataHolder) {
        return new PDHSimplified(persistentDataHolder);
    }

    public boolean has(String key) {
        if (holder == null) return false;
        PersistentDataContainer container = holder.getPersistentDataContainer();
        return container.has(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)), PersistentDataType.BYTE_ARRAY);
    }


    public void setBytes(String key, byte[] bytes) {
        if (holder == null) throw new IllegalStateException("ItemMeta == null");
        PersistentDataContainer container = holder.getPersistentDataContainer();
        container.set(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)), PersistentDataType.BYTE_ARRAY, bytes);
    }

    public byte[] getAsBytes(String key) {
        if (holder == null) return null;
        PersistentDataContainer container = holder.getPersistentDataContainer();
        return container.get(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)), PersistentDataType.BYTE_ARRAY);
    }

    public void remove(String key) {
        if (holder == null) throw new IllegalStateException("ItemMeta == null");
        PersistentDataContainer container = holder.getPersistentDataContainer();
        container.remove(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)));
    }

    @Nullable
    public ItemMeta getHolder() {
        return holder;
    }

}
