package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {
    private final OneChunkGuard plugin;
    private final File dataFile;
    // プレイヤーUUID -> 保護データ（後方互換性のため残す）
    private final Map<UUID, ProtectionData> playerProtections = new HashMap<>();
    // プレイヤーUUID+種類 -> 保護データ
    private final Map<String, ProtectionData> playerTypeProtections = new HashMap<>();
    // チャンクキー -> 保護データ
    private final Map<String, ProtectionData> chunkProtections = new HashMap<>();
    // 初回保護ブロックを受け取ったプレイヤー
    private final Set<UUID> playersWithBlock = new HashSet<>();
    private FileConfiguration dataConfig;

    public DataManager(OneChunkGuard plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            saveData();
            return;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // ブロックを受け取ったプレイヤーを読み込み
        if (dataConfig.contains("players-with-block")) {
            for (String uuidStr : dataConfig.getStringList("players-with-block")) {
                try {
                    playersWithBlock.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in players-with-block: " + uuidStr);
                }
            }
        }

        // 保護データを読み込み
        ConfigurationSection protectionsSection = dataConfig.getConfigurationSection("protections");
        if (protectionsSection != null) {
            for (String uuidStr : protectionsSection.getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(uuidStr);
                    ConfigurationSection protection = protectionsSection.getConfigurationSection(uuidStr);

                    String worldName = protection.getString("world");
                    double x = protection.getDouble("x");
                    double y = protection.getDouble("y");
                    double z = protection.getDouble("z");

                    if (worldName != null && Bukkit.getWorld(worldName) != null) {
                        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);

                        Set<UUID> trusted = new HashSet<>();
                        if (protection.contains("trusted")) {
                            for (String trustedUuid : protection.getStringList("trusted")) {
                                try {
                                    trusted.add(UUID.fromString(trustedUuid));
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("Invalid trusted player UUID: " + trustedUuid);
                                }
                            }
                        }

                        // 保護ブロック種類を取得（デフォルトは "default"）
                        String blockTypeId = protection.getString("block-type", "default");
                        int chunkRange = protection.getInt("chunk-range", 1);
                        
                        ProtectionData data = new ProtectionData(owner, loc, blockTypeId, chunkRange, trusted);
                        playerProtections.put(owner, data);
                        playerTypeProtections.put(owner.toString() + ":" + blockTypeId, data);
                        
                        // 保護範囲内のすべてのチャンクキーを登録
                        for (String chunkKey : data.getProtectedChunkKeys()) {
                            chunkProtections.put(chunkKey, data);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load protection data: " + uuidStr + ": " + e.getMessage());
                }
            }
        }
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();

        // ブロックを受け取ったプレイヤーを保存
        dataConfig.set("players-with-block", playersWithBlock.stream()
                .map(UUID::toString)
                .toList());

        // 保護データを保存
        ConfigurationSection protectionsSection = dataConfig.createSection("protections");
        for (Map.Entry<UUID, ProtectionData> entry : playerProtections.entrySet()) {
            ProtectionData data = entry.getValue();
            ConfigurationSection protection = protectionsSection.createSection(entry.getKey().toString());

            protection.set("world", data.getWorldName());
            protection.set("x", data.getProtectionBlockLocation().getX());
            protection.set("y", data.getProtectionBlockLocation().getY());
            protection.set("z", data.getProtectionBlockLocation().getZ());
            protection.set("chunkX", data.getChunkX());
            protection.set("chunkZ", data.getChunkZ());
            
            // 新しい情報も保存
            protection.set("block-type", data.getProtectionBlockTypeId());
            protection.set("chunk-range", data.getChunkRange());

            if (!data.getTrustedPlayers().isEmpty()) {
                protection.set("trusted", data.getTrustedPlayers().stream()
                        .map(UUID::toString)
                        .toList());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data: " + e.getMessage());
        }
    }

    public void saveAll() {
        saveData();
    }

    public boolean hasReceivedProtectionBlock(UUID playerId) {
        return playersWithBlock.contains(playerId);
    }

    public void markPlayerReceivedBlock(UUID playerId) {
        playersWithBlock.add(playerId);
        saveData();
    }

    public ProtectionData getPlayerProtection(UUID playerId) {
        return playerProtections.get(playerId);
    }

    /**
     * 指定した種類のプレイヤー保護を取得
     */
    public ProtectionData getPlayerProtection(UUID playerId, String blockTypeId) {
        String key = playerId.toString() + ":" + blockTypeId;
        return playerTypeProtections.get(key);
    }

    public ProtectionData getChunkProtection(String chunkKey) {
        return chunkProtections.get(chunkKey);
    }

    public void addProtection(ProtectionData data) {
        UUID playerId = data.getOwner();
        String blockTypeId = data.getProtectionBlockTypeId();
        
        playerProtections.put(playerId, data);
        playerTypeProtections.put(playerId.toString() + ":" + blockTypeId, data);
        
        // 保護範囲内のすべてのチャンクキーを登録
        for (String chunkKey : data.getProtectedChunkKeys()) {
            chunkProtections.put(chunkKey, data);
        }
        
        saveData();
    }

    public void removeProtection(UUID playerId) {
        ProtectionData data = playerProtections.remove(playerId);
        if (data != null) {
            String blockTypeId = data.getProtectionBlockTypeId();
            playerTypeProtections.remove(playerId.toString() + ":" + blockTypeId);
            
            // 保護範囲内のすべてのチャンクキーを削除
            for (String chunkKey : data.getProtectedChunkKeys()) {
                chunkProtections.remove(chunkKey);
            }
            
            saveData();
        }
    }

    /**
     * 指定した種類のプレイヤー保護を削除
     */
    public void removeProtection(UUID playerId, String blockTypeId) {
        String key = playerId.toString() + ":" + blockTypeId;
        ProtectionData data = playerTypeProtections.remove(key);
        
        if (data != null) {
            // メイン保護マップからも削除（後方互換性）
            if ("default".equals(blockTypeId)) {
                playerProtections.remove(playerId);
            }
            
            // 保護範囲内のすべてのチャンクキーを削除
            for (String chunkKey : data.getProtectedChunkKeys()) {
                chunkProtections.remove(chunkKey);
            }
            
            saveData();
        }
    }

    public boolean hasProtection(UUID playerId) {
        return playerProtections.containsKey(playerId);
    }
    
    /**
     * 指定した種類の保護を持っているかチェック
     */
    public boolean hasProtection(UUID playerId, String blockTypeId) {
        String key = playerId.toString() + ":" + blockTypeId;
        return playerTypeProtections.containsKey(key);
    }

    public boolean isChunkProtected(String chunkKey) {
        return chunkProtections.containsKey(chunkKey);
    }
}