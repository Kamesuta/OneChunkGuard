package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.InventoryUtils;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 保護ブロックをスロット9に固定するリスナー
 * 死亡時の処理、インベントリ操作、ドロップ、F-キー入れ替えを管理
 */
public class ProtectionBlockInventoryListener implements Listener {
    private final OneChunkGuard plugin;

    public ProtectionBlockInventoryListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // defaultタイプの保護ブロックのみ移動を防止
        if (isDefaultProtectionBlock(clickedItem) || isDefaultProtectionBlock(cursorItem)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));

            // 次のティックで保護ブロックを正しい位置に戻す
            Bukkit.getScheduler().runTask(plugin, () -> {
                InventoryUtils.giveProtectionBlock(player, "default");
            });
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // defaultタイプの保護ブロックのみドラッグを防止
        if (isDefaultProtectionBlock(event.getOldCursor()) || isDefaultProtectionBlock(event.getCursor())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        // defaultタイプの保護ブロックのみドロップを防止
        if (isDefaultProtectionBlock(droppedItem)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("cannot-drop-item"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 死亡時のドロップからdefaultタイプの保護ブロックのみ削除
        event.getDrops().removeIf(this::isDefaultProtectionBlock);

        // 死亡後のリスポーン時にdefaultタイプの保護ブロックを再配置
        Player player = event.getEntity();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                InventoryUtils.giveProtectionBlock(player, "default");
            }
        }, 5L); // 5ティック後に実行
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        // defaultタイプの保護ブロックのみ手の入れ替えを防止
        if (isDefaultProtectionBlock(mainHand) || isDefaultProtectionBlock(offHand)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
        }
    }

    /**
     * defaultタイプの保護ブロックかチェック
     */
    private boolean isDefaultProtectionBlock(ItemStack item) {
        if (!ItemUtils.isProtectionBlock(item)) {
            return false;
        }
        
        String typeId = ItemUtils.getProtectionBlockTypeId(item);
        return "default".equals(typeId);
    }
}