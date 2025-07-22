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

public class PlayerInteractListener implements Listener {
    private final OneChunkGuard plugin;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();
    private static final long COOLDOWN_MS = 500; // 500ms のクールダウン
    
    public PlayerInteractListener(OneChunkGuard plugin) {
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
        player.sendMessage(plugin.getConfigManager().getChatUIHeader());
        
        // メンバー追加ボタン
        TextComponent addMemberButton = new TextComponent(plugin.getConfigManager().getChatUIAddMember());
        addMemberButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/trust "));
        addMemberButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("クリックして /trust コマンドを入力").create()));
        player.spigot().sendMessage(addMemberButton);
        
        // メンバー削除ボタン
        TextComponent removeMemberButton = new TextComponent(plugin.getConfigManager().getChatUIRemoveMember());
        removeMemberButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/untrust "));
        removeMemberButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("クリックして /untrust コマンドを入力").create()));
        player.spigot().sendMessage(removeMemberButton);
        
        // メンバー一覧ボタン
        TextComponent listMembersButton = new TextComponent(plugin.getConfigManager().getChatUIListMembers());
        listMembersButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trustlist"));
        listMembersButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("クリックして信頼されたプレイヤー一覧を表示").create()));
        player.spigot().sendMessage(listMembersButton);
        
        // フッター
        player.sendMessage(plugin.getConfigManager().getChatUIFooter());
    }
}