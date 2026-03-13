package com.kamesuta.onechunkguard.utils;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.managers.ConfigManager;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemUtilsTest {

    @Mock
    private OneChunkGuard mockPlugin;
    
    @Mock
    private ConfigManager mockConfigManager;
    
    private ServerMock server;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup MockBukkit
        server = MockBukkit.mock();
        
        // Mock plugin setup
        when(mockPlugin.getConfigManager()).thenReturn(mockConfigManager);
        OneChunkGuard.setInstance(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testCreateProtectionBlockWithDefaultType() {
        // Setup mock protection block type
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of("&7Place this block to", "&7protect a chunk"),
            "", 1, "Default Area", new HashMap<>()
        );
        
        when(mockConfigManager.getDefaultProtectionBlockType()).thenReturn(defaultType);
        
        ItemStack item = ItemUtils.createProtectionBlock();
        
        assertNotNull(item);
        assertEquals(Material.END_STONE, item.getType());
        assertTrue(ItemUtils.isProtectionBlock(item));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(item));
        
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals("&6&lProtection Block", meta.getDisplayName());
        assertNotNull(meta.getLore());
        assertEquals(2, meta.getLore().size());
    }

    @Test
    void testCreateProtectionBlockWithSpecificType() {
        // Setup mock protection block type
        ProtectionBlockType vipType = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "&b&lVIP Protection Block",
            List.of("&7VIP only", "&73x3 chunk protection"),
            "vip_area", 3, "VIP Area", new HashMap<>()
        );
        
        when(mockConfigManager.getProtectionBlockType("vip")).thenReturn(vipType);
        
        ItemStack item = ItemUtils.createProtectionBlock("vip");
        
        assertNotNull(item);
        assertEquals(Material.DIAMOND_BLOCK, item.getType());
        assertTrue(ItemUtils.isProtectionBlock(item));
        assertEquals("vip", ItemUtils.getProtectionBlockTypeId(item));
        
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals("&b&lVIP Protection Block", meta.getDisplayName());
        assertNotNull(meta.getLore());
        assertEquals(2, meta.getLore().size());
    }

    @Test
    void testCreateProtectionBlockWithUnknownType() {
        // Setup mock to return null for unknown type
        when(mockConfigManager.getProtectionBlockType("unknown")).thenReturn(null);
        
        // Should fallback to default
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of("&7Place this block to", "&7protect a chunk"),
            "", 1, "Default Area", new HashMap<>()
        );
        when(mockConfigManager.getDefaultProtectionBlockType()).thenReturn(defaultType);
        
        ItemStack item = ItemUtils.createProtectionBlock("unknown");
        
        assertNotNull(item);
        assertEquals(Material.END_STONE, item.getType());
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(item));
    }

    @Test
    void testCreateProtectionBlockWithNoDefaultType() {
        // Setup mock to return null for default type
        when(mockConfigManager.getDefaultProtectionBlockType()).thenReturn(null);
        
        ItemStack item = ItemUtils.createProtectionBlock();
        
        assertNotNull(item);
        assertEquals(Material.END_STONE, item.getType());
        assertTrue(ItemUtils.isProtectionBlock(item));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(item));
        
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals("§6§lProtection Block", meta.getDisplayName());
        assertNotNull(meta.getLore());
        assertEquals(3, meta.getLore().size());
    }

    @Test
    void testIsProtectionBlock() {
        // Test with protection block
        ItemStack protectionBlock = ItemUtils.createProtectionBlock();
        assertTrue(ItemUtils.isProtectionBlock(protectionBlock));
        
        // Test with regular item
        ItemStack regularItem = new ItemStack(Material.STONE);
        assertFalse(ItemUtils.isProtectionBlock(regularItem));
        
        // Test with null
        assertFalse(ItemUtils.isProtectionBlock(null));
        
        // Test with item without meta
        ItemStack itemWithoutMeta = new ItemStack(Material.STONE);
        itemWithoutMeta.setItemMeta(null);
        assertFalse(ItemUtils.isProtectionBlock(itemWithoutMeta));
    }

    @Test
    void testGetProtectionBlockTypeId() {
        // Test with protection block
        ItemStack protectionBlock = ItemUtils.createProtectionBlock("default");
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(protectionBlock));
        
        // Test with VIP protection block
        ProtectionBlockType vipType = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "&b&lVIP Protection Block",
            List.of(), "", 3, "VIP Area", new HashMap<>()
        );
        when(mockConfigManager.getProtectionBlockType("vip")).thenReturn(vipType);
        
        ItemStack vipBlock = ItemUtils.createProtectionBlock("vip");
        assertEquals("vip", ItemUtils.getProtectionBlockTypeId(vipBlock));
        
        // Test with regular item
        ItemStack regularItem = new ItemStack(Material.STONE);
        assertNull(ItemUtils.getProtectionBlockTypeId(regularItem));
        
        // Test with null
        assertNull(ItemUtils.getProtectionBlockTypeId(null));
    }

    @Test
    void testGetProtectionBlockType() {
        // Setup mock protection block type
        ProtectionBlockType vipType = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "&b&lVIP Protection Block",
            List.of(), "", 3, "VIP Area", new HashMap<>()
        );
        when(mockConfigManager.getProtectionBlockType("vip")).thenReturn(vipType);
        
        ItemStack vipBlock = ItemUtils.createProtectionBlock("vip");
        ProtectionBlockType retrievedType = ItemUtils.getProtectionBlockType(vipBlock);
        
        assertNotNull(retrievedType);
        assertEquals("vip", retrievedType.getId());
        assertEquals(Material.DIAMOND_BLOCK, retrievedType.getMaterial());
        
        // Test with regular item
        ItemStack regularItem = new ItemStack(Material.STONE);
        assertNull(ItemUtils.getProtectionBlockType(regularItem));
        
        // Test with null
        assertNull(ItemUtils.getProtectionBlockType(null));
    }

    @Test
    void testGetAllProtectionBlockTypes() {
        // Setup mock protection block types
        Map<String, ProtectionBlockType> types = new HashMap<>();
        types.put("default", new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of(), "", 1, "Default Area", new HashMap<>()
        ));
        types.put("vip", new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "&b&lVIP Protection Block",
            List.of(), "", 3, "VIP Area", new HashMap<>()
        ));
        
        when(mockConfigManager.getProtectionBlockTypes()).thenReturn(types);
        
        Map<String, ProtectionBlockType> retrievedTypes = ItemUtils.getAllProtectionBlockTypes();
        
        assertNotNull(retrievedTypes);
        assertEquals(2, retrievedTypes.size());
        assertTrue(retrievedTypes.containsKey("default"));
        assertTrue(retrievedTypes.containsKey("vip"));
    }

    @Test
    void testEmergencyProtectionBlock() {
        // Test emergency fallback when no default type is available
        when(mockConfigManager.getDefaultProtectionBlockType()).thenReturn(null);
        
        ItemStack item = ItemUtils.createProtectionBlock();
        
        assertNotNull(item);
        assertEquals(Material.END_STONE, item.getType());
        assertTrue(ItemUtils.isProtectionBlock(item));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(item));
        
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals("§6§lProtection Block", meta.getDisplayName());
        assertNotNull(meta.getLore());
        assertEquals(3, meta.getLore().size());
        assertTrue(meta.getLore().contains("§7Place this block to"));
        assertTrue(meta.getLore().contains("§7protect a chunk"));
        assertTrue(meta.getLore().contains("§cOne chunk per person!"));
    }

    @Test
    void testProtectionBlockWithFlags() {
        // Setup mock protection block type with flags
        Map<String, String> flags = new HashMap<>();
        flags.put("pvp", "deny");
        flags.put("mob-spawning", "deny");
        
        ProtectionBlockType typeWithFlags = new ProtectionBlockType(
            "premium", Material.EMERALD_BLOCK, "&a&lPremium Protection Block",
            List.of("&7Premium features"), "", 5, "Premium Area", flags
        );
        
        when(mockConfigManager.getProtectionBlockType("premium")).thenReturn(typeWithFlags);
        
        ItemStack item = ItemUtils.createProtectionBlock("premium");
        
        assertNotNull(item);
        assertEquals(Material.EMERALD_BLOCK, item.getType());
        assertEquals("premium", ItemUtils.getProtectionBlockTypeId(item));
        
        ProtectionBlockType retrievedType = ItemUtils.getProtectionBlockType(item);
        assertNotNull(retrievedType);
        assertEquals(flags, retrievedType.getFlags());
    }
}