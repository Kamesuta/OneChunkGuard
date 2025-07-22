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
     * インベントリからdefaultタイプの保護ブロックのみを削除する
     */
    public static void removeDefaultProtectionBlocks(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (ItemUtils.isProtectionBlock(item)) {
                String typeId = ItemUtils.getProtectionBlockTypeId(item);
                if ("default".equals(typeId)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }

        // カーソルにあるdefaultタイプの保護ブロックも削除
        ItemStack cursorItem = player.getItemOnCursor();
        if (ItemUtils.isProtectionBlock(cursorItem)) {
            String typeId = ItemUtils.getProtectionBlockTypeId(cursorItem);
            if ("default".equals(typeId)) {
                player.setItemOnCursor(null);
            }
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
     * 保護ブロックを配布する
     * defaultタイプはスロット8（ホットバー9番目）に固定配置
     * その他のタイプは通常のインベントリに配布
     */
    public static void giveProtectionBlock(Player player, String blockTypeId) {
        // 保護ブロックを作成
        ItemStack protectionBlock;
        if (blockTypeId != null) {
            protectionBlock = ItemUtils.createProtectionBlock(blockTypeId);
        } else {
            protectionBlock = ItemUtils.createProtectionBlock();
            blockTypeId = "default"; // デフォルト値
        }

        if ("default".equals(blockTypeId)) {
            // defaultブロックはスロット9に固定
            // 既存のdefaultタイプの保護ブロックを全て削除
            removeDefaultProtectionBlocks(player);

            // スロット9に物があれば落とす
            ItemStack existingItem = player.getInventory().getItem(8);
            if (existingItem != null && !existingItem.getType().isAir()) {
                player.getWorld().dropItemNaturally(player.getLocation(), existingItem);
            }

            // スロット9に配置
            player.getInventory().setItem(8, protectionBlock);
        } else {
            // defaultブロック以外は通常のインベントリに配布
            // インベントリに空きがない場合はドロップ
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), protectionBlock);
            } else {
                player.getInventory().addItem(protectionBlock);
            }
        }
    }
}