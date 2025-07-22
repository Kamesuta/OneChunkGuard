package com.kamesuta.onechunkguard.commands;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.InventoryUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnprotectCommand implements CommandExecutor {
    private final OneChunkGuard plugin;

    public UnprotectCommand(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
        InventoryUtils.giveProtectionBlock(player);
        player.sendMessage(plugin.getConfigManager().getMessage("unprotect-success"));
    }
}