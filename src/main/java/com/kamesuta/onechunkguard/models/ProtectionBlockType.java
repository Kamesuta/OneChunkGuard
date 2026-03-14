package com.kamesuta.onechunkguard.models;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

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
    private final String areaName;
    private final Map<String, String> flags;
    /** 初期保護時間（秒）【0の場合は時間制限なし */
    private final long initialDurationSeconds;
    /** 補充コスト係数（次の補充は前の補充の係数倍のコスト） */
    private final double costMultiplier;
    /** 補充アイテム（Material -&gt; 追加秒数） */
    private final Map<Material, Long> refillItems;

    public ProtectionBlockType(String id, Material material, String displayName, 
                              List<String> lore, String parentRegion, int chunkRange, String areaName, Map<String, String> flags,
                              long initialDurationSeconds, double costMultiplier, Map<Material, Long> refillItems) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.parentRegion = parentRegion;
        this.chunkRange = chunkRange;
        this.areaName = areaName;
        this.flags = flags;
        this.initialDurationSeconds = initialDurationSeconds;
        this.costMultiplier = costMultiplier;
        this.refillItems = refillItems;
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

    public String getAreaName() {
        return areaName;
    }

    /**
     * マルチチャンク保護かチェック
     */
    public boolean isMultiChunk() {
        return chunkRange > 1;
    }

    public Map<String, String> getFlags() {
        return flags;
    }

    /**
     * 初期保護時間（秒）を取得【0の場合は時間制限なし
     */
    public long getInitialDurationSeconds() {
        return initialDurationSeconds;
    }

    /**
     * 時間制限機能が有効かチェック
     */
    public boolean hasTimeLimit() {
        return initialDurationSeconds > 0;
    }

    /**
     * 補充コスト係数を取得
     */
    public double getCostMultiplier() {
        return costMultiplier;
    }

    /**
     * 補充アイテムマップ（Material -&gt; 追加秒数）を取得
     */
    public Map<Material, Long> getRefillItems() {
        return refillItems;
    }
}