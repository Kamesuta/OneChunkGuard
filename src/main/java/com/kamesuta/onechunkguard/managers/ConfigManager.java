package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {
    private final OneChunkGuard plugin;
    private final FileConfiguration config;
    private final Map<String, ProtectionBlockType> protectionBlockTypes = new HashMap<>();
    private FileConfiguration messagesConfig;
    private FileConfiguration langConfig;
    private String language;

    public ConfigManager(OneChunkGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadLanguage();
        loadLanguageConfig();
        loadMessagesConfig();
        loadProtectionBlockTypes();
    }

    /**
     * 言語別設定ファイルを読み込み
     */
    private void loadLanguageConfig() {
        String fileName = "config_" + language + ".yml";
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        // ファイルが存在しない場合はリソースからコピー
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        langConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // デフォルトをリソースから読み込み
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defConfig);
        }
    }
    
    /**
     * 保護ブロックタイプを読み込み
     */
    private void loadProtectionBlockTypes() {
        protectionBlockTypes.clear();
        ConfigurationSection section = langConfig.getConfigurationSection("protection-blocks");
        
        if (section == null) {
            plugin.getLogger().warning("protection-blocks section not found in config.yml");
            return;
        }
        
        for (String typeId : section.getKeys(false)) {
            ConfigurationSection typeSection = section.getConfigurationSection(typeId);
            if (typeSection == null) continue;
            
            String materialName = typeSection.getString("material", "END_STONE");
            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for " + typeId + ": " + materialName + ", skipping");
                continue;
            }
            
            String displayName = ChatColor.translateAlternateColorCodes('&', 
                typeSection.getString("display-name", "&6&l保護ブロック"));
            List<String> lore = typeSection.getStringList("lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
            String parentRegion = typeSection.getString("parent-region", "");
            int chunkRange = typeSection.getInt("chunk-range", 1);
            String areaName = ChatColor.translateAlternateColorCodes('&', 
                typeSection.getString("area-name", displayName));
            
            ProtectionBlockType type = new ProtectionBlockType(typeId, material, displayName, lore, parentRegion, chunkRange, areaName);
            protectionBlockTypes.put(typeId, type);
        }
        
        if (protectionBlockTypes.isEmpty()) {
            plugin.getLogger().warning("No valid protection block types found in config.yml");
        } else {
            plugin.getLogger().info("Loaded " + protectionBlockTypes.size() + " protection block types");
        }
    }
    
    /**
     * すべての保護ブロックタイプを取得
     */
    public Map<String, ProtectionBlockType> getProtectionBlockTypes() {
        return new HashMap<>(protectionBlockTypes);
    }
    
    /**
     * 指定IDの保護ブロックタイプを取得
     */
    public ProtectionBlockType getProtectionBlockType(String typeId) {
        return protectionBlockTypes.get(typeId);
    }
    
    /**
     * デフォルトの保護ブロックタイプを取得
     */
    public ProtectionBlockType getDefaultProtectionBlockType() {
        ProtectionBlockType defaultType = protectionBlockTypes.get("default");
        if (defaultType != null) {
            return defaultType;
        }
        // デフォルトがない場合は最初のタイプを返す
        return protectionBlockTypes.values().stream().findFirst().orElse(null);
    }
    
    /**
     * 後方互換性のためのメソッド
     */
    @Deprecated
    public Material getProtectionBlockMaterial() {
        ProtectionBlockType defaultType = getDefaultProtectionBlockType();
        return defaultType != null ? defaultType.getMaterial() : Material.END_STONE;
    }
    
    /**
     * 後方互換性のためのメソッド
     */
    @Deprecated
    public String getProtectionBlockDisplayName() {
        ProtectionBlockType defaultType = getDefaultProtectionBlockType();
        return defaultType != null ? defaultType.getDisplayName() : "&6&l保護ブロック";
    }
    
    /**
     * 後方互換性のためのメソッド
     */
    @Deprecated
    public List<String> getProtectionBlockLore() {
        ProtectionBlockType defaultType = getDefaultProtectionBlockType();
        if (defaultType != null) {
            return defaultType.getLore();
        }
        return List.of("&7このブロックを設置すると", "&7チャンクが保護されます", "&c1人1チャンクまで！");
    }

    /**
     * 言語設定を読み込み
     */
    private void loadLanguage() {
        this.language = config.getString("language", "en");
        if (!language.equals("ja") && !language.equals("en")) {
            plugin.getLogger().warning("Invalid language setting: " + language + ". Using 'en' as default.");
            this.language = "en";
        }
    }
    
    /**
     * メッセージ設定ファイルを読み込み
     */
    private void loadMessagesConfig() {
        String fileName = "messages_" + language + ".yml";
        File messagesFile = new File(plugin.getDataFolder(), fileName);
        
        // ファイルが存在しない場合はリソースからコピー
        if (!messagesFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // デフォルトをリソースから読み込み
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defConfig);
        }
    }
    
    /**
     * 言語設定を取得
     */
    public String getLanguage() {
        return language;
    }
    
    public String getMessage(String key) {
        // メッセージファイルから取得
        if (messagesConfig != null) {
            String message = messagesConfig.getString(key);
            if (message != null) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }
        
        // メッセージが見つからない場合
        plugin.getLogger().warning("Message not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', "&cMessage not found: " + key);
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public int getMinY() {
        return langConfig.getInt("protection.min-y", -64);
    }

    public int getMaxY() {
        return langConfig.getInt("protection.max-y", 320);
    }

    public int getMaxTrustedPlayers() {
        return langConfig.getInt("protection.max-trusted-players", 5);
    }

    public String getChatUIHeader() {
        return getMessage("chat-ui.header");
    }

    public String getChatUIAddMember() {
        return getMessage("chat-ui.add-member");
    }

    public String getChatUIRemoveMember() {
        return getMessage("chat-ui.remove-member");
    }

    public String getChatUIListMembers() {
        return getMessage("chat-ui.list-members");
    }

    public String getChatUIFooter() {
        return getMessage("chat-ui.footer");
    }

    public boolean isShowOwnerActionBar() {
        return langConfig.getBoolean("protection.show-owner-actionbar", true);
    }
}