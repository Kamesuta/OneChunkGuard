package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 保護ブロックを持ったときにチャンクの範囲をパーティクルで表示するリスナー
 */
public class ChunkVisualizerListener implements Listener {
    private final OneChunkGuard plugin;
    private final Map<UUID, BukkitRunnable> activeVisualizers = new HashMap<>();

    public ChunkVisualizerListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        
        // 新しく持ったアイテムが保護ブロックかチェック
        if (ItemUtils.isProtectionBlock(newItem)) {
            startVisualization(player, newItem);
        } else {
            stopVisualization(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // ビジュアライザーが有効でない場合は無視
        if (!activeVisualizers.containsKey(playerId)) {
            return;
        }
        
        // 異なるチャンクに移動した場合は再描画
        if (event.getFrom().getChunk() != event.getTo().getChunk()) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (ItemUtils.isProtectionBlock(heldItem)) {
                restartVisualization(player, heldItem);
            }
        }
    }

    /**
     * ビジュアライゼーションを開始
     */
    private void startVisualization(Player player, ItemStack protectionItem) {
        stopVisualization(player); // 既存のものを停止
        
        ProtectionBlockType blockType = ItemUtils.getProtectionBlockType(protectionItem);
        if (blockType == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        BukkitRunnable visualizer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !ItemUtils.isProtectionBlock(player.getInventory().getItemInMainHand())) {
                    cancel();
                    activeVisualizers.remove(playerId);
                    return;
                }
                
                showChunkBorders(player, blockType.getChunkRange());
            }
        };
        
        visualizer.runTaskTimer(plugin, 0L, 10L); // 0.5秒ごとに実行
        activeVisualizers.put(playerId, visualizer);
    }

    /**
     * ビジュアライゼーションを停止
     */
    private void stopVisualization(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable visualizer = activeVisualizers.remove(playerId);
        if (visualizer != null) {
            visualizer.cancel();
        }
    }

    /**
     * ビジュアライゼーションを再開始
     */
    private void restartVisualization(Player player, ItemStack protectionItem) {
        stopVisualization(player);
        startVisualization(player, protectionItem);
    }

    /**
     * チャンクの境界にパーティクルを表示
     */
    private void showChunkBorders(Player player, int chunkRange) {
        Location playerLocation = player.getLocation();
        Chunk centerChunk = playerLocation.getChunk();
        int playerY = (int) playerLocation.getY();
        
        int halfRange = chunkRange / 2;
        
        // 保護範囲内の各チャンクの境界を表示
        for (int dx = -halfRange; dx <= halfRange; dx++) {
            for (int dz = -halfRange; dz <= halfRange; dz++) {
                int targetChunkX = centerChunk.getX() + dx;
                int targetChunkZ = centerChunk.getZ() + dz;
                
                showSingleChunkBorder(player, targetChunkX, targetChunkZ, playerY);
            }
        }
    }

    /**
     * 単一チャンクの境界にパーティクルを表示
     */
    private void showSingleChunkBorder(Player player, int chunkX, int chunkZ, int y) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        
        // チャンクの4辺にパーティクルを表示
        // 北辺 (Z=minZ)
        for (int x = minX; x <= maxX; x += 2) {
            spawnParticle(player, x, y, minZ);
        }
        
        // 南辺 (Z=maxZ)
        for (int x = minX; x <= maxX; x += 2) {
            spawnParticle(player, x, y, maxZ);
        }
        
        // 西辺 (X=minX)
        for (int z = minZ + 2; z <= maxZ - 2; z += 2) {
            spawnParticle(player, minX, y, z);
        }
        
        // 東辺 (X=maxX)
        for (int z = minZ + 2; z <= maxZ - 2; z += 2) {
            spawnParticle(player, maxX, y, z);
        }
        
        // 角にも少し目立つパーティクルを表示
        spawnCornerParticle(player, minX, y, minZ);
        spawnCornerParticle(player, maxX, y, minZ);
        spawnCornerParticle(player, minX, y, maxZ);
        spawnCornerParticle(player, maxX, y, maxZ);
    }

    /**
     * パーティクルを表示
     */
    private void spawnParticle(Player player, double x, double y, double z) {
        Location loc = new Location(player.getWorld(), x + 0.5, y + 0.5, z + 0.5);
        player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0, 0, 0, 0);
    }

    /**
     * 角のパーティクルを表示（少し目立つように）
     */
    private void spawnCornerParticle(Player player, double x, double y, double z) {
        Location loc = new Location(player.getWorld(), x + 0.5, y + 0.5, z + 0.5);
        player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.2, 0.2, 0.2, 0);
    }
}