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
    // プレイヤーUUID -> 保護データ
    private final Map<UUID, ProtectionData> playerProtections = new HashMap<>();
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
                    plugin.getLogger().warning("players-with-blockに無効なUUID: " + uuidStr);
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
                                    plugin.getLogger().warning("無効な信頼プレイヤーUUID: " + trustedUuid);
                                }
                            }
                        }

                        ProtectionData data = new ProtectionData(owner, loc, trusted);
                        playerProtections.put(owner, data);
                        chunkProtections.put(data.getChunkKey(), data);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("保護データの読み込みに失敗: " + uuidStr + ": " + e.getMessage());
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

            if (!data.getTrustedPlayers().isEmpty()) {
                protection.set("trusted", data.getTrustedPlayers().stream()
                        .map(UUID::toString)
                        .toList());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("データ保存に失敗: " + e.getMessage());
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

    public ProtectionData getChunkProtection(String chunkKey) {
        return chunkProtections.get(chunkKey);
    }

    public void addProtection(ProtectionData data) {
        playerProtections.put(data.getOwner(), data);
        chunkProtections.put(data.getChunkKey(), data);
        saveData();
    }

    public void removeProtection(UUID playerId) {
        ProtectionData data = playerProtections.remove(playerId);
        if (data != null) {
            chunkProtections.remove(data.getChunkKey());
            saveData();
        }
    }

    public boolean hasProtection(UUID playerId) {
        return playerProtections.containsKey(playerId);
    }

    public boolean isChunkProtected(String chunkKey) {
        return chunkProtections.containsKey(chunkKey);
    }
}