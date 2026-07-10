package net.cengiz1.uxmskyblock;

import net.cengiz1.uxmskyblock.command.CommandRegistrar;
import net.cengiz1.uxmskyblock.config.ConfigMigrator;
import net.cengiz1.uxmskyblock.config.MessageManager;
import net.cengiz1.uxmskyblock.config.SettingsManager;
import net.cengiz1.uxmskyblock.economy.EconomyHook;
import net.cengiz1.uxmskyblock.economy.NoEconomyHook;
import net.cengiz1.uxmskyblock.economy.VaultEconomyHook;
import net.cengiz1.uxmskyblock.invite.InviteManager;
import net.cengiz1.uxmskyblock.island.IslandManager;
import net.cengiz1.uxmskyblock.island.IslandTimeTask;
import net.cengiz1.uxmskyblock.island.RoleManager;
import net.cengiz1.uxmskyblock.island.VisitService;
import net.cengiz1.uxmskyblock.island.WarpService;
import net.cengiz1.uxmskyblock.level.BlockValueManager;
import net.cengiz1.uxmskyblock.level.LevelManager;
import net.cengiz1.uxmskyblock.listener.ListenerRegistrar;
import net.cengiz1.uxmskyblock.menu.MenuManager;
import net.cengiz1.uxmskyblock.module.ModuleManager;
import net.cengiz1.uxmskyblock.proxy.ProxyManager;
import net.cengiz1.uxmskyblock.storage.SqlStorage;
import net.cengiz1.uxmskyblock.storage.Storage;
import net.cengiz1.uxmskyblock.upgrade.UpgradeManager;
import net.cengiz1.uxmskyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class UxmSkyblockPlugin extends JavaPlugin {

    private SettingsManager settings;
    private MessageManager messages;
    private Storage storage;
    private WorldManager worldManager;
    private net.cengiz1.uxmskyblock.world.DimensionManager dimensionManager;
    private IslandManager islandManager;
    private MenuManager menuManager;
    private BlockValueManager blockValueManager;
    private LevelManager levelManager;
    private UpgradeManager upgradeManager;
    private RoleManager roleManager;
    private InviteManager inviteManager;
    private VisitService visitService;
    private WarpService warpService;
    private net.cengiz1.uxmskyblock.island.TopService topService;
    private net.cengiz1.uxmskyblock.island.TopHologramManager topHologramManager;
    private EconomyHook economy;
    private ProxyManager proxyManager;
    private ModuleManager moduleManager;
    private net.cengiz1.uxmskyblock.island.BankService bankService;
    private net.cengiz1.uxmskyblock.island.RatingService ratingService;
    private net.cengiz1.uxmskyblock.island.BiomeService biomeService;
    private net.cengiz1.uxmskyblock.island.LimitManager limitManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigMigrator.sync(this, "config.yml");
        ConfigMigrator.sync(this, "messages.yml");
        ConfigMigrator.sync(this, "roles.yml");
        ConfigMigrator.sync(this, "levels.yml");
        ConfigMigrator.sync(this, "block-values.yml");
        ConfigMigrator.sync(this, "upgrades.yml");
        reloadConfig();

        this.settings = new SettingsManager(this);
        this.messages = new MessageManager(this);

        try {
            this.storage = new SqlStorage(this, this.settings);
            this.storage.init();
        } catch (Exception error) {
            getLogger().severe("Could not connect to the database: " + error.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.worldManager = new WorldManager(this, this.settings);
        if (this.settings.isIslandEnabled())
            this.worldManager.loadWorld();
        else
            getLogger().info("Island system disabled (island.enabled: false); running for modules only.");

        this.islandManager = new IslandManager(this, this.settings, this.storage, this.worldManager);
        this.islandManager.loadAll();

        this.dimensionManager = new net.cengiz1.uxmskyblock.world.DimensionManager(
                this.settings, this.worldManager, this.islandManager.getSchematicService());

        this.blockValueManager = new BlockValueManager(this);
        this.levelManager = new LevelManager(this);
        this.roleManager = new RoleManager(this);
        net.cengiz1.uxmskyblock.island.Island.setResolver(this.roleManager);
        this.inviteManager = new InviteManager(this.settings.getInviteExpireSeconds());
        this.visitService = new VisitService(this);
        this.warpService = new WarpService(this);
        this.topService = new net.cengiz1.uxmskyblock.island.TopService(this);

        this.economy = setupEconomy();
        this.upgradeManager = new UpgradeManager(this);
        this.islandManager.setUpgradeManager(this.upgradeManager);

        this.bankService = new net.cengiz1.uxmskyblock.island.BankService(this);
        this.ratingService = new net.cengiz1.uxmskyblock.island.RatingService(this);
        this.biomeService = new net.cengiz1.uxmskyblock.island.BiomeService(this);
        this.limitManager = new net.cengiz1.uxmskyblock.island.LimitManager(this);

        this.menuManager = new MenuManager(this);

        this.proxyManager = new ProxyManager(this);
        this.proxyManager.start();

        if (this.settings.isIslandEnabled()) {
            ListenerRegistrar.registerAll(this);

            this.topHologramManager = new net.cengiz1.uxmskyblock.island.TopHologramManager(this, this.topService);
            getServer().getPluginManager().registerEvents(this.topHologramManager, this);
            this.topHologramManager.start();

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
                registerPlaceholders();

            CommandRegistrar.register(this);

            new IslandTimeTask(this, this.islandManager, this.settings)
                    .runTaskTimer(this, 40L, 40L);

            long interestPeriod = 20L * 60L * Math.max(1,
                    getConfig().getInt("bank.interest.check-interval-seconds", 60));
            new net.cengiz1.uxmskyblock.island.BankInterestTask(this)
                    .runTaskTimer(this, interestPeriod, interestPeriod);
        }

        this.moduleManager = new ModuleManager(this);
        this.moduleManager.loadModules();

        getLogger().info("uxmSkyblock enabled.");
    }

    @Override
    public void onDisable() {
        if (this.topHologramManager != null)
            this.topHologramManager.stop();
        if (this.moduleManager != null)
            this.moduleManager.unloadModules();
        CommandRegistrar.unregister(this);
        if (this.proxyManager != null)
            this.proxyManager.stop();
        if (this.islandManager != null)
            this.islandManager.shutdown();
        if (this.storage != null)
            this.storage.close();
    }

    private void registerPlaceholders() {
        try {
            new net.cengiz1.uxmskyblock.placeholder.UxmSkyblockExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI (%skyblock_...%).");
        } catch (Throwable error) {
            getLogger().warning("Could not register PlaceholderAPI expansion: " + error.getMessage());
        }
    }

    private EconomyHook setupEconomy() {
        if (!this.settings.isEconomyEnabled()) {
            getLogger().info("Economy disabled in config; money requirements will be skipped.");
            return new NoEconomyHook();
        }
        try {
            EconomyHook hook = VaultEconomyHook.setup();
            if (hook != null) {
                getLogger().info("Hooked into Vault economy.");
                return hook;
            }
        } catch (Throwable error) {
            getLogger().warning("Could not hook into Vault: " + error.getMessage());
        }
        getLogger().info("Vault not found; upgrades will only require island level.");
        return new NoEconomyHook();
    }

    public void reloadAll() {
        reloadConfig();
        this.settings.reload();
        this.messages.reload();
        this.blockValueManager.reload();
        this.levelManager.reload();
        this.upgradeManager.reload();
        this.roleManager.reload();
        this.menuManager.reload();
    }

    public SettingsManager getSettings() {
        return settings;
    }

    public MessageManager getMessages() {
        return messages;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public net.cengiz1.uxmskyblock.world.DimensionManager getDimensionManager() {
        return dimensionManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    public Storage getStorage() {
        return storage;
    }

    public net.cengiz1.uxmskyblock.island.BankService getBankService() {
        return bankService;
    }

    public net.cengiz1.uxmskyblock.island.RatingService getRatingService() {
        return ratingService;
    }

    public net.cengiz1.uxmskyblock.island.BiomeService getBiomeService() {
        return biomeService;
    }

    public net.cengiz1.uxmskyblock.island.LimitManager getLimitManager() {
        return limitManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public BlockValueManager getBlockValueManager() {
        return blockValueManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public VisitService getVisitService() {
        return visitService;
    }

    public WarpService getWarpService() {
        return warpService;
    }

    public net.cengiz1.uxmskyblock.island.TopService getTopService() {
        return topService;
    }

    public net.cengiz1.uxmskyblock.island.TopHologramManager getTopHologramManager() {
        return topHologramManager;
    }

    public EconomyHook getEconomy() {
        return economy;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
