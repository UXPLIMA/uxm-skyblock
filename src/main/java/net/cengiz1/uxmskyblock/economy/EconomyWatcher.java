package net.cengiz1.uxmskyblock.economy;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;

public class EconomyWatcher implements Listener {

    private final UxmSkyblockPlugin plugin;

    public EconomyWatcher(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() != Economy.class)
            return;
        if (!plugin.getSettings().isEconomyEnabled() || plugin.getEconomy().isEnabled())
            return;
        EconomyHook hook = VaultEconomyHook.setup();
        if (hook != null) {
            plugin.setEconomy(hook);
            plugin.getLogger().info("Hooked into Vault economy (registered after startup).");
        }
    }
}
