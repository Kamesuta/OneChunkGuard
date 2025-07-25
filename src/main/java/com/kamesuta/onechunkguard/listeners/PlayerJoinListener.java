package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.utils.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final OneChunkGuard plugin;

    public PlayerJoinListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // プレイヤーが既に保護ブロックを受け取っているかチェック
        if (!plugin.getDataManager().hasReceivedProtectionBlock(player.getUniqueId())) {
            // デフォルトの保護ブロックを配布
            InventoryUtils.giveProtectionBlock(player, "default");

            // プレイヤーがブロックを受け取ったことをマーク
            plugin.getDataManager().markPlayerReceivedBlock(player.getUniqueId());

            // ウェルカムメッセージを送信
            player.sendMessage(plugin.getConfigManager().getProtectionBlockDisplayName());
            player.sendMessage(plugin.getConfigManager().getMessage("protection-given"));
        }
    }
}