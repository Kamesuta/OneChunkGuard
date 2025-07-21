package com.kamesuta.onechunkguard.models;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;

public class ProtectionData {
    private final UUID owner;
    private final Location protectionBlockLocation;
    private final int chunkX;
    private final int chunkZ;
    private final String worldName;
    private final Set<UUID> trustedPlayers;
    
    public ProtectionData(UUID owner, Location protectionBlockLocation) {
        this.owner = owner;
        this.protectionBlockLocation = protectionBlockLocation;
        this.chunkX = protectionBlockLocation.getChunk().getX();
        this.chunkZ = protectionBlockLocation.getChunk().getZ();
        this.worldName = protectionBlockLocation.getWorld().getName();
        this.trustedPlayers = new HashSet<>();
    }
    
    public ProtectionData(UUID owner, Location protectionBlockLocation, Set<UUID> trustedPlayers) {
        this(owner, protectionBlockLocation);
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
}