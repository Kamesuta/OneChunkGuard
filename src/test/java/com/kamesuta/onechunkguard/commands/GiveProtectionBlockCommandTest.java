package com.kamesuta.onechunkguard.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.managers.ConfigManager;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GiveProtectionBlockCommandTest {

    @Mock
    private OneChunkGuard mockPlugin;
    
    @Mock
    private ConfigManager mockConfigManager;
    
    private ServerMock server;
    private PlayerMock admin;
    private PlayerMock targetPlayer;
    private GiveProtectionBlockCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup MockBukkit
        server = MockBukkit.mock();
        admin = server.addPlayer("Admin");
        targetPlayer = server.addPlayer("TargetPlayer");
        
        // Mock plugin setup
        when(mockPlugin.getConfigManager()).thenReturn(mockConfigManager);
        when(mockPlugin.getName()).thenReturn("onechunkguard");
        org.bukkit.plugin.PluginDescriptionFile pdf = mock(org.bukkit.plugin.PluginDescriptionFile.class);
        when(pdf.getFullName()).thenReturn("OneChunkGuard-1.0");
        when(mockPlugin.getDescription()).thenReturn(pdf);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("OneChunkGuard"));
        when(mockPlugin.isEnabled()).thenReturn(true);
        OneChunkGuard.setInstance(mockPlugin);
        
        // Mock messages
        when(mockConfigManager.getMessage("no-permission")).thenReturn("&cYou don't have permission.");
        when(mockConfigManager.getMessage("give-usage")).thenReturn("&cUsage: /giveprotectionblock <player> <type> [amount]");
        when(mockConfigManager.getMessage("no-protection")).thenReturn("&cYou don't have any protection.");
        when(mockConfigManager.getMessage("protection-created")).thenReturn("&aProtection created!");
        when(mockConfigManager.getMessage("protection-removed")).thenReturn("&aProtection removed.");
        when(mockConfigManager.getMessage(anyString())).thenReturn("mocked");
        when(mockConfigManager.getMessage(anyString(), any(String[].class))).thenReturn("mocked");
        when(mockConfigManager.getMessage("selector-no-target")).thenReturn("&cNo target found for selector.");
        when(mockConfigManager.getMessage("selector-not-player")).thenReturn("&cSelector target is not a player.");
        when(mockConfigManager.getMessage("invalid-selector-or-player")).thenReturn("&cInvalid selector or player name.");
        when(mockConfigManager.getMessage("target-player-not-found", "{player}", "NonExistentPlayer")).thenReturn("&cPlayer NonExistentPlayer not found.");
        when(mockConfigManager.getMessage("protection-type-not-found", "{type}", "invalid")).thenReturn("&cProtection type 'invalid' not found.");
        when(mockConfigManager.getMessage("available-types", "{types}", "default, vip")).thenReturn("&7Available types: default, vip");
        when(mockConfigManager.getMessage("invalid-amount-range")).thenReturn("&cAmount must be between 1 and 64.");
        when(mockConfigManager.getMessage("invalid-amount-format")).thenReturn("&cInvalid amount format.");
        when(mockConfigManager.getMessage("give-success-admin", "{player}", "TargetPlayer", "{block}", "&6&lProtection Block", "{amount}", "1")).thenReturn("&aGave 1 &6&lProtection Block to TargetPlayer");
        when(mockConfigManager.getMessage("give-success-player", "{block}", "&6&lProtection Block", "{amount}", "1")).thenReturn("&aYou received 1 &6&lProtection Block");
        
        command = new GiveProtectionBlockCommand(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testNoPermission() {
        Command cmd = mock(Command.class);
        PlayerMock console = server.addPlayer(); // Player without admin permission
        
        boolean result = command.onCommand(console, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("no-permission");
    }

    @Test
    void testInvalidUsage() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // No arguments
        boolean result1 = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{});
        assertTrue(result1);
        verify(mockConfigManager).getMessage("give-usage");
        
        // Only one argument
        boolean result2 = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer"});
        assertTrue(result2);
        verify(mockConfigManager, times(2)).getMessage("give-usage");
    }

    @Test
    void testGiveToOnlinePlayer() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Setup mock protection block type
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of("&7Place this block to", "&7protect a chunk"),
            "", 1, "Default Area", new HashMap<>(), 0L, 2.0, new HashMap<>()
        );
        when(mockConfigManager.getProtectionBlockType("default")).thenReturn(defaultType);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default"});
        
        assertTrue(result);
        
        // Check that target player received the protection block
        ItemStack item = targetPlayer.getInventory().getItem(0);
        assertNotNull(item);
        assertTrue(ItemUtils.isProtectionBlock(item));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(item));
        
        verify(mockConfigManager).getMessage("give-success-admin", "{player}", "TargetPlayer", "{block}", "&6&lProtection Block", "{amount}", "1");
    }

    @Test
    void testGiveWithAmount() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Setup mock protection block type
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of(), "", 1, "Default Area", new HashMap<>(), 0L, 2.0, new HashMap<>()
        );
        when(mockConfigManager.getProtectionBlockType("default")).thenReturn(defaultType);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default", "5"});
        
        assertTrue(result);
        
        // Check that target player received 5 protection blocks
        int protectionBlockCount = 0;
        for (int i = 0; i < targetPlayer.getInventory().getSize(); i++) {
            ItemStack item = targetPlayer.getInventory().getItem(i);
            if (item != null && ItemUtils.isProtectionBlock(item)) {
                protectionBlockCount += item.getAmount();
            }
        }
        assertEquals(5, protectionBlockCount);
    }

    @Test
    void testGiveWithInvalidAmount() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Test negative amount
        boolean result1 = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default", "-1"});
        assertTrue(result1);
        verify(mockConfigManager).getMessage("invalid-amount-range");
        
        // Test zero amount
        boolean result2 = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default", "0"});
        assertTrue(result2);
        verify(mockConfigManager, times(2)).getMessage("invalid-amount-range");
        
        // Test amount over 64
        boolean result3 = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default", "65"});
        assertTrue(result3);
        verify(mockConfigManager, times(3)).getMessage("invalid-amount-range");
        
        // Test invalid format
        boolean result4 = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default", "abc"});
        assertTrue(result4);
        verify(mockConfigManager).getMessage("invalid-amount-format");
    }

    @Test
    void testGiveToNonExistentPlayer() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"NonExistentPlayer", "default"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("target-player-not-found", "{player}", "NonExistentPlayer");
    }

    @Test
    void testGiveInvalidProtectionType() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        when(mockConfigManager.getProtectionBlockType("invalid")).thenReturn(null);
        
        Map<String, ProtectionBlockType> types = new HashMap<>();
        types.put("default", new ProtectionBlockType("default", Material.END_STONE, "Default", List.of(), "", 1, "Default", new HashMap<>(), 0L, 2.0, new HashMap<>()));
        types.put("vip", new ProtectionBlockType("vip", Material.DIAMOND_BLOCK, "VIP", List.of(), "", 3, "VIP", new HashMap<>(), 0L, 2.0, new HashMap<>()));
        when(mockConfigManager.getProtectionBlockTypes()).thenReturn(types);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "invalid"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("protection-type-not-found", "{type}", "invalid");
        verify(mockConfigManager).getMessage("available-types", "{types}", "default, vip");
    }

    @Test
    @org.junit.jupiter.api.Disabled("MockBukkit 1.21 doesn't support entity selectors")
    void testGiveWithSelector() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Setup mock protection block type
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of(), "", 1, "Default Area", new HashMap<>(), 0L, 2.0, new HashMap<>()
        );
        when(mockConfigManager.getProtectionBlockType("default")).thenReturn(defaultType);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"@p", "default"});
        
        assertTrue(result);
        
        // Check that admin received the protection block (since @p targets the command sender)
        ItemStack item = admin.getInventory().getItem(0);
        assertNotNull(item);
        assertTrue(ItemUtils.isProtectionBlock(item));
        assertEquals("default", ItemUtils.getProtectionBlockTypeId(item));
    }

    @Test
    @org.junit.jupiter.api.Disabled("MockBukkit 1.21 doesn't support entity selectors")
    void testGiveWithInvalidSelector() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"@invalid", "default"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("invalid-selector-or-player");
    }

    @Test
    @org.junit.jupiter.api.Disabled("MockBukkit 1.21 doesn't support entity selectors")
    void testGiveWithSelectorNonPlayer() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Add a non-player entity to the world
        // server.addEntity(admin.getLocation(), org.bukkit.entity.EntityType.ZOMBIE); // このメソッドは存在しない可能性
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"@e[type=zombie,limit=1]", "default"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("selector-not-player");
    }

    @Test
    void testGiveToFullInventory() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Fill target player's inventory
        for (int i = 0; i < targetPlayer.getInventory().getSize(); i++) {
            targetPlayer.getInventory().setItem(i, new ItemStack(Material.STONE));
        }
        
        // Setup mock protection block type
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&lProtection Block",
            List.of(), "", 1, "Default Area", new HashMap<>(), 0L, 2.0, new HashMap<>()
        );
        when(mockConfigManager.getProtectionBlockType("default")).thenReturn(defaultType);
        
        boolean result = command.onCommand(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default"});
        
        assertTrue(result);
        
        // Check that protection block was not added to inventory
        assertFalse(targetPlayer.getInventory().contains(Material.END_STONE));
    }

    @Test
    void testTabCompletion() {
        Command cmd = mock(Command.class);
        admin.addAttachment(mockPlugin, "onechunkguard.admin", true);
        
        // Test player name completion
        List<String> completions1 = command.onTabComplete(admin, cmd, "giveprotectionblock", new String[]{"Ta"});
        assertTrue(completions1.contains("TargetPlayer"));
        
        // Test protection type completion
        Map<String, ProtectionBlockType> types = new HashMap<>();
        types.put("default", new ProtectionBlockType("default", Material.END_STONE, "Default", List.of(), "", 1, "Default", new HashMap<>(), 0L, 2.0, new HashMap<>()));
        types.put("vip", new ProtectionBlockType("vip", Material.DIAMOND_BLOCK, "VIP", List.of(), "", 3, "VIP", new HashMap<>(), 0L, 2.0, new HashMap<>()));
        when(mockConfigManager.getProtectionBlockTypes()).thenReturn(types);
        
        List<String> completions2 = command.onTabComplete(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "d"});
        assertTrue(completions2.contains("default"));
        
        // Test amount completion
        List<String> completions3 = command.onTabComplete(admin, cmd, "giveprotectionblock", new String[]{"TargetPlayer", "default", ""});
        assertTrue(completions3.contains("1"));
        assertTrue(completions3.contains("5"));
        assertTrue(completions3.contains("10"));
    }

    @Test
    void testTabCompletionNoPermission() {
        Command cmd = mock(Command.class);
        
        List<String> completions = command.onTabComplete(admin, cmd, "giveprotectionblock", new String[]{"Ta"});
        assertTrue(completions.isEmpty());
    }
}
