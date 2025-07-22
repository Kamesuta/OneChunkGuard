package com.kamesuta.onechunkguard.models;

import org.bukkit.Material;

import java.util.List;

/**
 * 保護ブロックの種類を表すモデルクラス
 */
public class ProtectionBlockType {
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final String parentRegion;
    private final int chunkRange;

    public ProtectionBlockType(String id, Material material, String displayName, 
                              List<String> lore, String parentRegion, int chunkRange) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.parentRegion = parentRegion;
        this.chunkRange = chunkRange;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getParentRegion() {
        return parentRegion;
    }

    public int getChunkRange() {
        return chunkRange;
    }

    /**
     * 親regionの制限があるかチェック
     */
    public boolean hasParentRegionRestriction() {
        return parentRegion != null && !parentRegion.trim().isEmpty();
    }

    /**
     * マルチチャンク保護かチェック
     */
    public boolean isMultiChunk() {
        return chunkRange > 1;
    }
}