package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TrustListCommand implements CommandExecutor {
    private final OneChunkGuard plugin;
    
    public TrustListCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command-only-player"));
            return true;
        }
        
        // プレイヤーが保護を持っているかチェック
        ProtectionData protection = plugin.getDataManager().getPlayerProtection(player.getUniqueId());
        if (protection == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-protection"));
            return true;
        }
        
        Set<UUID> trustedPlayers = protection.getTrustedPlayers();
        
        if (trustedPlayers.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("trust-list-empty"));
            return true;
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("trust-list-header"));
        
        for (UUID trustedId : trustedPlayers) {
            OfflinePlayer trustedPlayer = Bukkit.getOfflinePlayer(trustedId);
            String status = trustedPlayer.isOnline() ? "§aオンライン" : "§7オフライン";
            player.sendMessage(plugin.getConfigManager().getMessage("trust-list-entry", "{player}", trustedPlayer.getName(), "{status}", status));
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("trust-list-footer"));
        
        return true;
    }
}