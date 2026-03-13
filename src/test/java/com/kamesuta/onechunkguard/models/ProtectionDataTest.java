package com.kamesuta.onechunkguard.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtectionDataTest {

    @Mock
    private World mockWorld;
    
    @Mock
    private Chunk mockChunk;
    
    @Mock
    private Location mockLocation;
    
    private UUID ownerId;
    private UUID trustedPlayerId;
    private UUID otherPlayerId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        ownerId = UUID.randomUUID();
        trustedPlayerId = UUID.randomUUID();
        otherPlayerId = UUID.randomUUID();
        
        // Mock setup
        when(mockWorld.getName()).thenReturn("test_world");
        when(mockChunk.getWorld()).thenReturn(mockWorld);
        when(mockChunk.getX()).thenReturn(10);
        when(mockChunk.getZ()).thenReturn(20);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockLocation.getWorld()).thenReturn(mockWorld);
    }

    @Test
    void testConstructorWithDefaultValues() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        
        assertEquals(ownerId, protectionData.getOwner());
        assertEquals(mockLocation, protectionData.getProtectionBlockLocation());
        assertEquals(10, protectionData.getChunkX());
        assertEquals(20, protectionData.getChunkZ());
        assertEquals("test_world", protectionData.getWorldName());
        assertEquals("default", protectionData.getProtectionBlockTypeId());
        assertEquals(1, protectionData.getChunkRange());
        assertTrue(protectionData.getTrustedPlayers().isEmpty());
    }

    @Test
    void testConstructorWithCustomValues() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "vip", 3);
        
        assertEquals(ownerId, protectionData.getOwner());
        assertEquals("vip", protectionData.getProtectionBlockTypeId());
        assertEquals(3, protectionData.getChunkRange());
    }

    @Test
    void testConstructorWithTrustedPlayers() {
        Set<UUID> trustedPlayers = new HashSet<>();
        trustedPlayers.add(trustedPlayerId);
        
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, trustedPlayers);
        
        assertTrue(protectionData.getTrustedPlayers().contains(trustedPlayerId));
        assertEquals(1, protectionData.getTrustedPlayers().size());
    }

    @Test
    void testConstructorWithCustomTypeAndTrustedPlayers() {
        Set<UUID> trustedPlayers = new HashSet<>();
        trustedPlayers.add(trustedPlayerId);
        
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "vip", 3, trustedPlayers);
        
        assertEquals("vip", protectionData.getProtectionBlockTypeId());
        assertEquals(3, protectionData.getChunkRange());
        assertTrue(protectionData.getTrustedPlayers().contains(trustedPlayerId));
    }

    @Test
    void testIsTrusted() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        protectionData.addTrustedPlayer(trustedPlayerId);
        
        assertTrue(protectionData.isTrusted(ownerId));
        assertTrue(protectionData.isTrusted(trustedPlayerId));
        assertFalse(protectionData.isTrusted(otherPlayerId));
    }

    @Test
    void testAddTrustedPlayer() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        
        assertTrue(protectionData.addTrustedPlayer(trustedPlayerId));
        assertTrue(protectionData.getTrustedPlayers().contains(trustedPlayerId));
        
        // 重複追加はfalseを返す
        assertFalse(protectionData.addTrustedPlayer(trustedPlayerId));
    }

    @Test
    void testRemoveTrustedPlayer() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        protectionData.addTrustedPlayer(trustedPlayerId);
        
        assertTrue(protectionData.removeTrustedPlayer(trustedPlayerId));
        assertFalse(protectionData.getTrustedPlayers().contains(trustedPlayerId));
        
        // 存在しないプレイヤーの削除はfalseを返す
        assertFalse(protectionData.removeTrustedPlayer(trustedPlayerId));
    }

    @Test
    void testGetChunkKey() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        
        assertEquals("test_world:10:20", protectionData.getChunkKey());
    }

    @Test
    void testIsInChunk() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        
        assertTrue(protectionData.isInChunk(mockChunk));
        
        // 異なるチャンク
        Chunk differentChunk = mock(Chunk.class);
        when(differentChunk.getWorld()).thenReturn(mockWorld);
        when(differentChunk.getX()).thenReturn(11);
        when(differentChunk.getZ()).thenReturn(20);
        
        assertFalse(protectionData.isInChunk(differentChunk));
    }

    @Test
    void testProtectedChunkKeysSingleChunk() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "default", 1);
        List<String> protectedChunks = protectionData.getProtectedChunkKeys();
        
        assertEquals(1, protectedChunks.size());
        assertTrue(protectedChunks.contains("test_world:10:20"));
    }

    @Test
    void testProtectedChunkKeysMultiChunk() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "vip", 3);
        List<String> protectedChunks = protectionData.getProtectedChunkKeys();
        
        assertEquals(9, protectedChunks.size()); // 3x3 = 9 chunks
        
        // 中央チャンク周辺のチャンクが含まれているかチェック
        assertTrue(protectedChunks.contains("test_world:9:19"));  // 左上
        assertTrue(protectedChunks.contains("test_world:10:19")); // 上
        assertTrue(protectedChunks.contains("test_world:11:19")); // 右上
        assertTrue(protectedChunks.contains("test_world:9:20"));  // 左
        assertTrue(protectedChunks.contains("test_world:10:20")); // 中央
        assertTrue(protectedChunks.contains("test_world:11:20")); // 右
        assertTrue(protectedChunks.contains("test_world:9:21"));  // 左下
        assertTrue(protectedChunks.contains("test_world:10:21")); // 下
        assertTrue(protectedChunks.contains("test_world:11:21")); // 右下
    }

    @Test
    void testIsChunkProtectedWithString() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "vip", 3);
        
        assertTrue(protectionData.isChunkProtected("test_world:10:20"));
        assertTrue(protectionData.isChunkProtected("test_world:9:19"));
        assertTrue(protectionData.isChunkProtected("test_world:11:21"));
        
        assertFalse(protectionData.isChunkProtected("test_world:8:20"));
        assertFalse(protectionData.isChunkProtected("test_world:12:20"));
        assertFalse(protectionData.isChunkProtected("different_world:10:20"));
    }

    @Test
    void testIsChunkProtectedWithChunk() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "vip", 3);
        
        assertTrue(protectionData.isChunkProtected(mockChunk));
        
        // 範囲外のチャンク
        Chunk outOfRangeChunk = mock(Chunk.class);
        when(outOfRangeChunk.getWorld()).thenReturn(mockWorld);
        when(outOfRangeChunk.getX()).thenReturn(8);
        when(outOfRangeChunk.getZ()).thenReturn(20);
        
        assertFalse(protectionData.isChunkProtected(outOfRangeChunk));
    }

    @Test
    void testGetTrustedPlayersReturnsCopy() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation);
        protectionData.addTrustedPlayer(trustedPlayerId);
        
        Set<UUID> trustedPlayers = protectionData.getTrustedPlayers();
        trustedPlayers.add(otherPlayerId);
        
        // 元のデータは変更されていない
        assertFalse(protectionData.getTrustedPlayers().contains(otherPlayerId));
        assertEquals(1, protectionData.getTrustedPlayers().size());
    }

    @Test
    void testGetProtectedChunkKeysReturnsCopy() {
        ProtectionData protectionData = new ProtectionData(ownerId, mockLocation, "vip", 3);
        List<String> protectedChunks = protectionData.getProtectedChunkKeys();
        protectedChunks.add("test_world:100:100");
        
        // 元のデータは変更されていない
        assertFalse(protectionData.getProtectedChunkKeys().contains("test_world:100:100"));
        assertEquals(9, protectionData.getProtectedChunkKeys().size());
    }
}