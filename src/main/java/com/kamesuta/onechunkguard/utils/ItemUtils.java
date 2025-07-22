package com.kamesuta.onechunkguard.utils;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class ItemUtils {
    private static final NamespacedKey PROTECTION_BLOCK_KEY =
            new NamespacedKey(OneChunkGuard.getInstance(), "protection_block");
    private static final NamespacedKey PROTECTION_BLOCK_TYPE_KEY =
            new NamespacedKey(OneChunkGuard.getInstance(), "protection_block_type");

    /**
     * デフォルトの保護ブロックを作成
     */
    public static ItemStack createProtectionBlock() {
        OneChunkGuard plugin = OneChunkGuard.getInstance();
        ProtectionBlockType defaultType = plugin.getConfigManager().getDefaultProtectionBlockType();
        
        if (defaultType == null) {
            plugin.getLogger().warning("No default protection block type found, creating emergency fallback");
            return createEmergencyProtectionBlock();
        }
        
        return createProtectionBlock(defaultType.getId());
    }
    
    /**
     * 指定タイプの保護ブロックを作成
     */
    public static ItemStack createProtectionBlock(String typeId) {
        OneChunkGuard plugin = OneChunkGuard.getInstance();
        ProtectionBlockType type = plugin.getConfigManager().getProtectionBlockType(typeId);
        
        if (type == null) {
            plugin.getLogger().warning("Protection block type not found: " + typeId + ", using default");
            return createProtectionBlock();
        }
        
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getDisplayName());
            meta.setLore(type.getLore());
            meta.getPersistentDataContainer().set(PROTECTION_BLOCK_KEY, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(PROTECTION_BLOCK_TYPE_KEY, PersistentDataType.STRING, typeId);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 緊急時用の保護ブロックを作成
     */
    private static ItemStack createEmergencyProtectionBlock() {
        ItemStack item = new ItemStack(Material.END_STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lProtection Block");
            meta.setLore(List.of("§7Place this block to", "§7protect a chunk", "§cOne chunk per person!"));
            meta.getPersistentDataContainer().set(PROTECTION_BLOCK_KEY, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(PROTECTION_BLOCK_TYPE_KEY, PersistentDataType.STRING, "default");
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * アイテムが保護ブロックかチェック
     */
    public static boolean isProtectionBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(PROTECTION_BLOCK_KEY, PersistentDataType.BOOLEAN);
    }
    
    /**
     * 保護ブロックのタイプIDを取得
     */
    public static String getProtectionBlockTypeId(ItemStack item) {
        if (!isProtectionBlock(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(PROTECTION_BLOCK_TYPE_KEY, PersistentDataType.STRING);
    }
    
    /**
     * 保護ブロックのタイプを取得
     */
    public static ProtectionBlockType getProtectionBlockType(ItemStack item) {
        String typeId = getProtectionBlockTypeId(item);
        if (typeId == null) {
            return null;
        }
        
        OneChunkGuard plugin = OneChunkGuard.getInstance();
        return plugin.getConfigManager().getProtectionBlockType(typeId);
    }
    
    /**
     * 利用可能なすべての保護ブロックタイプを取得
     */
    public static Map<String, ProtectionBlockType> getAllProtectionBlockTypes() {
        OneChunkGuard plugin = OneChunkGuard.getInstance();
        return plugin.getConfigManager().getProtectionBlockTypes();
    }
}