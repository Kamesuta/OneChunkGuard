package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.models.ProtectionData;
import com.kamesuta.onechunkguard.utils.InventoryUtils;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnprotectCommand implements CommandExecutor, TabCompleter {
    private final OneChunkGuard plugin;

    public UnprotectCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command-only-player"));
            return true;
        }

        // 引数がある場合は種類指定
        if (args.length >= 1) {
            String blockTypeId = args[0];
            return handleUnprotectWithType(player, blockTypeId);
        } else {
            // 引数がない場合は既存の動作
            return handleUnprotectDefault(player);
        }
    }
    
    /**
     * デフォルトの/unprotect動作（defaultブロックのみ対象）
     */
    private boolean handleUnprotectDefault(Player player) {
        // defaultタイプの保護があるかチェック
        ProtectionData defaultProtection = plugin.getDataManager().getPlayerProtection(player.getUniqueId(), "default");
        
        if (defaultProtection != null) {
            // defaultブロックの保護を解除してブロックを返却
            plugin.getProtectionManager().removeProtection(player, "default", true);
        } else {
            // defaultの保護がない場合でもデフォルト保護ブロックを配布
            giveProtectionBlock(player, "default");
        }
        return true;
    }
    
    /**
     * 種類指定での/unprotect動作
     */
    private boolean handleUnprotectWithType(Player player, String blockTypeId) {
        // 指定された種類が存在するかチェック
        ProtectionBlockType blockType = plugin.getConfigManager().getProtectionBlockType(blockTypeId);
        if (blockType == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("protection-type-not-found", "{type}", blockTypeId));
            player.sendMessage(plugin.getConfigManager().getMessage("available-types", "{types}", String.join(", ", plugin.getConfigManager().getProtectionBlockTypes().keySet())));
            return true;
        }
        
        // 指定種類の保護を確認
        ProtectionData targetProtection = plugin.getDataManager().getPlayerProtection(player.getUniqueId(), blockTypeId);
        
        if (targetProtection != null) {
            // 指定された種類の保護がある場合は解除
            plugin.getProtectionManager().removeProtection(player, blockTypeId, true);
        } else {
            // 保護がない場合の処理
            if ("default".equals(blockTypeId)) {
                // defaultの場合は保護ブロックを配布
                giveProtectionBlock(player, blockTypeId);
            } else {
                // default以外の場合は返却なし
                player.sendMessage(plugin.getConfigManager().getMessage("no-protection"));
            }
        }
        
        return true;
    }

    private void giveProtectionBlock(Player player, String blockTypeId) {
        if (blockTypeId != null) {
            InventoryUtils.giveProtectionBlock(player, blockTypeId);
        } else {
            InventoryUtils.giveProtectionBlock(player);
        }
        player.sendMessage(plugin.getConfigManager().getMessage("unprotect-success"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 保護ブロック種類の補完
            String partial = args[0].toLowerCase();
            Map<String, ProtectionBlockType> blockTypes = plugin.getConfigManager().getProtectionBlockTypes();
            for (String typeId : blockTypes.keySet()) {
                if (typeId.toLowerCase().startsWith(partial)) {
                    completions.add(typeId);
                }
            }
        }
        
        return completions;
    }
}