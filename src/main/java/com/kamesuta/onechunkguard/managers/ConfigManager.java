package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {
    private final OneChunkGuard plugin;
    private final FileConfiguration config;
    private final Map<String, ProtectionBlockType> protectionBlockTypes = new HashMap<>();

    public ConfigManager(OneChunkGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadProtectionBlockTypes();
    }

    /**
     * 保護ブロックタイプを読み込み
     */
    private void loadProtectionBlockTypes() {
        protectionBlockTypes.clear();
        ConfigurationSection section = config.getConfigurationSection("protection-blocks");
        
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
            
            ProtectionBlockType type = new ProtectionBlockType(typeId, material, displayName, lore, parentRegion, chunkRange);
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

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("messages." + key, "&cMessage not found: " + key));
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
        return config.getInt("protection.min-y", -64);
    }

    public int getMaxY() {
        return config.getInt("protection.max-y", 320);
    }

    public int getMaxTrustedPlayers() {
        return config.getInt("protection.max-trusted-players", 5);
    }

    public String getChatUIHeader() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("chat-ui.header", "&6━━━━━ チャンク保護設定 ━━━━━"));
    }

    public String getChatUIAddMember() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("chat-ui.add-member", "&a[メンバー追加]"));
    }

    public String getChatUIRemoveMember() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("chat-ui.remove-member", "&c[メンバー削除]"));
    }

    public String getChatUIListMembers() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("chat-ui.list-members", "&b[メンバー一覧]"));
    }

    public String getChatUIFooter() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("chat-ui.footer", "&6━━━━━━━━━━━━━━━━━━━"));
    }

    public boolean isShowOwnerActionBar() {
        return config.getBoolean("protection.show-owner-actionbar", true);
    }
}