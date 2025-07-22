package com.kamesuta.onechunkguard.models;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProtectionData {
    private final UUID owner;
    private final Location protectionBlockLocation;
    private final int chunkX;
    private final int chunkZ;
    private final String worldName;
    private final Set<UUID> trustedPlayers;
    private final String protectionBlockTypeId;
    private final int chunkRange;
    private final List<String> protectedChunkKeys;

    public ProtectionData(UUID owner, Location protectionBlockLocation) {
        this(owner, protectionBlockLocation, "default", 1);
    }
    
    public ProtectionData(UUID owner, Location protectionBlockLocation, String protectionBlockTypeId, int chunkRange) {
        this.owner = owner;
        this.protectionBlockLocation = protectionBlockLocation;
        this.chunkX = protectionBlockLocation.getChunk().getX();
        this.chunkZ = protectionBlockLocation.getChunk().getZ();
        this.worldName = protectionBlockLocation.getWorld().getName();
        this.trustedPlayers = new HashSet<>();
        this.protectionBlockTypeId = protectionBlockTypeId;
        this.chunkRange = chunkRange;
        this.protectedChunkKeys = generateProtectedChunkKeys();
    }

    public ProtectionData(UUID owner, Location protectionBlockLocation, Set<UUID> trustedPlayers) {
        this(owner, protectionBlockLocation, "default", 1);
        this.trustedPlayers.addAll(trustedPlayers);
    }
    
    public ProtectionData(UUID owner, Location protectionBlockLocation, String protectionBlockTypeId, int chunkRange, Set<UUID> trustedPlayers) {
        this(owner, protectionBlockLocation, protectionBlockTypeId, chunkRange);
        this.trustedPlayers.addAll(trustedPlayers);
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getProtectionBlockLocation() {
        return protectionBlockLocation;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public String getWorldName() {
        return worldName;
    }

    public Set<UUID> getTrustedPlayers() {
        return new HashSet<>(trustedPlayers);
    }

    public boolean isTrusted(UUID playerId) {
        return owner.equals(playerId) || trustedPlayers.contains(playerId);
    }

    public boolean addTrustedPlayer(UUID playerId) {
        return trustedPlayers.add(playerId);
    }

    public boolean removeTrustedPlayer(UUID playerId) {
        return trustedPlayers.remove(playerId);
    }

    public String getChunkKey() {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }

    public boolean isInChunk(Chunk chunk) {
        return chunk.getWorld().getName().equals(worldName) &&
                chunk.getX() == chunkX &&
                chunk.getZ() == chunkZ;
    }
    
    public String getProtectionBlockTypeId() {
        return protectionBlockTypeId;
    }
    
    public int getChunkRange() {
        return chunkRange;
    }
    
    /**
     * 保護されているすべてのチャンクキーを取得
     */
    public List<String> getProtectedChunkKeys() {
        return new ArrayList<>(protectedChunkKeys);
    }
    
    /**
     * 保護範囲内のチャンクキーを生成
     */
    private List<String> generateProtectedChunkKeys() {
        List<String> keys = new ArrayList<>();
        int halfRange = chunkRange / 2;
        
        for (int dx = -halfRange; dx <= halfRange; dx++) {
            for (int dz = -halfRange; dz <= halfRange; dz++) {
                int targetChunkX = chunkX + dx;
                int targetChunkZ = chunkZ + dz;
                keys.add(worldName + ":" + targetChunkX + ":" + targetChunkZ);
            }
        }
        
        return keys;
    }
    
    /**
     * 指定されたチャンクが保護範囲内かチェック
     */
    public boolean isChunkProtected(String chunkKey) {
        return protectedChunkKeys.contains(chunkKey);
    }
    
    /**
     * 指定されたチャンクが保護範囲内かチェック
     */
    public boolean isChunkProtected(Chunk chunk) {
        String chunkKey = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        return isChunkProtected(chunkKey);
    }
}