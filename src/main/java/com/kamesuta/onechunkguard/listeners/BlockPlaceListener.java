package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {
    private final OneChunkGuard plugin;
    
    public BlockPlaceListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // これが保護ブロックかチェック
        if (!ItemUtils.isProtectionBlock(item)) {
            return;
        }
        
        // 保護の作成を試みる
        boolean success = plugin.getProtectionManager().createProtection(player, event.getBlock().getLocation());
        
        if (!success) {
            // 保護作成に失敗した場合はイベントをキャンセル
            event.setCancelled(true);
        }
    }
}