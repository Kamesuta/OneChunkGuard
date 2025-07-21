package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {
    private final OneChunkGuard plugin;
    
    public PlayerDropItemListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // ドロップされたアイテムが保護ブロックかチェック
        if (ItemUtils.isProtectionBlock(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-drop-item"));
        }
    }
}