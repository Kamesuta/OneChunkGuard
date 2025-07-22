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
        
        // 保護ブロックが移動されているかチェック
        if (ItemUtils.isProtectionBlock(clickedItem) || ItemUtils.isProtectionBlock(cursorItem)) {
            // すべての保護ブロックの移動を防止
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
            
            // 次のティックで保護ブロックを正しい位置に戻す
            Bukkit.getScheduler().runTask(plugin, () -> {
                InventoryUtils.giveProtectionBlock(player);
            });
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // ドラッグされたアイテムが保護ブロックかチェック
        if (ItemUtils.isProtectionBlock(event.getOldCursor()) || ItemUtils.isProtectionBlock(event.getCursor())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // 保護ブロックがドロップされたかチェック
        if (ItemUtils.isProtectionBlock(droppedItem)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("cannot-drop-item"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 死亡時のドロップから保護ブロックを削除
        event.getDrops().removeIf(ItemUtils::isProtectionBlock);
        
        // 死亡後のリスポーン時に保護ブロックを再配置
        Player player = event.getEntity();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                InventoryUtils.giveProtectionBlock(player);
            }
        }, 5L); // 5ティック後に実行
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        
        // どちらかの手に保護ブロックがあるかチェック
        if (ItemUtils.isProtectionBlock(mainHand) || ItemUtils.isProtectionBlock(offHand)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
        }
    }
}