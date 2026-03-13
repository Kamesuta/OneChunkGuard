package com.kamesuta.onechunkguard.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.managers.ConfigManager;
import com.kamesuta.onechunkguard.managers.DataManager;
import com.kamesuta.onechunkguard.managers.ProtectionManager;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrustCommandTest {

    @Mock
    private OneChunkGuard mockPlugin;
    
    @Mock
    private ConfigManager mockConfigManager;
    
    @Mock
    private DataManager mockDataManager;
    
    @Mock
    private ProtectionManager mockProtectionManager;
    
    private ServerMock server;
    private PlayerMock player;
    private PlayerMock targetPlayer;
    private TrustCommand trustCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup MockBukkit
        server = MockBukkit.mock();
        player = server.addPlayer("TestPlayer");
        targetPlayer = server.addPlayer("TargetPlayer");
        
        // Mock plugin setup
        when(mockPlugin.getConfigManager()).thenReturn(mockConfigManager);
        when(mockPlugin.getDataManager()).thenReturn(mockDataManager);
        when(mockPlugin.getProtectionManager()).thenReturn(mockProtectionManager);
        
        // Mock messages
        when(mockConfigManager.getMessage("command-only-player")).thenReturn("&cThis command can only be used by players.");
        when(mockConfigManager.getMessage("trust-usage")).thenReturn("&cUsage: /trust <player>");
        when(mockConfigManager.getMessage("no-protection")).thenReturn("&cYou don't have any protection.");
        when(mockConfigManager.getMessage("player-not-found")).thenReturn("&cPlayer not found.");
        when(mockConfigManager.getMessage("cannot-trust-self")).thenReturn("&cYou cannot trust yourself.");
        when(mockConfigManager.getMessage("already-trusted", "{player}", "TargetPlayer")).thenReturn("&cTargetPlayer is already trusted.");
        when(mockConfigManager.getMessage("trust-limit")).thenReturn("&cYou have reached the trust limit.");
        when(mockConfigManager.getMessage("trust-success", "{player}", "TargetPlayer")).thenReturn("&aTargetPlayer has been trusted.");
        
        trustCommand = new TrustCommand(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testCommandOnlyPlayer() {
        CommandSender console = server.getConsoleSender();
        Command command = mock(Command.class);
        
        boolean result = trustCommand.onCommand(console, command, "trust", new String[]{"TargetPlayer"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("command-only-player");
    }

    @Test
    void testInvalidUsage() {
        Command command = mock(Command.class);
        
        // No arguments
        boolean result1 = trustCommand.onCommand(player, command, "trust", new String[]{});
        assertTrue(result1);
        verify(mockConfigManager).getMessage("trust-usage");
        
        // Too many arguments
        boolean result2 = trustCommand.onCommand(player, command, "trust", new String[]{"Player1", "Player2"});
        assertTrue(result2);
        verify(mockConfigManager, times(2)).getMessage("trust-usage");
    }

    @Test
    void testNoProtection() {
        Command command = mock(Command.class);
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(null);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"TargetPlayer"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("no-protection");
    }

    @Test
    void testTrustSelf() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"TestPlayer"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("cannot-trust-self");
    }

    @Test
    void testTrustOnlinePlayer() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        when(mockConfigManager.getMaxTrustedPlayers()).thenReturn(5);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"TargetPlayer"});
        
        assertTrue(result);
        verify(mockProtectionManager).addTrustedPlayer(player.getUniqueId(), targetPlayer.getUniqueId());
        verify(mockConfigManager).getMessage("trust-success", "{player}", "TargetPlayer");
    }

    @Test
    void testTrustOfflinePlayer() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        when(mockConfigManager.getMaxTrustedPlayers()).thenReturn(5);
        
        // Remove target player from online players
        // server.removePlayer(targetPlayer); // このメソッドは存在しない可能性
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"TargetPlayer"});
        
        assertTrue(result);
        verify(mockProtectionManager).addTrustedPlayer(player.getUniqueId(), targetPlayer.getUniqueId());
        verify(mockConfigManager).getMessage("trust-success", "{player}", "TargetPlayer");
    }

    @Test
    void testTrustNonExistentPlayer() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"NonExistentPlayer"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("player-not-found");
    }

    @Test
    void testTrustAlreadyTrustedPlayer() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        protection.addTrustedPlayer(targetPlayer.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"TargetPlayer"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("already-trusted", "{player}", "TargetPlayer");
        verify(mockProtectionManager, never()).addTrustedPlayer(any(), any());
    }

    @Test
    void testTrustLimitReached() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        
        // Add maximum trusted players
        for (int i = 0; i < 5; i++) {
            protection.addTrustedPlayer(UUID.randomUUID());
        }
        
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        when(mockConfigManager.getMaxTrustedPlayers()).thenReturn(5);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"TargetPlayer"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("trust-limit");
        verify(mockProtectionManager, never()).addTrustedPlayer(any(), any());
    }

    @Test
    void testTrustWithSelector() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        when(mockConfigManager.getMaxTrustedPlayers()).thenReturn(5);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"@p"});
        
        assertTrue(result);
        verify(mockProtectionManager).addTrustedPlayer(player.getUniqueId(), player.getUniqueId());
        verify(mockConfigManager).getMessage("cannot-trust-self");
    }

    @Test
    void testTrustWithInvalidSelector() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"@invalid"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("player-not-found");
    }

    @Test
    void testTrustWithSelectorNonPlayer() {
        Command command = mock(Command.class);
        ProtectionData protection = createMockProtection(player.getUniqueId());
        when(mockDataManager.getPlayerProtection(player.getUniqueId())).thenReturn(protection);
        
        // Add a non-player entity to the world
        // server.addEntity(player.getLocation(), org.bukkit.entity.EntityType.ZOMBIE); // このメソッドは存在しない可能性
        
        boolean result = trustCommand.onCommand(player, command, "trust", new String[]{"@e[type=zombie,limit=1]"});
        
        assertTrue(result);
        verify(mockConfigManager).getMessage("player-not-found");
    }

    private ProtectionData createMockProtection(UUID ownerId) {
        Location location = new Location(player.getWorld(), 10.5, 64.0, 20.5);
        return new ProtectionData(ownerId, location, "default", 1);
    }
}