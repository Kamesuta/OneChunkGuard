package com.kamesuta.onechunkguard.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {

    /**
     * インベントリから全ての保護ブロックを削除する
     */
    public static void removeAllProtectionBlocks(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (ItemUtils.isProtectionBlock(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        // カーソルにある保護ブロックも削除
        if (ItemUtils.isProtectionBlock(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    /**
     * スロット8（ホットバー9番目）にデフォルトの保護ブロックを配置する
     * 既存のアイテムがある場合はドロップする
     */
    public static void giveProtectionBlock(Player player) {
        giveProtectionBlock(player, null);
    }
    
    /**
     * スロット8（ホットバー9番目）に指定種類の保護ブロックを配置する
     * 既存のアイテムがある場合はドロップする
     */
    public static void giveProtectionBlock(Player player, String blockTypeId) {
        // 既存の保護ブロックを全て削除
        removeAllProtectionBlocks(player);

        // スロット9に物があれば落とす
        ItemStack existingItem = player.getInventory().getItem(8);
        if (existingItem != null && !existingItem.getType().isAir()) {
            player.getWorld().dropItemNaturally(player.getLocation(), existingItem);
        }

        // 保護ブロックを配置
        ItemStack protectionBlock;
        if (blockTypeId != null) {
            protectionBlock = ItemUtils.createProtectionBlock(blockTypeId);
        } else {
            protectionBlock = ItemUtils.createProtectionBlock();
        }
        player.getInventory().setItem(8, protectionBlock);
    }
}