package com.kamesuta.onechunkguard;

import com.kamesuta.onechunkguard.commands.GiveProtectionBlockCommand;
import com.kamesuta.onechunkguard.commands.TrustCommand;
import com.kamesuta.onechunkguard.commands.TrustListCommand;
import com.kamesuta.onechunkguard.commands.UnprotectCommand;
import com.kamesuta.onechunkguard.commands.UntrustCommand;
import com.kamesuta.onechunkguard.listeners.*;
import com.kamesuta.onechunkguard.managers.ConfigManager;
import com.kamesuta.onechunkguard.managers.DataManager;
import com.kamesuta.onechunkguard.managers.ProtectionManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class OneChunkGuard extends JavaPlugin {
    private static OneChunkGuard instance;
    private ConfigManager configManager;
    private ProtectionManager protectionManager;
    private DataManager dataManager;
    private RegionContainer regionContainer;

    public static OneChunkGuard getInstance() {
        return instance;
    }

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

        // イベントの登録（目的別にまとめたリスナー）
        // 初回ログイン時配布リスナー
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 保護ブロックをスロット9に固定するリスナー（死亡時の処理もこれ）
        getServer().getPluginManager().registerEvents(new ProtectionBlockInventoryListener(this), this);

        // 設置/回収リスナー
        getServer().getPluginManager().registerEvents(new ProtectionBlockPlaceBreakListener(this), this);

        // 保護ブロックのTUIを開くリスナー
        getServer().getPluginManager().registerEvents(new ProtectionBlockInteractListener(this), this);

        // チャンクに入ったときにメッセージを出すリスナー
        getServer().getPluginManager().registerEvents(new ChunkEntryListener(this), this);
        
        // チャンクビジュアライザーリスナー
        getServer().getPluginManager().registerEvents(new ChunkVisualizerListener(this), this);

        // コマンドの登録
        UnprotectCommand unprotectCommand = new UnprotectCommand(this);
        getCommand("unprotect").setExecutor(unprotectCommand);
        getCommand("unprotect").setTabCompleter(unprotectCommand);
        getCommand("trust").setExecutor(new TrustCommand(this));
        getCommand("untrust").setExecutor(new UntrustCommand(this));
        getCommand("trustlist").setExecutor(new TrustListCommand(this));
        
        // 管理者用コマンド
        GiveProtectionBlockCommand giveCommand = new GiveProtectionBlockCommand(this);
        getCommand("giveprotectionblock").setExecutor(giveCommand);
        getCommand("giveprotectionblock").setTabCompleter(giveCommand);

        // bStats初期化
        int pluginId = 26619;
        Metrics metrics = new Metrics(this, pluginId);

        getLogger().info("OneChunkGuard has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("OneChunkGuard has been disabled!");
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
