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

public class TrustCommand implements CommandExecutor {
    private final OneChunkGuard plugin;

    public TrustCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command-only-player"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("trust-usage"));
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
        // 自分自身をtrustしようとした場合
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-trust-self"));
            return true;
        }

        // 既に信頼されているかチェック
        if (protection.isTrusted(target.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-trusted", "{player}", target.getName()));
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