package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnprotectCommand implements CommandExecutor {
    private final OneChunkGuard plugin;
    
    public UnprotectCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }
        
        // 保護を解除してブロックを返却、もしくは保護がなくても保護ブロックを取得
        if (!plugin.getProtectionManager().removeProtection(player, true)) {
            // 保護がない場合でも保護ブロックを配布
            giveProtectionBlock(player);
        }
        
        return true;
    }
    
    private void giveProtectionBlock(Player player) {
        // 既存の保護ブロックを削除
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
            if (com.kamesuta.onechunkguard.utils.ItemUtils.isProtectionBlock(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        
        // スロット9に物があれば落とす
        org.bukkit.inventory.ItemStack existingItem = player.getInventory().getItem(8);
        if (existingItem != null && !existingItem.getType().isAir()) {
            player.getWorld().dropItemNaturally(player.getLocation(), existingItem);
        }
        
        player.getInventory().setItem(8, com.kamesuta.onechunkguard.utils.ItemUtils.createProtectionBlock());
        player.sendMessage(plugin.getConfigManager().getMessage("unprotect-success"));
    }
}