package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UntrustCommand implements CommandExecutor {
    private final OneChunkGuard plugin;

    public UntrustCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command-only-player"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("untrust-usage"));
            return true;
        }

        // プレイヤーが保護を持っているかチェック
        ProtectionData protection = plugin.getDataManager().getPlayerProtection(player.getUniqueId());
        if (protection == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-protection"));
            return true;
        }

        // セレクター対応でプレイヤーを検索
        Player target = null;
        OfflinePlayer offlineTarget = null;
        String playerName = args[0];
        
        try {
            // セレクター（@p, @r, @a[name=...]等）の場合
            if (playerName.startsWith("@")) {
                var selectedEntities = Bukkit.selectEntities(player, playerName);
                if (selectedEntities.isEmpty()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                    return true;
                }
                var entity = selectedEntities.get(0);
                if (!(entity instanceof Player)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                    return true;
                }
                target = (Player) entity;
                offlineTarget = target;
            } else {
                // 通常のプレイヤー名の場合
                target = Bukkit.getPlayer(playerName);
                if (target != null) {
                    offlineTarget = target;
                } else {
                    // オフラインプレイヤーとして検索
                    offlineTarget = Bukkit.getOfflinePlayer(playerName);
                    if (!offlineTarget.hasPlayedBefore()) {
                        player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        // 自分自身をuntrustしようとした場合
        if (offlineTarget.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-untrust-self"));
            return true;
        }

        // プレイヤーが信頼されているかチェック
        if (!protection.getTrustedPlayers().contains(offlineTarget.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-trusted", "{player}", offlineTarget.getName()));
            return true;
        }

        // 信頼プレイヤーを削除
        plugin.getProtectionManager().removeTrustedPlayer(player.getUniqueId(), offlineTarget.getUniqueId());

        player.sendMessage(plugin.getConfigManager().getMessage("untrust-success", "{player}", offlineTarget.getName()));

        return true;
    }
}