package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final OneChunkGuard plugin;
    
    public BlockBreakListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLocation = block.getLocation();
        
        // このブロックが保護ブロックかチェック
        String chunkKey = blockLocation.getWorld().getName() + ":" + 
                         blockLocation.getChunk().getX() + ":" + 
                         blockLocation.getChunk().getZ();
        
        ProtectionData protection = plugin.getDataManager().getChunkProtection(chunkKey);
        
        if (protection != null && protection.getProtectionBlockLocation().equals(blockLocation)) {
            // これは保護ブロック
            if (!plugin.getProtectionManager().canBreakProtectionBlock(player, blockLocation)) {
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-break"));
                event.setCancelled(true);
                return;
            }
            
            // 保護を解除
            plugin.getProtectionManager().removeProtection(player, true);
            
            // デフォルトのブロック破壊をキャンセルして手動で処理
            event.setCancelled(true);
        }
        
        // 保護ブロックの上のプレイヤーヘッドもチェック
        if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
            Location belowLocation = blockLocation.clone().add(0, -1, 0);
            String belowChunkKey = belowLocation.getWorld().getName() + ":" + 
                                  belowLocation.getChunk().getX() + ":" + 
                                  belowLocation.getChunk().getZ();
            
            ProtectionData belowProtection = plugin.getDataManager().getChunkProtection(belowChunkKey);
            
            if (belowProtection != null && belowProtection.getProtectionBlockLocation().equals(belowLocation)) {
                // この頭は保護の一部
                if (!belowProtection.isTrusted(player.getUniqueId()) && !player.isOp()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break"));
                    event.setCancelled(true);
                }
            }
        }
    }
}