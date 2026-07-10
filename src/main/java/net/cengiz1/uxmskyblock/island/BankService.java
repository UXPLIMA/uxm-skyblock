package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.storage.BankLogEntry;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class BankService {

    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_WITHDRAW = "WITHDRAW";
    public static final String TYPE_INTEREST = "INTEREST";

    private final UxmSkyblockPlugin plugin;

    public BankService(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void logAsync(UUID islandId, String type, UUID actor, String actorName, double amount, double balance) {
        BankLogEntry entry = new BankLogEntry(System.currentTimeMillis(), type, actor, actorName, amount, balance);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getStorage().appendBankLog(islandId, entry));
    }

    public void logDirect(UUID islandId, String type, UUID actor, String actorName, double amount, double balance) {
        BankLogEntry entry = new BankLogEntry(System.currentTimeMillis(), type, actor, actorName, amount, balance);
        plugin.getStorage().appendBankLog(islandId, entry);
    }

    public void recentAsync(UUID islandId, int limit, Consumer<List<BankLogEntry>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BankLogEntry> entries = plugin.getStorage().loadBankLog(islandId, limit);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(entries));
        });
    }
}
