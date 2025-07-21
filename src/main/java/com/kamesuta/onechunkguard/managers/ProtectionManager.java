package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ProtectionManager {
    private final OneChunkGuard plugin;
    
    public ProtectionManager(OneChunkGuard plugin) {
        this.plugin = plugin;
    }
    
    public boolean createProtection(Player player, Location blockLocation) {
        UUID playerId = player.getUniqueId();
        DataManager dataManager = plugin.getDataManager();
        
        // プレイヤーが既に保護を持っているかチェック
        if (dataManager.hasProtection(playerId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-protected"));
            return false;
        }
        
        Chunk chunk = blockLocation.getChunk();
        String chunkKey = blockLocation.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        
        // チャンクが既に保護されているかチェック
        ProtectionData existingProtection = dataManager.getChunkProtection(chunkKey);
        if (existingProtection != null) {
            String ownerName = Bukkit.getOfflinePlayer(existingProtection.getOwner()).getName();
            player.sendMessage(plugin.getConfigManager().getMessage("region-overlap"));
            player.sendMessage(plugin.getConfigManager().getMessage("owner-info", "{owner}", ownerName));
            return false;
        }
        
        // 既存のWorldGuard領域をチェック
        if (hasExistingRegions(chunk)) {
            player.sendMessage(plugin.getConfigManager().getMessage("region-overlap"));
            return false;
        }
        
        // WorldGuard領域を作成
        if (!createWorldGuardRegion(player, chunk)) {
            return false;
        }
        
        // 保護データを作成
        ProtectionData protection = new ProtectionData(playerId, blockLocation);
        dataManager.addProtection(protection);
        
        // プレイヤーヘッドを設置
        placePlayerHead(blockLocation, player);
        
        player.sendMessage(plugin.getConfigManager().getMessage("protection-created"));
        return true;
    }
    
    public boolean removeProtection(Player player, boolean returnBlock) {
        UUID playerId = player.getUniqueId();
        DataManager dataManager = plugin.getDataManager();
        
        ProtectionData protection = dataManager.getPlayerProtection(playerId);
        if (protection == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-protection"));
            return false;
        }
        
        // WorldGuard領域を削除
        removeWorldGuardRegion(playerId, protection.getWorldName());
        
        // 保護ブロックとヘッドを削除
        Location blockLoc = protection.getProtectionBlockLocation();
        if (blockLoc.getWorld() != null) {
            Block block = blockLoc.getBlock();
            block.setType(Material.AIR);
            
            // 上のヘッドを削除
            Block above = blockLoc.clone().add(0, 1, 0).getBlock();
            if (above.getType() == Material.PLAYER_HEAD || above.getType() == Material.PLAYER_WALL_HEAD) {
                above.setType(Material.AIR);
            }
        }
        
        // データから削除
        dataManager.removeProtection(playerId);
        
        // プレイヤーに保護ブロックを返却
        if (returnBlock) {
            player.getInventory().setItem(8, com.kamesuta.onechunkguard.utils.ItemUtils.createProtectionBlock());
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("unprotect-success"));
        return true;
    }
    
    public boolean canBreakProtectionBlock(Player player, Location blockLocation) {
        DataManager dataManager = plugin.getDataManager();
        
        // すべての保護をチェック
        for (ProtectionData protection : dataManager.getPlayerProtection(player.getUniqueId()) != null ? 
                dataManager.getPlayerProtection(player.getUniqueId()).getTrustedPlayers().stream()
                    .map(dataManager::getPlayerProtection)
                    .toList() : java.util.Collections.<ProtectionData>emptyList()) {
            if (protection != null && protection.getProtectionBlockLocation().equals(blockLocation)) {
                return protection.getOwner().equals(player.getUniqueId()) || player.isOp();
            }
        }
        
        // これが保護ブロックかチェック
        String chunkKey = blockLocation.getWorld().getName() + ":" + 
                         blockLocation.getChunk().getX() + ":" + 
                         blockLocation.getChunk().getZ();
        ProtectionData chunkProtection = dataManager.getChunkProtection(chunkKey);
        
        if (chunkProtection != null && chunkProtection.getProtectionBlockLocation().equals(blockLocation)) {
            return chunkProtection.getOwner().equals(player.getUniqueId()) || player.isOp();
        }
        
        return true; // 保護ブロックではない
    }
    
    private boolean hasExistingRegions(Chunk chunk) {
        World world = chunk.getWorld();
        RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) return false;
        
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int minY = plugin.getConfigManager().getMinY();
        int maxY = plugin.getConfigManager().getMaxY();
        
        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
        
        ApplicableRegionSet regions = regionManager.getApplicableRegions(
            new ProtectedCuboidRegion("temp", min, max));
        
        return regions.size() > 0;
    }
    
    private boolean createWorldGuardRegion(Player player, Chunk chunk) {
        World world = chunk.getWorld();
        RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            plugin.getLogger().warning("ワールドのRegionManagerを取得できません: " + world.getName());
            return false;
        }
        
        String regionId = "onechunk_" + player.getUniqueId().toString();
        
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int minY = plugin.getConfigManager().getMinY();
        int maxY = plugin.getConfigManager().getMaxY();
        
        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        
        // 所有者を設定
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(player.getUniqueId());
        region.setOwners(owners);
        
        // フラグを設定（所有者とメンバーは除外）
        // FLAGSは設定しない - 所有者とメンバーが自由に使えるように
        
        regionManager.addRegion(region);
        
        return true;
    }
    
    private void removeWorldGuardRegion(UUID playerId, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        
        RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) return;
        
        String regionId = "onechunk_" + playerId.toString();
        regionManager.removeRegion(regionId);
    }
    
    private void placePlayerHead(Location blockLocation, Player player) {
        Location headLocation = blockLocation.clone().add(0, 1, 0);
        Block headBlock = headLocation.getBlock();
        
        headBlock.setType(Material.PLAYER_HEAD);
        
        if (headBlock.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update();
        }
    }
    
    public void addTrustedPlayer(UUID ownerId, UUID trustedId) {
        DataManager dataManager = plugin.getDataManager();
        ProtectionData protection = dataManager.getPlayerProtection(ownerId);
        
        if (protection == null) return;
        
        protection.addTrustedPlayer(trustedId);
        dataManager.saveData();
        
        // WorldGuard領域を更新
        World world = Bukkit.getWorld(protection.getWorldName());
        if (world != null) {
            RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion("onechunk_" + ownerId.toString());
                if (region != null) {
                    DefaultDomain members = region.getMembers();
                    members.addPlayer(trustedId);
                }
            }
        }
    }
    
    public void removeTrustedPlayer(UUID ownerId, UUID trustedId) {
        DataManager dataManager = plugin.getDataManager();
        ProtectionData protection = dataManager.getPlayerProtection(ownerId);
        
        if (protection == null) return;
        
        protection.removeTrustedPlayer(trustedId);
        dataManager.saveData();
        
        // WorldGuard領域を更新
        World world = Bukkit.getWorld(protection.getWorldName());
        if (world != null) {
            RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion("onechunk_" + ownerId.toString());
                if (region != null) {
                    DefaultDomain members = region.getMembers();
                    members.removePlayer(trustedId);
                }
            }
        }
    }
}