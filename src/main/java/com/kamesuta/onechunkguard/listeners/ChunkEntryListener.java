package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * チャンクに入ったときにメッセージを出すリスナー
 */
public class ChunkEntryListener implements Listener {
    private final OneChunkGuard plugin;
    private final Map<UUID, String> lastChunkKey = new HashMap<>();
    
    public ChunkEntryListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isShowOwnerActionBar()) {
            return;
        }
        
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();
        
        // 同じチャンク内の移動は無視
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }
        
        String chunkKey = toChunk.getWorld().getName() + ":" + toChunk.getX() + ":" + toChunk.getZ();
        
        // 最後に表示したチャンクと同じなら無視
        String lastKey = lastChunkKey.get(player.getUniqueId());
        if (chunkKey.equals(lastKey)) {
            return;
        }
        
        lastChunkKey.put(player.getUniqueId(), chunkKey);
        
        // このチャンクが保護されているかチェック
        ProtectionData protection = plugin.getDataManager().getChunkProtection(chunkKey);
        
        if (protection != null) {
            String ownerName = Bukkit.getOfflinePlayer(protection.getOwner()).getName();
            if (ownerName == null) ownerName = "不明なプレイヤー";
            
            String message = plugin.getConfigManager().getMessage("owner-info", "{owner}", ownerName);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}