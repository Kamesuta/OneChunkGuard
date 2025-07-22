package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理者用の保護ブロック配布コマンド
 */
public class GiveProtectionBlockCommand implements CommandExecutor, TabCompleter {
    private final OneChunkGuard plugin;

    public GiveProtectionBlockCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 権限チェック
        if (!sender.hasPermission("onechunkguard.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // 使用方法チェック
        if (args.length < 2) {
            sender.sendMessage("§c使用方法: /giveprotectionblock <プレイヤー名> <保護ブロック種類> [数量]");
            return true;
        }

        String targetPlayerName = args[0];
        
        // プレイヤーを取得（セレクター対応）
        Player targetPlayer = null;
        
        try {
            // セレクター（@p, @r, @a[name=...]等）の場合
            if (targetPlayerName.startsWith("@")) {
                var selectedEntities = org.bukkit.Bukkit.selectEntities(sender, targetPlayerName);
                if (selectedEntities.isEmpty()) {
                    sender.sendMessage("§cセレクターで対象が見つかりません。");
                    return true;
                }
                var entity = selectedEntities.get(0);
                if (!(entity instanceof Player)) {
                    sender.sendMessage("§c対象がプレイヤーではありません。");
                    return true;
                }
                targetPlayer = (Player) entity;
            } else {
                // 通常のプレイヤー名の場合
                targetPlayer = Bukkit.getPlayer(targetPlayerName);
            }
        } catch (Exception e) {
            sender.sendMessage("§c無効なセレクターまたはプレイヤー名です。");
            return true;
        }
        String blockTypeId = args[1];
        int amount = 1;

        // 数量の解析
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§c数量は1から64の間で指定してください。");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c数量は数字で指定してください。");
                return true;
            }
        }

        // プレイヤーが見つからない場合
        if (targetPlayer == null) {
            sender.sendMessage("§cプレイヤー " + targetPlayerName + " が見つかりません。");
            return true;
        }

        // 保護ブロックタイプを取得
        ProtectionBlockType blockType = plugin.getConfigManager().getProtectionBlockType(blockTypeId);
        if (blockType == null) {
            sender.sendMessage("§c保護ブロック種類 '" + blockTypeId + "' が見つかりません。");
            sender.sendMessage("§7利用可能な種類: " + String.join(", ", plugin.getConfigManager().getProtectionBlockTypes().keySet()));
            return true;
        }

        // 保護ブロックを作成して配布
        for (int i = 0; i < amount; i++) {
            ItemStack protectionBlock = ItemUtils.createProtectionBlock(blockTypeId);
            
            // インベントリに空きがない場合はドロップ
            if (targetPlayer.getInventory().firstEmpty() == -1) {
                targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), protectionBlock);
            } else {
                targetPlayer.getInventory().addItem(protectionBlock);
            }
        }

        // 成功メッセージ
        String blockName = blockType.getDisplayName();
        sender.sendMessage("§a" + targetPlayerName + " に " + blockName + " を " + amount + " 個配布しました。");
        targetPlayer.sendMessage("§a管理者から " + blockName + " を " + amount + " 個受け取りました。");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("onechunkguard.admin")) {
            return completions;
        }

        if (args.length == 1) {
            // プレイヤー名の補完
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // 保護ブロック種類の補完
            String partial = args[1].toLowerCase();
            Map<String, ProtectionBlockType> blockTypes = plugin.getConfigManager().getProtectionBlockTypes();
            for (String typeId : blockTypes.keySet()) {
                if (typeId.toLowerCase().startsWith(partial)) {
                    completions.add(typeId);
                }
            }
        } else if (args.length == 3) {
            // 数量の補完
            completions.add("1");
            completions.add("5");
            completions.add("10");
        }

        return completions;
    }
}