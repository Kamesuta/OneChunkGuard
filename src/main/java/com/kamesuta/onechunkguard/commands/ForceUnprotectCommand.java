package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.managers.ConfigManager;
import com.kamesuta.onechunkguard.managers.DataManager;
import com.kamesuta.onechunkguard.managers.ProtectionManager;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 管理者専用の強制解除コマンド
 * 使い方: /force_unprotect <プレイヤー> [type]
 */
public class ForceUnprotectCommand implements CommandExecutor, TabCompleter {
    private final OneChunkGuard plugin;

    public ForceUnprotectCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        // 権限チェック（管理者のみ）
        if (!sender.hasPermission("onechunkguard.admin")) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        // 引数が不足している場合は使い方を表示
        if (args.length < 1) {
            sender.sendMessage("/" + label + " <player> [type]");
            return true;
        }

        String targetName = args[0];
        String typeId = args.length >= 2 ? args[1] : "default";

        // 種別の妥当性チェック
        ProtectionBlockType type = plugin.getConfigManager().getProtectionBlockType(typeId);
        if (type == null) {
            sender.sendMessage(config.getMessage("protection-type-not-found", "{type}", typeId));
            sender.sendMessage(config.getMessage("available-types", "{types}", String.join(", ", plugin.getConfigManager().getProtectionBlockTypes().keySet())));
            return true;
        }

        // 対象プレイヤーを解決（オンライン/オフライン対応）
        UUID targetId = null;
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            targetId = online.getUniqueId();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (off != null && off.hasPlayedBefore()) {
                targetId = off.getUniqueId();
            }
        }

        if (targetId == null) {
            sender.sendMessage(config.getMessage("target-player-not-found", "{player}", targetName));
            return true;
        }

        // 対象プレイヤーが当該種別の保護を持っているか確認
        DataManager dataManager = plugin.getDataManager();
        if (dataManager.getPlayerProtection(targetId, typeId) == null) {
            sender.sendMessage(config.getMessage("no-protection"));
            return true;
        }

        // 強制解除を実行
        ProtectionManager pm = plugin.getProtectionManager();
        boolean removed = pm.forceRemoveProtection(targetId, typeId);
        if (removed) {
            sender.sendMessage(config.getMessage("protection-removed"));
        } else {
            sender.sendMessage(config.getMessage("no-protection"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("onechunkguard.admin")) return completions;

        if (args.length == 1) {
            // 第1引数: プレイヤー名補完（オンライン）
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            // 第2引数: 保護種別ID補完
            String partial = args[1].toLowerCase();
            for (String id : plugin.getConfigManager().getProtectionBlockTypes().keySet()) {
                if (id.toLowerCase().startsWith(partial)) {
                    completions.add(id);
                }
            }
        }
        return completions;
    }
}

