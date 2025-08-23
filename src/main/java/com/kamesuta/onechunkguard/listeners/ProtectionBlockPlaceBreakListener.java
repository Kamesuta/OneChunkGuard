package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 保護ブロックの設置/回収を管理するリスナー
 */
public class ProtectionBlockPlaceBreakListener implements Listener {
    private final OneChunkGuard plugin;

    public ProtectionBlockPlaceBreakListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block block = event.getBlock();

        // 保護ブロックの頭の位置に何かを置こうとしているかチェック
        if (isProtectionHeadLocation(block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        // これが保護ブロックかチェック
        if (!ItemUtils.isProtectionBlock(item)) {
            return;
        }

        // 使用権限チェック
        if (!player.hasPermission("onechunkguard.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            event.setCancelled(true);
            return;
        }

        // 保護の作成を試みる
        boolean success = plugin.getProtectionManager().createProtection(player, event.getBlock().getLocation(), item);

        if (!success) {
            // 保護作成に失敗した場合はイベントをキャンセル
            event.setCancelled(true);
        }
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
            boolean isOwner = protection.getOwner().equals(player.getUniqueId());

            // 利用権限チェック（管理者はバイパス）。所有者であっても use 権限が無ければ破壊不可
            if (!player.hasPermission("onechunkguard.use") && !player.hasPermission("onechunkguard.admin")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                event.setCancelled(true);
                return;
            }

            if (!plugin.getProtectionManager().canBreakProtectionBlock(player, blockLocation)) {
                // 他の人の保護ブロックか確認
                if (!isOwner) {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break-others"));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break"));
                }
                event.setCancelled(true);
                return;
            }

            // 所有者自身が破壊 -> 既存処理
            if (isOwner) {
                plugin.getProtectionManager().removeProtection(player, true, true);
                // デフォルトのブロック破壊をキャンセルし手動で処理
                event.setCancelled(true);
                return;
            }

            // 管理者が他人の保護を破壊 -> 強制解除（返却なし）
            if (player.hasPermission("onechunkguard.admin")) {
                boolean removed = plugin.getProtectionManager().forceRemoveProtection(protection.getOwner(), protection.getProtectionBlockTypeId());
                if (removed) {
                    player.sendMessage(plugin.getConfigManager().getMessage("protection-removed"));
                }
                event.setCancelled(true);
                return;
            }

            // 念のため取消
            event.setCancelled(true);
            return;
        }

        // 保護ブロックの上のプレイヤーヘッドもチェック
        if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
            Location belowLocation = blockLocation.clone().add(0, -1, 0);
            String belowChunkKey = belowLocation.getWorld().getName() + ":" +
                    belowLocation.getChunk().getX() + ":" +
                    belowLocation.getChunk().getZ();

            ProtectionData belowProtection = plugin.getDataManager().getChunkProtection(belowChunkKey);

            if (belowProtection != null && belowProtection.getProtectionBlockLocation().equals(belowLocation)) {
                // この頭は保護の一部なので、誰も壊せない（権限チェック）
                if (!player.hasPermission("onechunkguard.admin")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break"));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block targetBlock = event.getBlock();
        
        // 保護ブロックの頭の位置にバケツで液体を置こうとしているかチェック
        if (isProtectionHeadLocation(targetBlock.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 指定された位置が保護ブロックの頭の位置かどうかチェック
     */
    private boolean isProtectionHeadLocation(Location location) {
        // 一つ下のブロックが保護ブロックかチェック
        Location belowLocation = location.clone().add(0, -1, 0);
        String chunkKey = belowLocation.getWorld().getName() + ":" +
                belowLocation.getChunk().getX() + ":" +
                belowLocation.getChunk().getZ();

        ProtectionData protection = plugin.getDataManager().getChunkProtection(chunkKey);
        return protection != null && protection.getProtectionBlockLocation().equals(belowLocation);
    }
}