package com.kamesuta.onechunkguard.utils;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.managers.ConfigManager;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryUtilsTest {

    private void assertEmpty(ItemStack item) {
        assertTrue(item == null || item.getType().isAir(), "Expected empty item, but was: " + item);
    }

    @Mock
    private OneChunkGuard mockPlugin;
    
    @Mock
    private ConfigManager mockConfigManager;
    
    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup MockBukkit
        server = MockBukkit.mock();
        player = server.addPlayer("TestPlayer");
        
        // Mock plugin setup
        when(mockPlugin.getConfigManager()).thenReturn(mockConfigManager);
        when(mockPlugin.getName()).thenReturn("onechunkguard");
        org.bukkit.plugin.PluginDescriptionFile pdf = mock(org.bukkit.plugin.PluginDescriptionFile.class);
        when(pdf.getFullName()).thenReturn("OneChunkGuard-1.0");
        when(mockPlugin.getDescription()).thenReturn(pdf);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("OneChunkGuard"));
        OneChunkGuard.setInstance(mockPlugin);
        
        // Setup mock protection block types
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of("&7Place this block to", "&7protect a chunk"),
            "", 1, "Default Area", new HashMap<>(), 0L, 2.0, new HashMap<>()
        );
        
        ProtectionBlockType vipType = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "&b&lVIP Protection Block",
            List.of("&7VIP only", "&73x3 chunk protection"),
            "vip_area", 3, "VIP Area", new HashMap<>(), 0L, 2.0, new HashMap<>()
        );
        
        when(mockConfigManager.getDefaultProtectionBlockType()).thenReturn(defaultType);
        when(mockConfigManager.getProtectionBlockType("default")).thenReturn(defaultType);
        when(mockConfigManager.getProtectionBlockType("vip")).thenReturn(vipType);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testRemoveAllProtectionBlocks() {
        // Add various items to inventory
        ItemStack protectionBlock = ItemUtils.createProtectionBlock("default");
        ItemStack vipBlock = ItemUtils.createProtectionBlock("vip");
        ItemStack regularItem = new ItemStack(Material.STONE);
        
        player.getInventory().setItem(0, protectionBlock);
        player.getInventory().setItem(1, vipBlock);
        player.getInventory().setItem(2, regularItem);
        player.setItemOnCursor(protectionBlock);
        
        // Remove all protection blocks
        InventoryUtils.removeAllProtectionBlocks(player);
        
        // Check that protection blocks are removed
        assertEmpty(player.getInventory().getItem(0));
        assertEmpty(player.getInventory().getItem(1));
        assertEquals(regularItem, player.getInventory().getItem(2)); // Regular item should remain
        assertEmpty(player.getItemOnCursor());
    }

    @Test
    void testRemoveDefaultProtectionBlocks() {
        // Add various items to inventory
        ItemStack defaultBlock = ItemUtils.createProtectionBlock("default");
        ItemStack vipBlock = ItemUtils.createProtectionBlock("vip");
        ItemStack regularItem = new ItemStack(Material.STONE);
        
        player.getInventory().setItem(0, defaultBlock);
        player.getInventory().setItem(1, vipBlock);
        player.getInventory().setItem(2, regularItem);
        player.setItemOnCursor(defaultBlock);
        
        // Remove only default protection blocks
        InventoryUtils.removeDefaultProtectionBlocks(player);
        
        // Check that only default blocks are removed
        assertEmpty(player.getInventory().getItem(0)); // Default block removed
        assertEquals(vipBlock, player.getInventory().getItem(1)); // VIP block remains
        assertEquals(regularItem, player.getInventory().getItem(2)); // Regular item remains
        assertEmpty(player.getItemOnCursor()); // Default block on cursor removed
    }

    @Test
    void testGiveProtectionBlockDefault() {
        // Clear inventory
        player.getInventory().clear();
        
        // Give default protection block
        InventoryUtils.giveProtectionBlock(player);
        
        // Check that default block is in slot 8 (hotbar slot 9)
        ItemStack itemInSlot8 = player.getInventory().getItem(8);
        assertNotNull(itemInSlot8);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot8));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(itemInSlot8));
        assertEquals(Material.END_STONE, itemInSlot8.getType());
    }

    @Test
    void testGiveProtectionBlockDefaultWithExistingItem() {
        // Add item to slot 8
        ItemStack existingItem = new ItemStack(Material.DIRT);
        player.getInventory().setItem(8, existingItem);
        
        // Give default protection block
        InventoryUtils.giveProtectionBlock(player);
        
        // Check that default block is in slot 8
        ItemStack itemInSlot8 = player.getInventory().getItem(8);
        assertNotNull(itemInSlot8);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot8));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(itemInSlot8));
        
        // Check that existing item was dropped
        assertTrue(player.getWorld().getEntities().stream()
            .anyMatch(entity -> entity instanceof org.bukkit.entity.Item));
    }

    @Test
    void testGiveProtectionBlockVip() {
        // Clear inventory
        player.getInventory().clear();
        
        // Give VIP protection block
        InventoryUtils.giveProtectionBlock(player, "vip");
        
        // Check that VIP block is in inventory (not slot 8)
        ItemStack itemInSlot0 = player.getInventory().getItem(0);
        assertNotNull(itemInSlot0);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot0));
        assertEquals("vip", ItemUtils.getProtectionBlockTypeId(itemInSlot0));
        assertEquals(Material.DIAMOND_BLOCK, itemInSlot0.getType());
        
        // Slot 8 should be empty
        assertEmpty(player.getInventory().getItem(8));
    }

    @Test
    void testGiveProtectionBlockVipWithFullInventory() {
        // Fill inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            player.getInventory().setItem(i, new ItemStack(Material.STONE, 64));
        }
        
        // Give VIP protection block
        InventoryUtils.giveProtectionBlock(player, "vip");
        
        // Check that VIP block was not added to inventory
        assertFalse(player.getInventory().contains(Material.DIAMOND_BLOCK));
        
        // Check that inventory is still full
        for (int i = 0; i < 36; i++) {
            assertNotNull(player.getInventory().getItem(i));
        }
    }

    @Test
    void testGiveProtectionBlockDefaultReplacesExistingDefault() {
        // Add existing default protection block
        ItemStack existingDefault = ItemUtils.createProtectionBlock("default");
        player.getInventory().setItem(0, existingDefault);
        player.getInventory().setItem(5, existingDefault);
        player.setItemOnCursor(existingDefault);
        
        // Give new default protection block
        InventoryUtils.giveProtectionBlock(player);
        
        // Check that only one default block exists in slot 8
        ItemStack itemInSlot8 = player.getInventory().getItem(8);
        assertNotNull(itemInSlot8);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot8));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(itemInSlot8));
        
        // Check that other default blocks were removed
        assertEmpty(player.getInventory().getItem(0));
        assertEmpty(player.getInventory().getItem(5));
        assertEmpty(player.getItemOnCursor());
    }

    @Test
    void testGiveProtectionBlockWithNullTypeId() {
        // Clear inventory
        player.getInventory().clear();
        
        // Give protection block with null type ID (should default to default)
        InventoryUtils.giveProtectionBlock(player, null);
        
        // Check that default block is in slot 8
        ItemStack itemInSlot8 = player.getInventory().getItem(8);
        assertNotNull(itemInSlot8);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot8));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(itemInSlot8));
    }

    @Test
    void testGiveProtectionBlockDefaultWithMixedInventory() {
        // Add mixed items to inventory
        ItemStack defaultBlock = ItemUtils.createProtectionBlock("default");
        ItemStack vipBlock = ItemUtils.createProtectionBlock("vip");
        ItemStack regularItem = new ItemStack(Material.STONE);
        
        player.getInventory().setItem(0, defaultBlock);
        player.getInventory().setItem(1, vipBlock);
        player.getInventory().setItem(2, regularItem);
        player.getInventory().setItem(8, new ItemStack(Material.DIRT));
        
        // Give default protection block
        InventoryUtils.giveProtectionBlock(player);
        
        // Check that default block is in slot 8
        ItemStack itemInSlot8 = player.getInventory().getItem(8);
        assertNotNull(itemInSlot8);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot8));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(itemInSlot8));
        
        // Check that other default blocks were removed
        assertEmpty(player.getInventory().getItem(0)); // Default block removed
        
        // Check that VIP block and regular item remain
        assertEquals(vipBlock, player.getInventory().getItem(1));
        assertEquals(regularItem, player.getInventory().getItem(2));
    }

    @Test
    void testGiveProtectionBlockVipWithExistingDefault() {
        // Add default protection block
        ItemStack defaultBlock = ItemUtils.createProtectionBlock("default");
        player.getInventory().setItem(8, defaultBlock);
        
        // Give VIP protection block
        InventoryUtils.giveProtectionBlock(player, "vip");
        
        // Check that VIP block is in inventory
        ItemStack itemInSlot0 = player.getInventory().getItem(0);
        assertNotNull(itemInSlot0);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot0));
        assertEquals("vip", ItemUtils.getProtectionBlockTypeId(itemInSlot0));
        
        // Check that default block remains in slot 8
        ItemStack itemInSlot8 = player.getInventory().getItem(8);
        assertNotNull(itemInSlot8);
        assertTrue(ItemUtils.isProtectionBlock(itemInSlot8));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(itemInSlot8));
    }
}
