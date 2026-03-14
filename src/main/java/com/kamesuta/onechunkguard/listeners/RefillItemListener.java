package com.kamesuta.onechunkguard.listeners;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 保護チャンク内で補充アイテムをインベントリに持ったとき自動消費するリスナー。
 * プレイヤーが保護チャンク（自分の所有）の中にいて、設定された補充アイテムを
 * 持っている場合にアイテムを消費して残り時間を延長する。
 *
 * タイミング：
 * - プレイヤーが保護チャンクに入ったとき（移動イベント）
 * - アイテムをピックアップしたとき
 */
public class RefillItemListener implements Listener {
    private final OneChunkGuard plugin;

    public RefillItemListener(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    /** 最後に補充を試みたチャンクキー（チャンクに入るたびに補充を試みる） */
    private final Map<UUID, String> lastRefillChunk = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // チャンクをまたいだとき
        if (event.getFrom().getChunk().getX() == event.getTo().getChunk().getX()
                && event.getFrom().getChunk().getZ() == event.getTo().getChunk().getZ()) {
            return;
        }

        String chunkKey = event.getTo().getChunk().getWorld().getName() + ":"
                + event.getTo().getChunk().getX() + ":" + event.getTo().getChunk().getZ();

        // 補充チェックは入場時のみ（スパムを避ける）
        String lastKey = lastRefillChunk.get(player.getUniqueId());
        if (chunkKey.equals(lastKey)) return;
        lastRefillChunk.put(player.getUniqueId(), chunkKey);

        checkAndRefill(player, chunkKey);
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        // アイテムピックアップ後に補充を試みる
        Player player = event.getPlayer();
        String chunkKey = player.getLocation().getChunk().getWorld().getName() + ":"
                + player.getLocation().getChunk().getX() + ":" + player.getLocation().getChunk().getZ();

        // 少し遅延させてアイテムがインベントリに入ってから補充する
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkAndRefill(player, chunkKey);
            }
        }, 1L);
    }

    /**
     * 指定チャンクが自分の保護チャンクならば補充アイテムを消費して時間延長を試みる
     */
    private void checkAndRefill(Player player, String chunkKey) {
        ProtectionData data = plugin.getDataManager().getChunkProtection(chunkKey);
        if (data == null) return;

        // 自分の所有チャンクかチェック
        if (!data.getOwner().equals(player.getUniqueId())) return;

        // 時間制限があるかチェック
        ProtectionBlockType type = plugin.getConfigManager()
                .getProtectionBlockType(data.getProtectionBlockTypeId());
        if (type == null || !type.hasTimeLimit()) return;
        if (type.getRefillItems().isEmpty()) return;

        // 残り時間がある程度残っていれば補充しない（すでに十分な場合）
        // ただし補充アイテムをインベントリに持っている場合は積極的に補充する
        boolean hasRefillItem = hasAnyRefillItem(player, type);
        if (!hasRefillItem) return;

        // 補充を実行
        plugin.getTimeLimitManager().tryRefill(player, data);
    }

    /**
     * プレイヤーが補充アイテムを持っているかチェック
     */
    private boolean hasAnyRefillItem(Player player, ProtectionBlockType type) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (type.getRefillItems().containsKey(item.getType())) {
                return true;
            }
        }
        return false;
    }
}
