package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryDragListener implements Listener {
    private final OneChunkGuard plugin;
    
    public InventoryDragListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack draggedItem = event.getOldCursor();
        
        // 保護ブロックがドラッグされているかチェック
        if (ItemUtils.isProtectionBlock(draggedItem)) {
            // ドラッグスロットのいずれかがホットバー外またはスロット8以外かチェック
            for (int slot : event.getRawSlots()) {
                if (slot != 8 && (slot < 0 || slot > 8)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("cannot-move-item"));
                    return;
                }
            }
        }
    }
}