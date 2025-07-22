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

        // これが保護ブロックかチェック
        if (!ItemUtils.isProtectionBlock(item)) {
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
            if (!plugin.getProtectionManager().canBreakProtectionBlock(player, blockLocation)) {
                // 他の人の保護ブロックか確認
                if (!protection.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break-others"));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break"));
                }
                event.setCancelled(true);
                return;
            }

            // 保護を解除（ブロック破壊からの呼び出しなのでtrue）
            plugin.getProtectionManager().removeProtection(player, true, true);

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
                // この頭は保護の一部なので、誰も壊せない（権限チェック）
                if (!player.hasPermission("onechunkguard.admin")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-break"));
                    event.setCancelled(true);
                }
            }
        }
    }
}