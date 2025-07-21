package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {
    private final OneChunkGuard plugin;
    
    public InventoryClickListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // 保護ブロックが移動されているかチェック
        if (ItemUtils.isProtectionBlock(clickedItem) || ItemUtils.isProtectionBlock(cursorItem)) {
            // クリックされたアイテムが保護ブロックの場合
            if (ItemUtils.isProtectionBlock(clickedItem)) {
                // スロット8（ホットバー9番目）にあり、ホットバー内での移動の場合のみ許可
                if (event.getSlot() == 8) {
                    // スロット8からの取り出しはスロット8やホットバーに戻す場合のみ許可
                    if (event.getClick().isShiftClick() || 
                        (event.getSlot() >= 0 && event.getSlot() <= 8)) {
                        // これは許可
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
                    }
                } else {
                    // 保護ブロックが間違ったスロットにある - 移動を防止
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
                }
            }
            
            // カーソルに保護ブロックがある場合
            if (ItemUtils.isProtectionBlock(cursorItem)) {
                // スロット8またはホットバーへの配置のみ許可
                if (event.getSlot() != 8 && (event.getSlot() < 0 || event.getSlot() > 8)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
                }
            }
        }
    }
}