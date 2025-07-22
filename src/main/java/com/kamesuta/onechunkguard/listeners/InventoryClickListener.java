package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.Bukkit;
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
            // すべての保護ブロックの移動を防止
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
            
            // 次のティックで保護ブロックを正しい位置に戻す
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 全インベントリをスキャンして保護ブロックを削除
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
                
                // スロット8に保護ブロックを配置
                player.getInventory().setItem(8, ItemUtils.createProtectionBlock());
            });
        }
    }
}