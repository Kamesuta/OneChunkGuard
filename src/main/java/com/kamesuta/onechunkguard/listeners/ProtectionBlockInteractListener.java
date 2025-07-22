package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 保護ブロックの右クリック時のTUI表示を管理するリスナー
 */
public class ProtectionBlockInteractListener implements Listener {
    private static final long COOLDOWN_MS = 500; // 500ms のクールダウン
    private final OneChunkGuard plugin;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();

    public ProtectionBlockInteractListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Location blockLocation = event.getClickedBlock().getLocation();

        // これが保護ブロックかチェック
        String chunkKey = blockLocation.getWorld().getName() + ":" +
                blockLocation.getChunk().getX() + ":" +
                blockLocation.getChunk().getZ();

        ProtectionData protection = plugin.getDataManager().getChunkProtection(chunkKey);

        if (protection != null && protection.getProtectionBlockLocation().equals(blockLocation)) {
            // これは保護ブロック
            if (!protection.getOwner().equals(player.getUniqueId())) {
                // 所有者ではないのでUIを表示しない
                return;
            }

            // クールダウンチェック
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastInteraction.get(playerId);

            if (lastTime != null && currentTime - lastTime < COOLDOWN_MS) {
                event.setCancelled(true);
                return;
            }

            lastInteraction.put(playerId, currentTime);

            // チャットTUIを表示
            showChatTUI(player);
            event.setCancelled(true);
        }
    }

    private void showChatTUI(Player player) {
        // ヘッダー
        player.sendMessage(plugin.getConfigManager().getMessage("chat-ui.header"));

        // メンバー追加ボタン
        TextComponent addMemberButton = new TextComponent(plugin.getConfigManager().getMessage("chat-ui.add-member"));
        addMemberButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/trust "));
        addMemberButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(plugin.getConfigManager().getMessage("chat-ui.add-member-hover")).create()));
        player.spigot().sendMessage(addMemberButton);

        // メンバー削除ボタン
        TextComponent removeMemberButton = new TextComponent(plugin.getConfigManager().getMessage("chat-ui.remove-member"));
        removeMemberButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/untrust "));
        removeMemberButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(plugin.getConfigManager().getMessage("chat-ui.remove-member-hover")).create()));
        player.spigot().sendMessage(removeMemberButton);

        // メンバー一覧ボタン
        TextComponent listMembersButton = new TextComponent(plugin.getConfigManager().getMessage("chat-ui.list-members"));
        listMembersButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trustlist"));
        listMembersButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(plugin.getConfigManager().getMessage("chat-ui.list-members-hover")).create()));
        player.spigot().sendMessage(listMembersButton);

        // フッター
        player.sendMessage(plugin.getConfigManager().getMessage("chat-ui.footer"));
    }
}