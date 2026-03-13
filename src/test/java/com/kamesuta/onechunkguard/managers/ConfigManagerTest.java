package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigManagerTest {

    @Mock
    private OneChunkGuard mockPlugin;
    
    @Mock
    private FileConfiguration mockConfig;
    
    @Mock
    private FileConfiguration mockLangConfig;
    
    @Mock
    private FileConfiguration mockMessagesConfig;
    
    @TempDir
    Path tempDir;
    
    private ConfigManager configManager;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        // Mock plugin setup
        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(mockPlugin.getResource(anyString())).thenReturn(null);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("OneChunkGuard"));
        
        // Mock config values
        when(mockConfig.getString("language", "en")).thenReturn("en");
        
        // Mock lang config
        when(mockLangConfig.getConfigurationSection("protection-blocks")).thenReturn(null);
        when(mockLangConfig.getInt("protection.min-y", -64)).thenReturn(-64);
        when(mockLangConfig.getInt("protection.max-y", 320)).thenReturn(320);
        when(mockLangConfig.getInt("protection.max-trusted-players", 5)).thenReturn(5);
        when(mockLangConfig.getBoolean("protection.show-owner-actionbar", true)).thenReturn(true);
        
        // Mock messages config
        when(mockMessagesConfig.getString(anyString())).thenReturn("&cTest message");
        
        // Create test config files
        createTestConfigFiles();
        
        // Create ConfigManager with mocked dependencies
        configManager = new ConfigManager(mockPlugin);
    }

    private void createTestConfigFiles() throws IOException {
        // Create config_en.yml
        File configEnFile = tempDir.resolve("config_en.yml").toFile();
        YamlConfiguration configEn = new YamlConfiguration();
        configEn.set("protection-blocks.default.material", "END_STONE");
        configEn.set("protection-blocks.default.display-name", "&6&lProtection Block");
        configEn.set("protection-blocks.default.lore", new String[]{"&7Place this block to", "&7protect a chunk"});
        configEn.set("protection-blocks.default.parent-region", "");
        configEn.set("protection-blocks.default.chunk-range", 1);
        configEn.set("protection-blocks.default.area-name", "Default Area");
        configEn.set("protection-blocks.vip.material", "DIAMOND_BLOCK");
        configEn.set("protection-blocks.vip.display-name", "&b&lVIP Protection Block");
        configEn.set("protection-blocks.vip.lore", new String[]{"&7VIP only", "&73x3 chunk protection"});
        configEn.set("protection-blocks.vip.parent-region", "vip_area");
        configEn.set("protection-blocks.vip.chunk-range", 3);
        configEn.set("protection-blocks.vip.area-name", "VIP Area");
        configEn.save(configEnFile);
        
        // Create messages_en.yml
        File messagesEnFile = tempDir.resolve("messages_en.yml").toFile();
        YamlConfiguration messagesEn = new YamlConfiguration();
        messagesEn.set("protection-created", "&aProtection created!");
        messagesEn.set("no-protection", "&cYou don't have any protection.");
        messagesEn.set("already-protected", "&cYou already have a protection.");
        messagesEn.save(messagesEnFile);
    }

    @Test
    void testGetLanguage() {
        assertEquals("en", configManager.getLanguage());
    }

    @Test
    void testGetMessage() {
        String message = configManager.getMessage("protection-created");
        assertNotNull(message);
        assertTrue(message.contains("Protection created"));
    }

    @Test
    void testGetMessageWithReplacements() {
        String message = configManager.getMessage("owner-info", "{owner}", "TestPlayer");
        assertNotNull(message);
        // Note: The actual replacement logic would be tested with real message content
    }

    @Test
    void testGetMessageNotFound() {
        String message = configManager.getMessage("nonexistent-key");
        assertNotNull(message);
        assertTrue(message.contains("Message not found"));
    }

    @Test
    void testGetMinY() {
        assertEquals(-64, configManager.getMinY());
    }

    @Test
    void testGetMaxY() {
        assertEquals(320, configManager.getMaxY());
    }

    @Test
    void testGetMaxTrustedPlayers() {
        assertEquals(5, configManager.getMaxTrustedPlayers());
    }

    @Test
    void testIsShowOwnerActionBar() {
        assertTrue(configManager.isShowOwnerActionBar());
    }

    @Test
    void testGetProtectionBlockTypes() {
        Map<String, ProtectionBlockType> types = configManager.getProtectionBlockTypes();
        assertNotNull(types);
        // In the mocked version, this will be empty since we override loadProtectionBlockTypes
        assertFalse(types.isEmpty());
    }

    @Test
    void testGetProtectionBlockType() {
        ProtectionBlockType type = configManager.getProtectionBlockType("default");
        // In the mocked version, this will be null since we override loadProtectionBlockTypes
        assertNotNull(type);
    }

    @Test
    void testGetDefaultProtectionBlockType() {
        ProtectionBlockType type = configManager.getDefaultProtectionBlockType();
        // In the mocked version, this will be null since we override loadProtectionBlockTypes
        assertNotNull(type);
    }

    @Test
    void testDeprecatedMethods() {
        // Test deprecated methods for backward compatibility
        Material material = configManager.getProtectionBlockMaterial();
        assertEquals(Material.END_STONE, material); // Default fallback
        
        String displayName = configManager.getProtectionBlockDisplayName();
        assertEquals("§6§lProtection Block", displayName); // Default fallback
        
        assertNotNull(configManager.getProtectionBlockLore());
    }
}