package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    private final OneChunkGuard plugin;
    private final FileConfiguration config;

    public ConfigManager(OneChunkGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public Material getProtectionBlockMaterial() {
        String materialName = config.getString("protection-block.material", "END_STONE");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName + ", using END_STONE");
            return Material.END_STONE;
        }
    }

    public String getProtectionBlockDisplayName() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("protection-block.display-name", "&6&l保護ブロック"));
    }

    public List<String> getProtectionBlockLore() {
        return config.getStringList("protection-block.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
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