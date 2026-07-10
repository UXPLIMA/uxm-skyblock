package net.cengiz1.uxmskyblock.listener;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.island.BorderManager;
import net.cengiz1.uxmskyblock.island.IslandManager;
import net.cengiz1.uxmskyblock.level.BlockTrackListener;
import net.cengiz1.uxmskyblock.menu.MenuListener;
import net.cengiz1.uxmskyblock.proxy.ProxyListener;
import net.cengiz1.uxmskyblock.upgrade.UpgradeEffectListener;
import org.bukkit.event.Listener;

public final class ListenerRegistrar {

    private ListenerRegistrar() {
    }

    public static void registerAll(UxmSkyblockPlugin plugin) {
        IslandManager islandManager = plugin.getIslandManager();

        register(plugin, new MenuListener(plugin.getMenuManager()));

        register(plugin, new PvpListener(islandManager));
        register(plugin, new MobSpawnListener(islandManager));
        register(plugin, new ExplosionListener(islandManager));
        register(plugin, new FireListener(islandManager));
        register(plugin, new VisitorInteractListener(islandManager));
        register(plugin, new WarpSignListener(plugin));
        register(plugin, new ObsidianBucketListener(plugin, islandManager));

        if (plugin.getDimensionManager() != null)
            register(plugin, new PortalListener(plugin));

        register(plugin, new BlockTrackListener(plugin, islandManager,
                plugin.getBlockValueManager(), plugin.getLevelManager()));
        register(plugin, new UpgradeEffectListener(islandManager, plugin.getUpgradeManager()));

        if (plugin.getLimitManager() != null)
            register(plugin, plugin.getLimitManager());

        BorderManager borderManager = new BorderManager(plugin, islandManager);
        islandManager.setBorderManager(borderManager);
        register(plugin, borderManager);

        if (plugin.getProxyManager() != null && plugin.getProxyManager().isEnabled())
            register(plugin, new ProxyListener(plugin.getProxyManager()));
    }

    private static void register(UxmSkyblockPlugin plugin, Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}
