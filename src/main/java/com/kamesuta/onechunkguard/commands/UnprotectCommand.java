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
        
        // 保護を解除してブロックを返却
        plugin.getProtectionManager().removeProtection(player, true);
        
        return true;
    }
}