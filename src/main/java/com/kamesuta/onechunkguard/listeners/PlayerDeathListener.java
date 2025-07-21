package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class PlayerDeathListener implements Listener {
    private final OneChunkGuard plugin;
    
    public PlayerDeathListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // プレイヤーのドロップアイテムから保護ブロックを削除
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (ItemUtils.isProtectionBlock(item)) {
                iterator.remove();
                // プレイヤーのインベントリに保護ブロックを戻す（リスポーン時）
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.getInventory().setItem(8, ItemUtils.createProtectionBlock());
                }, 1L);
                break;
            }
        }
    }
}