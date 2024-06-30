package com.molean.staticmap;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public class PDHSimplified {

    private final ItemMeta persistentDataHolder;

    private final static String NAMESPACE = "staticmap";


    private PDHSimplified(ItemMeta persistentDataHolder) {
        this.persistentDataHolder = persistentDataHolder;
    }

    public static PDHSimplified of(ItemMeta persistentDataHolder) {
        return new PDHSimplified(persistentDataHolder);
    }

    public boolean has(String key) {
        PersistentDataContainer persistentDataContainer = persistentDataHolder.getPersistentDataContainer();
        return persistentDataContainer.has(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)), PersistentDataType.BYTE_ARRAY);
    }


    public void setBytes(String key, byte[] bytes) {
        PersistentDataContainer persistentDataContainer = persistentDataHolder.getPersistentDataContainer();
        persistentDataContainer.set(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)), PersistentDataType.BYTE_ARRAY, bytes);
    }

    public byte[] getAsBytes(String key) {
        PersistentDataContainer persistentDataContainer = persistentDataHolder.getPersistentDataContainer();
        return persistentDataContainer.get(new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT)), PersistentDataType.BYTE_ARRAY);
    }

    public ItemMeta getHolder() {
        return persistentDataHolder;
    }

}
