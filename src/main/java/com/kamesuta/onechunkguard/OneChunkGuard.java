package com.kamesuta.onechunkguard;

import com.kamesuta.onechunkguard.commands.*;
import com.kamesuta.onechunkguard.listeners.*;
import com.kamesuta.onechunkguard.managers.*;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class OneChunkGuard extends JavaPlugin {
    private static OneChunkGuard instance;
    private ConfigManager configManager;
    private ProtectionManager protectionManager;
    private DataManager dataManager;
    private RegionContainer regionContainer;

    @Override
    public void onEnable() {
        instance = this;
        
        // 設定の初期化
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        
        // WorldGuardの初期化
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        
        // マネージャーの初期化
        this.dataManager = new DataManager(this);
        this.protectionManager = new ProtectionManager(this);
        
        // イベントの登録
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(this), this);
        
        // コマンドの登録
        getCommand("unprotect").setExecutor(new UnprotectCommand(this));
        getCommand("trust").setExecutor(new TrustCommand(this));
        getCommand("untrust").setExecutor(new UntrustCommand(this));
        getCommand("trustlist").setExecutor(new TrustListCommand(this));
        
        getLogger().info("OneChunkGuardが有効になりました！");
    }
    
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("OneChunkGuardが無効になりました！");
    }
    
    public static OneChunkGuard getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public RegionContainer getRegionContainer() {
        return regionContainer;
    }
}
