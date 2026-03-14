package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.models.ProtectionData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 保護チャンクの時間制限を管理するマネージャー
 * - 残り時間のアクションバー表示
 * - 補充アイテムの消費と時間延長
 * - 期限切れ保護の自動解除
 */
public class TimeLimitManager {

    private final OneChunkGuard plugin;
    /** アクションバー更新・期限切れチェック用タスク */
    private BukkitTask tickTask;
    /** プレイヤーが現在いるチャンクの保護データ（アクションバー用） */
    private final Map<UUID, ProtectionData> playerCurrentChunk = new HashMap<>();
    /** 各保護の補充カウント（コスト計算用）: "playerUUID:typeId" -> 補充済み回数 */
    private final Map<String, Integer> refillCounts = new HashMap<>();

    public TimeLimitManager(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    /**
     * タスクを開始する（onEnable時に呼ぶ）
     */
    public void start() {
        // 1秒ごとにアクションバー更新と期限切れチェック
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /**
     * タスクを停止する（onDisable時に呼ぶ）
     */
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    /**
     * プレイヤーが現在いるチャンクを更新する（ChunkEntryListenerから呼ぶ）
     */
    public void updatePlayerChunk(UUID playerId, ProtectionData protection) {
        if (protection == null) {
            playerCurrentChunk.remove(playerId);
        } else {
            playerCurrentChunk.put(playerId, protection);
        }
    }

    /**
     * プレイヤーが退出したときにクリア
     */
    public void removePlayer(UUID playerId) {
        playerCurrentChunk.remove(playerId);
    }

    /**
     * 毎秒の処理: アクションバー表示 + 期限切れチェック
     */
    private void tick() {
        // アクションバーに残り時間表示
        for (Map.Entry<UUID, ProtectionData> entry : new HashMap<>(playerCurrentChunk).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                playerCurrentChunk.remove(entry.getKey());
                continue;
            }

            ProtectionData data = entry.getValue();
            ProtectionBlockType type = plugin.getConfigManager().getProtectionBlockType(data.getProtectionBlockTypeId());
            if (type == null || !type.hasTimeLimit()) continue;

            // 残り時間を取得してアクションバーに表示
            String msg = buildActionBarMessage(data, type, player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
        }

        // 期限切れ保護をチェックして自動解除
        List<ProtectionData> expired = new ArrayList<>();
        for (ProtectionData data : plugin.getDataManager().getAllProtections()) {
            ProtectionBlockType type = plugin.getConfigManager().getProtectionBlockType(data.getProtectionBlockTypeId());
            if (type == null || !type.hasTimeLimit()) continue;
            if (data.isExpired()) {
                expired.add(data);
            }
        }

        for (ProtectionData data : expired) {
            forceExpireProtection(data);
        }
    }

    /**
     * アクションバーに表示するメッセージを生成する
     */
    private String buildActionBarMessage(ProtectionData data, ProtectionBlockType type, Player viewer) {
        long remaining = data.getRemainingSeconds();
        String timeStr = formatTime(remaining);

        // 所有者名
        String ownerName = Bukkit.getOfflinePlayer(data.getOwner()).getName();
        if (ownerName == null) {
            ownerName = plugin.getConfigManager().getMessage("unknown-player");
        }

        boolean isOwner = viewer.getUniqueId().equals(data.getOwner());
        if (isOwner) {
            // 所有者向け: 残り時間を強調
            return plugin.getConfigManager().getMessage("timelimit-actionbar-owner",
                    "{time}", timeStr);
        } else {
            // 訪問者向け: 所有者名と残り時間
            return plugin.getConfigManager().getMessage("timelimit-actionbar-visitor",
                    "{owner}", ownerName, "{time}", timeStr);
        }
    }

    /**
     * インベントリ内の補充アイテムを消費して保護の残り時間を延長する。
     * 保護のオーナーのインベントリを走査して補充アイテムを探す。
     * @param player 補充を行うプレイヤー（所有者である必要あり）
     * @param data   対象の保護データ
     * @return 延長に成功した秒数（0なら補充アイテムなし）
     */
    public long tryRefill(Player player, ProtectionData data) {
        ProtectionBlockType type = plugin.getConfigManager().getProtectionBlockType(data.getProtectionBlockTypeId());
        if (type == null || !type.hasTimeLimit() || type.getRefillItems().isEmpty()) {
            return 0L;
        }

        Map<Material, Long> refillItems = type.getRefillItems();
        String key = data.getOwner().toString() + ":" + data.getProtectionBlockTypeId();
        int currentCount = refillCounts.getOrDefault(key, 0);

        // コスト係数を計算
        // 補充回数が増えるほどコストが増加: 必要アイテム数 = ceil(multiplier^count)
        double multiplier = type.getCostMultiplier();
        int itemsNeeded = (int) Math.ceil(Math.pow(multiplier, currentCount));

        // プレイヤーのインベントリから補充アイテムを探す
        long addedSeconds = 0L;

        for (Map.Entry<Material, Long> refillEntry : refillItems.entrySet()) {
            Material mat = refillEntry.getKey();
            long secondsPerItem = refillEntry.getValue();

            // インベントリ内の数量を取得
            int available = countItems(player, mat);
            if (available <= 0) continue;

            // 消費できる個数（必要個数を上限として）
            int toConsume = Math.min(available, itemsNeeded);
            removeItems(player, mat, toConsume);

            // 延長秒数を計算（消費数 / 必要数 の割合で延長）
            addedSeconds += (long) secondsPerItem * toConsume / itemsNeeded;
            itemsNeeded -= toConsume;

            if (itemsNeeded <= 0) break;
        }

        if (addedSeconds > 0) {
            data.extendExpiry(addedSeconds);
            refillCounts.put(key, currentCount + 1);
            plugin.getDataManager().saveData();

            // 補充成功メッセージ
            int nextCount = currentCount + 1;
            int nextCost = (int) Math.ceil(Math.pow(multiplier, nextCount));
            player.sendMessage(plugin.getConfigManager().getMessage(
                    "timelimit-refill-success",
                    "{time}", formatTime(addedSeconds),
                    "{remaining}", formatTime(data.getRemainingSeconds()),
                    "{next_cost}", String.valueOf(nextCost)));
        }

        return addedSeconds;
    }

    /**
     * 新規保護時に初期有効期限を設定する
     */
    public void initializeExpiry(ProtectionData data) {
        ProtectionBlockType type = plugin.getConfigManager().getProtectionBlockType(data.getProtectionBlockTypeId());
        if (type == null || !type.hasTimeLimit()) return;

        long expiryTime = System.currentTimeMillis() + type.getInitialDurationSeconds() * 1000L;
        data.setExpiryTime(expiryTime);

        // 補充カウントをリセット
        String key = data.getOwner().toString() + ":" + data.getProtectionBlockTypeId();
        refillCounts.put(key, 0);
    }

    /**
     * 期限切れ保護を強制解除する
     */
    private void forceExpireProtection(ProtectionData data) {
        plugin.getLogger().info("Protection expired for " + data.getOwner() + " (type: " + data.getProtectionBlockTypeId() + ")");
        plugin.getProtectionManager().forceRemoveProtection(data.getOwner(), data.getProtectionBlockTypeId());

        // 所有者がオンラインの場合メッセージ送信
        Player owner = Bukkit.getPlayer(data.getOwner());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(plugin.getConfigManager().getMessage("timelimit-expired"));
        }

        // 補充カウントをクリア
        String key = data.getOwner().toString() + ":" + data.getProtectionBlockTypeId();
        refillCounts.remove(key);
    }

    /**
     * 保護を削除した際に補充カウントをリセット
     */
    public void onProtectionRemoved(UUID ownerId, String blockTypeId) {
        String key = ownerId.toString() + ":" + blockTypeId;
        refillCounts.remove(key);
    }

    /**
     * 指定された時間（秒）を読みやすい形式にフォーマットする
     * 例: 3661 -> "1時間1分1秒" (ja) / "1h1m1s" (en)
     */
    private String formatTime(long seconds) {
        if (seconds <= 0) return "0" + plugin.getConfigManager().getMessage("timelimit-unit-second");
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        String unitH = plugin.getConfigManager().getMessage("timelimit-unit-hour");
        String unitM = plugin.getConfigManager().getMessage("timelimit-unit-minute");
        String unitS = plugin.getConfigManager().getMessage("timelimit-unit-second");
        if (h > 0) sb.append(h).append(unitH);
        if (m > 0) sb.append(m).append(unitM);
        if (s > 0 || sb.isEmpty()) sb.append(s).append(unitS);
        return sb.toString();
    }

    /**
     * プレイヤーインベントリ内の指定マテリアルの数量をカウント
     */
    private int countItems(Player player, Material mat) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * プレイヤーインベントリから指定マテリアルを指定数量削除
     */
    private void removeItems(Player player, Material mat, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != mat) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
        player.getInventory().setContents(contents);
    }
}
