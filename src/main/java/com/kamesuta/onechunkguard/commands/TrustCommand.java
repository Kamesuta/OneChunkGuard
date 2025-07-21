package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustCommand implements CommandExecutor {
    private final OneChunkGuard plugin;
    
    public TrustCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }
        
        if (args.length != 1) {
            player.sendMessage("§c使用方法: /trust <プレイヤー名>");
            return true;
        }
        
        // プレイヤーが保護を持っているかチェック
        ProtectionData protection = plugin.getDataManager().getPlayerProtection(player.getUniqueId());
        if (protection == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-protection"));
            return true;
        }
        
        // 対象プレイヤーを検索
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        // 既に信頼されているかチェック
        if (protection.isTrusted(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " は既に信頼されたプレイヤーです！");
            return true;
        }
        
        // 信頼上限をチェック
        if (protection.getTrustedPlayers().size() >= plugin.getConfigManager().getMaxTrustedPlayers()) {
            player.sendMessage(plugin.getConfigManager().getMessage("trust-limit"));
            return true;
        }
        
        // 信頼プレイヤーを追加
        plugin.getProtectionManager().addTrustedPlayer(player.getUniqueId(), target.getUniqueId());
        
        player.sendMessage(plugin.getConfigManager().getMessage("trust-success", "{player}", target.getName()));
        
        return true;
    }
}