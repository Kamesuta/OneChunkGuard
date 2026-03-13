package com.kamesuta.onechunkguard.models;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProtectionBlockTypeTest {

    @Test
    void testConstructorAndGetters() {
        String id = "vip";
        Material material = Material.DIAMOND_BLOCK;
        String displayName = "&b&lVIP保護ブロック";
        List<String> lore = List.of("VIP専用の保護ブロック", "3x3チャンクを保護");
        String parentRegion = "vip_area";
        int chunkRange = 3;
        String areaName = "VIPエリア";
        Map<String, String> flags = new HashMap<>();
        flags.put("pvp", "deny");
        flags.put("mob-spawning", "deny");

        ProtectionBlockType type = new ProtectionBlockType(id, material, displayName, lore, 
                                                          parentRegion, chunkRange, areaName, flags);

        assertEquals(id, type.getId());
        assertEquals(material, type.getMaterial());
        assertEquals(displayName, type.getDisplayName());
        assertEquals(lore, type.getLore());
        assertEquals(parentRegion, type.getParentRegion());
        assertEquals(chunkRange, type.getChunkRange());
        assertEquals(areaName, type.getAreaName());
        assertEquals(flags, type.getFlags());
    }

    @Test
    void testHasParentRegionRestriction() {
        // 親リージョン制限あり
        ProtectionBlockType typeWithRestriction = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "VIP", List.of(), 
            "vip_area", 3, "VIPエリア", new HashMap<>()
        );
        assertTrue(typeWithRestriction.hasParentRegionRestriction());

        // 親リージョン制限なし（空文字）
        ProtectionBlockType typeWithoutRestriction1 = new ProtectionBlockType(
            "default", Material.END_STONE, "Default", List.of(), 
            "", 1, "デフォルトエリア", new HashMap<>()
        );
        assertFalse(typeWithoutRestriction1.hasParentRegionRestriction());

        // 親リージョン制限なし（null）
        ProtectionBlockType typeWithoutRestriction2 = new ProtectionBlockType(
            "default", Material.END_STONE, "Default", List.of(), 
            null, 1, "デフォルトエリア", new HashMap<>()
        );
        assertFalse(typeWithoutRestriction2.hasParentRegionRestriction());

        // 親リージョン制限なし（空白のみ）
        ProtectionBlockType typeWithoutRestriction3 = new ProtectionBlockType(
            "default", Material.END_STONE, "Default", List.of(), 
            "   ", 1, "デフォルトエリア", new HashMap<>()
        );
        assertFalse(typeWithoutRestriction3.hasParentRegionRestriction());
    }

    @Test
    void testIsMultiChunk() {
        // シングルチャンク
        ProtectionBlockType singleChunk = new ProtectionBlockType(
            "default", Material.END_STONE, "Default", List.of(), 
            "", 1, "デフォルトエリア", new HashMap<>()
        );
        assertFalse(singleChunk.isMultiChunk());

        // マルチチャンク
        ProtectionBlockType multiChunk = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "VIP", List.of(), 
            "vip_area", 3, "VIPエリア", new HashMap<>()
        );
        assertTrue(multiChunk.isMultiChunk());

        // 5x5チャンク
        ProtectionBlockType largeChunk = new ProtectionBlockType(
            "premium", Material.EMERALD_BLOCK, "Premium", List.of(), 
            "premium_area", 5, "プレミアムエリア", new HashMap<>()
        );
        assertTrue(largeChunk.isMultiChunk());
    }

    @Test
    void testDefaultProtectionBlockType() {
        ProtectionBlockType defaultType = new ProtectionBlockType(
            "default", Material.END_STONE, "&6&l保護ブロック", 
            List.of("1x1チャンクを保護", "基本的な保護ブロック"), 
            "", 1, "デフォルトエリア", new HashMap<>()
        );

        assertEquals("default", defaultType.getId());
        assertEquals(Material.END_STONE, defaultType.getMaterial());
        assertEquals("&6&l保護ブロック", defaultType.getDisplayName());
        assertFalse(defaultType.hasParentRegionRestriction());
        assertFalse(defaultType.isMultiChunk());
        assertEquals(1, defaultType.getChunkRange());
    }

    @Test
    void testVipProtectionBlockType() {
        Map<String, String> vipFlags = new HashMap<>();
        vipFlags.put("pvp", "deny");
        vipFlags.put("mob-spawning", "deny");
        vipFlags.put("creeper-explosion", "deny");

        ProtectionBlockType vipType = new ProtectionBlockType(
            "vip", Material.DIAMOND_BLOCK, "&b&lVIP保護ブロック", 
            List.of("3x3チャンクを保護", "VIP専用エリア"), 
            "vip_area", 3, "VIPエリア", vipFlags
        );

        assertEquals("vip", vipType.getId());
        assertEquals(Material.DIAMOND_BLOCK, vipType.getMaterial());
        assertEquals("&b&lVIP保護ブロック", vipType.getDisplayName());
        assertTrue(vipType.hasParentRegionRestriction());
        assertTrue(vipType.isMultiChunk());
        assertEquals(3, vipType.getChunkRange());
        assertEquals("vip_area", vipType.getParentRegion());
        assertEquals("VIPエリア", vipType.getAreaName());
        assertEquals(vipFlags, vipType.getFlags());
    }

    @Test
    void testEmptyLoreAndFlags() {
        ProtectionBlockType type = new ProtectionBlockType(
            "test", Material.STONE, "Test", List.of(), 
            "", 1, "Test Area", new HashMap<>()
        );

        assertTrue(type.getLore().isEmpty());
        assertTrue(type.getFlags().isEmpty());
    }

    @Test
    void testLargeChunkRange() {
        ProtectionBlockType largeType = new ProtectionBlockType(
            "mega", Material.NETHERITE_BLOCK, "Mega", List.of(), 
            "mega_area", 7, "Mega Area", new HashMap<>()
        );

        assertEquals(7, largeType.getChunkRange());
        assertTrue(largeType.isMultiChunk());
    }
}