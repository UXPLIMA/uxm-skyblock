package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.proxy.ProxyManager;
import org.bukkit.scheduler.BukkitRunnable;

public class BankInterestTask extends BukkitRunnable {

    private final UxmSkyblockPlugin plugin;

    public BankInterestTask(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("bank.interest.enabled", false))
            return;

        double rate = plugin.getConfig().getDouble("bank.interest.rate", 0.02);
        long intervalMillis = Math.max(1, plugin.getConfig().getLong("bank.interest.interval-minutes", 60)) * 60_000L;
        double maxBalance = plugin.getConfig().getDouble("bank.interest.max-balance", 1_000_000);
        double minBalance = plugin.getConfig().getDouble("bank.interest.min-balance", 0);
        if (rate <= 0)
            return;

        ProxyManager proxy = plugin.getProxyManager();
        String localServer = proxy != null && proxy.isEnabled() ? proxy.getServerName() : null;
        long now = System.currentTimeMillis();

        for (Island island : plugin.getIslandManager().getAllIslands()) {
            if (!ownsIsland(localServer, island))
                continue;

            if (island.getBankInterestAt() == 0) {
                island.setBankInterestAt(now);
                plugin.getIslandManager().saveAsync(island);
                continue;
            }
            if (now - island.getBankInterestAt() < intervalMillis)
                continue;

            double balance = island.getBank();
            if (balance < minBalance || balance <= 0) {
                island.setBankInterestAt(now);
                plugin.getIslandManager().saveAsync(island);
                continue;
            }

            double base = Math.min(balance, maxBalance);
            double gain = Math.floor(base * rate * 100) / 100.0;
            if (gain <= 0) {
                island.setBankInterestAt(now);
                plugin.getIslandManager().saveAsync(island);
                continue;
            }

            island.depositBank(gain);
            island.setBankInterestAt(now);
            plugin.getIslandManager().saveAsync(island);
            plugin.getBankService().logAsync(island.getUniqueId(),
                    BankService.TYPE_INTEREST, null, "Interest", gain, island.getBank());
        }
    }

    private boolean ownsIsland(String localServer, Island island) {
        if (localServer == null)
            return true;
        String owner = island.getServerName();
        return owner == null || owner.equals(localServer);
    }
}
