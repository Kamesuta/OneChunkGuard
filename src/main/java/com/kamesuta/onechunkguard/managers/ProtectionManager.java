package com.kamesuta.onechunkguard.managers;

import com.kamesuta.onechunkguard.OneChunkGuard;
import com.kamesuta.onechunkguard.models.ProtectionData;
import com.kamesuta.onechunkguard.models.ProtectionBlockType;
import com.kamesuta.onechunkguard.utils.InventoryUtils;
import com.kamesuta.onechunkguard.utils.ItemUtils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProtectionManager {
    private final OneChunkGuard plugin;

    public ProtectionManager(OneChunkGuard plugin) {
        this.plugin = plugin;
    }

    public boolean createProtection(Player player, Location blockLocation, ItemStack protectionItem) {
        UUID playerId = player.getUniqueId();
        DataManager dataManager = plugin.getDataManager();

        // プレイヤーが既に保護を持っているかチェック
        if (dataManager.hasProtection(playerId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-protected"));
            return false;
        }

        // 保護ブロックのタイプを取得
        String typeId = ItemUtils.getProtectionBlockTypeId(protectionItem);
        if (typeId == null) {
            typeId = "default";
        }
        
        ProtectionBlockType blockType = plugin.getConfigManager().getProtectionBlockType(typeId);
        if (blockType == null) {
            plugin.getLogger().warning("Unknown protection block type: " + typeId);
            return false;
        }

        // 親region制限をチェック
        if (blockType.hasParentRegionRestriction()) {
            if (!isInParentRegion(blockLocation, blockType.getParentRegion())) {
                player.sendMessage(plugin.getConfigManager().getMessage("outside-parent-region", "{region}", blockType.getParentRegion()));
                return false;
            }
        }

        // 保護範囲内のすべてのチャンクをチェック
        List<Chunk> protectedChunks = getProtectedChunks(blockLocation, blockType.getChunkRange());
        
        // 各チャンクが既に保護されているかチェック
        for (Chunk chunk : protectedChunks) {
            String chunkKey = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
            ProtectionData existingProtection = dataManager.getChunkProtection(chunkKey);
            if (existingProtection != null) {
                String ownerName = Bukkit.getOfflinePlayer(existingProtection.getOwner()).getName();
                player.sendMessage(plugin.getConfigManager().getMessage("region-overlap"));
                player.sendMessage(plugin.getConfigManager().getMessage("owner-info", "{owner}", ownerName));
                return false;
            }
        }

        // 各チャンクの既存のWorldGuard領域をチェック（親regionは除外）
        for (Chunk chunk : protectedChunks) {
            if (hasExistingRegions(chunk, blockType.getParentRegion())) {
                player.sendMessage(plugin.getConfigManager().getMessage("region-overlap"));
                return false;
            }
        }

        // WorldGuard領域を作成
        if (!createWorldGuardRegion(player, protectedChunks, blockType)) {
            return false;
        }

        // 保護データを作成
        ProtectionData protection = new ProtectionData(playerId, blockLocation, typeId, blockType.getChunkRange());
        dataManager.addProtection(protection);

        // プレイヤーヘッドを設置
        placePlayerHead(blockLocation, player);

        player.sendMessage(plugin.getConfigManager().getMessage("protection-created"));
        return true;
    }

    public boolean removeProtection(Player player, boolean returnBlock) {
        return removeProtection(player, returnBlock, false);
    }
    
    public boolean removeProtection(Player player, boolean returnBlock, boolean fromBlockBreak) {
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
            // default以外のブロックは破壊時以外は返却しない
            String blockTypeId = protection.getProtectionBlockTypeId();
            if ("default".equals(blockTypeId) || fromBlockBreak) {
                InventoryUtils.giveProtectionBlock(player, blockTypeId);
            } else {
                // default以外で/unprotectコマンドから呼ばれた場合は返却しない
                player.sendMessage(plugin.getConfigManager().getMessage("unprotect-success-no-return"));
                return true;
            }
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
                        .toList() : Collections.<ProtectionData>emptyList()) {
            if (protection != null && protection.getProtectionBlockLocation().equals(blockLocation)) {
                return protection.getOwner().equals(player.getUniqueId()) || player.hasPermission("onechunkguard.admin");
            }
        }

        // これが保護ブロックかチェック
        String chunkKey = blockLocation.getWorld().getName() + ":" +
                blockLocation.getChunk().getX() + ":" +
                blockLocation.getChunk().getZ();
        ProtectionData chunkProtection = dataManager.getChunkProtection(chunkKey);

        if (chunkProtection != null && chunkProtection.getProtectionBlockLocation().equals(blockLocation)) {
            return chunkProtection.getOwner().equals(player.getUniqueId()) || player.hasPermission("onechunkguard.admin");
        }

        return true; // 保護ブロックではない
    }

    private boolean hasExistingRegions(Chunk chunk) {
        return hasExistingRegions(chunk, null);
    }
    
    private boolean hasExistingRegions(Chunk chunk, String excludeParentRegion) {
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

        // 領域が存在する場合、親regionを除外してチェック
        if (regions.size() > 0) {
            if (excludeParentRegion != null && !excludeParentRegion.trim().isEmpty()) {
                // 親region以外の領域があるかチェック
                for (ProtectedRegion region : regions) {
                    if (!region.getId().equals(excludeParentRegion)) {
                        return true; // 親region以外の領域が存在
                    }
                }
                return false; // 親regionのみ
            }
            return true; // 親region除外指定なし、何らかの領域が存在
        }
        
        return false; // 領域なし
    }

    /**
     * 親region内かチェック
     */
    private boolean isInParentRegion(Location location, String parentRegionName) {
        World world = location.getWorld();
        RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            return false;
        }
        
        ProtectedRegion parentRegion = regionManager.getRegion(parentRegionName);
        if (parentRegion == null) {
            plugin.getLogger().warning("Parent region not found: " + parentRegionName);
            return false;
        }
        
        BlockVector3 point = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return parentRegion.contains(point);
    }
    
    /**
     * 保護範囲内のチャンクリストを取得
     */
    private List<Chunk> getProtectedChunks(Location centerLocation, int range) {
        List<Chunk> chunks = new ArrayList<>();
        Chunk centerChunk = centerLocation.getChunk();
        int halfRange = range / 2;
        
        for (int dx = -halfRange; dx <= halfRange; dx++) {
            for (int dz = -halfRange; dz <= halfRange; dz++) {
                int targetX = centerChunk.getX() + dx;
                int targetZ = centerChunk.getZ() + dz;
                chunks.add(centerChunk.getWorld().getChunkAt(targetX, targetZ));
            }
        }
        
        return chunks;
    }
    
    private boolean createWorldGuardRegion(Player player, List<Chunk> chunks, ProtectionBlockType blockType) {
        if (chunks.isEmpty()) {
            return false;
        }
        
        World world = chunks.get(0).getWorld();
        RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            plugin.getLogger().warning("ワールドのRegionManagerを取得できません: " + world.getName());
            return false;
        }

        String regionId = "onechunk_" + player.getUniqueId();
        
        // 範囲を計算
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (Chunk chunk : chunks) {
            int chunkMinX = chunk.getX() * 16;
            int chunkMinZ = chunk.getZ() * 16;
            int chunkMaxX = chunkMinX + 15;
            int chunkMaxZ = chunkMinZ + 15;
            
            minX = Math.min(minX, chunkMinX);
            minZ = Math.min(minZ, chunkMinZ);
            maxX = Math.max(maxX, chunkMaxX);
            maxZ = Math.max(maxZ, chunkMaxZ);
        }
        
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