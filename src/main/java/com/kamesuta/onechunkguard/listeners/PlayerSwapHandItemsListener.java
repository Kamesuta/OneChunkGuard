package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerSwapHandItemsListener implements Listener {
    private final OneChunkGuard plugin;
    
    public PlayerSwapHandItemsListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        // メインハンドまたはオフハンドに保護ブロックがある場合はスワップを禁止
        if (ItemUtils.isProtectionBlock(event.getMainHandItem()) || 
            ItemUtils.isProtectionBlock(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
        }
    }
}