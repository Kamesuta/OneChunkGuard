package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

/**
 * 保護ブロックの物理的な移動（ピストンなど）と爆発から保護するリスナー
 */
public class ProtectionBlockPhysicsListener implements Listener {
    private final OneChunkGuard plugin;

    public ProtectionBlockPhysicsListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // ピストンが押すブロックをチェック
        for (Block block : event.getBlocks()) {
            if (isProtectionBlock(block.getLocation()) || isProtectionHead(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        // ピストンが引くブロックをチェック
        for (Block block : event.getBlocks()) {
            if (isProtectionBlock(block.getLocation()) || isProtectionHead(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 指定された位置が保護ブロックかどうかチェック
     */
    private boolean isProtectionBlock(Location location) {
        String chunkKey = location.getWorld().getName() + ":" +
                location.getChunk().getX() + ":" +
                location.getChunk().getZ();

        ProtectionData protection = plugin.getDataManager().getChunkProtection(chunkKey);
        return protection != null && protection.getProtectionBlockLocation().equals(location);
    }

    /**
     * 指定されたブロックが保護ブロックの上の頭かどうかチェック
     */
    private boolean isProtectionHead(Block block) {
        if (block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) {
            return false;
        }

        Location belowLocation = block.getLocation().clone().add(0, -1, 0);
        return isProtectionBlock(belowLocation);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // 爆発で破壊されるブロックのリストから保護ブロックと頭を除外
        event.blockList().removeIf(block -> 
            isProtectionBlock(block.getLocation()) || isProtectionHead(block)
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        // ブロック爆発で破壊されるブロックのリストから保護ブロックと頭を除外
        event.blockList().removeIf(block -> 
            isProtectionBlock(block.getLocation()) || isProtectionHead(block)
        );
    }
}