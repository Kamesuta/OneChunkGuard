package com.kamesuta.onechunkguard.managers;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataManagerTest {

    @Mock
    private OneChunkGuard mockPlugin;
    
    @TempDir
    Path tempDir;
    
    private ServerMock server;
    private WorldMock world;
    private DataManager dataManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup MockBukkit
        server = MockBukkit.mock();
        world = new WorldMock(Material.GRASS_BLOCK, 10, 100, 10);
        server.addWorld(world);
        
        // Mock plugin setup
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(mockPlugin.getLogger()).thenReturn(server.getLogger());
        
        dataManager = new DataManager(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testInitialState() {
        // Test initial state
        assertFalse(dataManager.hasReceivedProtectionBlock(UUID.randomUUID()));
        assertFalse(dataManager.hasProtection(UUID.randomUUID()));
        assertNull(dataManager.getPlayerProtection(UUID.randomUUID()));
        assertNull(dataManager.getChunkProtection("world:0:0"));
    }

    @Test
    void testMarkPlayerReceivedBlock() {
        UUID playerId = UUID.randomUUID();
        
        assertFalse(dataManager.hasReceivedProtectionBlock(playerId));
        
        dataManager.markPlayerReceivedBlock(playerId);
        
        assertTrue(dataManager.hasReceivedProtectionBlock(playerId));
    }

    @Test
    void testAddAndGetProtection() {
        UUID playerId = UUID.randomUUID();
        Location location = new Location(world, 10.5, 64.0, 20.5);
        ProtectionData protection = new ProtectionData(playerId, location, "default", 1);
        
        // Initially no protection
        assertFalse(dataManager.hasProtection(playerId));
        assertFalse(dataManager.hasProtection(playerId, "default"));
        
        // Add protection
        dataManager.addProtection(protection);
        
        // Check protection exists
        assertTrue(dataManager.hasProtection(playerId));
        assertTrue(dataManager.hasProtection(playerId, "default"));
        
        // Get protection
        ProtectionData retrieved = dataManager.getPlayerProtection(playerId);
        assertNotNull(retrieved);
        assertEquals(playerId, retrieved.getOwner());
        assertEquals("default", retrieved.getProtectionBlockTypeId());
        
        // Get protection by type
        ProtectionData retrievedByType = dataManager.getPlayerProtection(playerId, "default");
        assertNotNull(retrievedByType);
        assertEquals(playerId, retrievedByType.getOwner());
        
        // Check chunk protection
        String chunkKey = "world:0:1"; // Chunk coordinates for location (10.5, 20.5)
        ProtectionData chunkProtection = dataManager.getChunkProtection(chunkKey);
        assertNotNull(chunkProtection);
        assertEquals(playerId, chunkProtection.getOwner());
    }

    @Test
    void testMultipleProtectionTypes() {
        UUID playerId = UUID.randomUUID();
        Location location1 = new Location(world, 10.5, 64.0, 20.5);
        Location location2 = new Location(world, 30.5, 64.0, 40.5);
        
        ProtectionData defaultProtection = new ProtectionData(playerId, location1, "default", 1);
        ProtectionData vipProtection = new ProtectionData(playerId, location2, "vip", 3);
        
        // Add both protections
        dataManager.addProtection(defaultProtection);
        dataManager.addProtection(vipProtection);
        
        // Check both exist
        assertTrue(dataManager.hasProtection(playerId, "default"));
        assertTrue(dataManager.hasProtection(playerId, "vip"));
        
        // Get specific protections
        ProtectionData retrievedDefault = dataManager.getPlayerProtection(playerId, "default");
        assertNotNull(retrievedDefault);
        assertEquals("default", retrievedDefault.getProtectionBlockTypeId());
        
        ProtectionData retrievedVip = dataManager.getPlayerProtection(playerId, "vip");
        assertNotNull(retrievedVip);
        assertEquals("vip", retrievedVip.getProtectionBlockTypeId());
        
        // The main getPlayerProtection should return the default one (backward compatibility)
        ProtectionData mainProtection = dataManager.getPlayerProtection(playerId);
        assertNotNull(mainProtection);
        assertEquals("default", mainProtection.getProtectionBlockTypeId());
    }

    @Test
    void testRemoveProtection() {
        UUID playerId = UUID.randomUUID();
        Location location = new Location(world, 10.5, 64.0, 20.5);
        ProtectionData protection = new ProtectionData(playerId, location, "default", 1);
        
        // Add protection
        dataManager.addProtection(protection);
        assertTrue(dataManager.hasProtection(playerId));
        
        // Remove protection
        dataManager.removeProtection(playerId);
        
        // Check protection is removed
        assertFalse(dataManager.hasProtection(playerId));
        assertNull(dataManager.getPlayerProtection(playerId));
        
        // Check chunk protection is also removed
        String chunkKey = "world:0:1";
        assertNull(dataManager.getChunkProtection(chunkKey));
    }

    @Test
    void testRemoveProtectionByType() {
        UUID playerId = UUID.randomUUID();
        Location location1 = new Location(world, 10.5, 64.0, 20.5);
        Location location2 = new Location(world, 30.5, 64.0, 40.5);
        
        ProtectionData defaultProtection = new ProtectionData(playerId, location1, "default", 1);
        ProtectionData vipProtection = new ProtectionData(playerId, location2, "vip", 3);
        
        // Add both protections
        dataManager.addProtection(defaultProtection);
        dataManager.addProtection(vipProtection);
        
        // Remove only VIP protection
        dataManager.removeProtection(playerId, "vip");
        
        // Check VIP is removed but default remains
        assertFalse(dataManager.hasProtection(playerId, "vip"));
        assertTrue(dataManager.hasProtection(playerId, "default"));
        assertTrue(dataManager.hasProtection(playerId)); // Main protection still exists
        
        // Remove default protection
        dataManager.removeProtection(playerId, "default");
        
        // Check all protections are removed
        assertFalse(dataManager.hasProtection(playerId, "default"));
        assertFalse(dataManager.hasProtection(playerId));
    }

    @Test
    void testMultiChunkProtection() {
        UUID playerId = UUID.randomUUID();
        Location location = new Location(world, 10.5, 64.0, 20.5);
        ProtectionData protection = new ProtectionData(playerId, location, "vip", 3);
        
        dataManager.addProtection(protection);
        
        // Check multiple chunks are protected
        assertTrue(dataManager.isChunkProtected("world:-1:-1"));
        assertTrue(dataManager.isChunkProtected("world:0:-1"));
        assertTrue(dataManager.isChunkProtected("world:1:-1"));
        assertTrue(dataManager.isChunkProtected("world:-1:0"));
        assertTrue(dataManager.isChunkProtected("world:0:0"));
        assertTrue(dataManager.isChunkProtected("world:1:0"));
        assertTrue(dataManager.isChunkProtected("world:-1:1"));
        assertTrue(dataManager.isChunkProtected("world:0:1"));
        assertTrue(dataManager.isChunkProtected("world:1:1"));
        
        // Check chunks outside range are not protected
        assertFalse(dataManager.isChunkProtected("world:-2:0"));
        assertFalse(dataManager.isChunkProtected("world:2:0"));
        assertFalse(dataManager.isChunkProtected("world:0:-2"));
        assertFalse(dataManager.isChunkProtected("world:0:2"));
    }

    @Test
    void testTrustedPlayers() {
        UUID ownerId = UUID.randomUUID();
        UUID trustedId = UUID.randomUUID();
        Location location = new Location(world, 10.5, 64.0, 20.5);
        ProtectionData protection = new ProtectionData(ownerId, location, "default", 1);
        
        // Add trusted player
        protection.addTrustedPlayer(trustedId);
        dataManager.addProtection(protection);
        
        // Check trusted player
        ProtectionData retrieved = dataManager.getPlayerProtection(ownerId);
        assertNotNull(retrieved);
        assertTrue(retrieved.isTrusted(trustedId));
        assertTrue(retrieved.isTrusted(ownerId)); // Owner is always trusted
        
        Set<UUID> trustedPlayers = retrieved.getTrustedPlayers();
        assertEquals(1, trustedPlayers.size());
        assertTrue(trustedPlayers.contains(trustedId));
    }

    @Test
    void testDataPersistence() {
        UUID playerId = UUID.randomUUID();
        Location location = new Location(world, 10.5, 64.0, 20.5);
        ProtectionData protection = new ProtectionData(playerId, location, "default", 1);
        
        // Add protection
        dataManager.addProtection(protection);
        dataManager.markPlayerReceivedBlock(playerId);
        
        // Save data
        dataManager.saveData();
        
        // Create new DataManager (simulating plugin reload)
        DataManager newDataManager = new DataManager(mockPlugin);
        
        // Check data is persisted
        assertTrue(newDataManager.hasReceivedProtectionBlock(playerId));
        assertTrue(newDataManager.hasProtection(playerId));
        
        ProtectionData retrieved = newDataManager.getPlayerProtection(playerId);
        assertNotNull(retrieved);
        assertEquals(playerId, retrieved.getOwner());
        assertEquals("default", retrieved.getProtectionBlockTypeId());
    }

    @Test
    void testInvalidDataHandling() {
        // Test with invalid UUID
        UUID playerId = UUID.randomUUID();
        Location location = new Location(world, 10.5, 64.0, 20.5);
        ProtectionData protection = new ProtectionData(playerId, location, "default", 1);
        
        dataManager.addProtection(protection);
        
        // Try to get protection for different player
        UUID differentPlayerId = UUID.randomUUID();
        assertFalse(dataManager.hasProtection(differentPlayerId));
        assertNull(dataManager.getPlayerProtection(differentPlayerId));
    }
}