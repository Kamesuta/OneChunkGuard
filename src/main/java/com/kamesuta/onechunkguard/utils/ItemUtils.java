package com.kamesuta.onechunkguard.utils;

import com.kamesuta.onechunkguard.OneChunkGuard;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemUtils {
    private static final NamespacedKey PROTECTION_BLOCK_KEY =
            new NamespacedKey(OneChunkGuard.getInstance(), "protection_block");

    public static ItemStack createProtectionBlock() {
        OneChunkGuard plugin = OneChunkGuard.getInstance();
        Material material = plugin.getConfigManager().getProtectionBlockMaterial();
        String displayName = plugin.getConfigManager().getProtectionBlockDisplayName();
        List<String> lore = plugin.getConfigManager().getProtectionBlockLore();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(PROTECTION_BLOCK_KEY, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static boolean isProtectionBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(PROTECTION_BLOCK_KEY, PersistentDataType.BOOLEAN);
    }
}