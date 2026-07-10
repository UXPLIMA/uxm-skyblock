package net.cengiz1.uxmskyblock.command;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.economy.EconomyHook;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandPermission;
import org.bukkit.entity.Player;

/**
 * Island bank: a shared balance stored on the island. Members deposit from their
 * own Vault wallet into the bank; members with the BANK permission can withdraw
 * back into their wallet. Requires a Vault economy (economy.enabled).
 */
public class BankCommands extends CommandHandler {

    public BankCommands(UxmSkyblockPlugin plugin) {
        super(plugin);
    }

    public void handle(Player player, String action, String amountArg) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }

        if (action == null || action.equalsIgnoreCase("balance")
                || action.equalsIgnoreCase("bakiye") || action.equalsIgnoreCase("info")) {
            plugin.getMessages().send(player, "bank-balance", "{amount}", formatNumber(island.getBank()));
            return;
        }

        if (action.equalsIgnoreCase("log") || action.equalsIgnoreCase("gecmis")
                || action.equalsIgnoreCase("geçmiş") || action.equalsIgnoreCase("history")) {
            showLog(player, island);
            return;
        }

        EconomyHook economy = plugin.getEconomy();
        if (economy == null || !economy.isEnabled()) {
            plugin.getMessages().send(player, "bank-no-economy");
            return;
        }

        boolean deposit = action.equalsIgnoreCase("deposit")
                || action.equalsIgnoreCase("yatir") || action.equalsIgnoreCase("yatır");
        boolean withdraw = action.equalsIgnoreCase("withdraw")
                || action.equalsIgnoreCase("cek") || action.equalsIgnoreCase("çek");

        if (!deposit && !withdraw) {
            plugin.getMessages().send(player, "bank-usage");
            return;
        }

        double amount = parseAmount(amountArg);
        if (amount <= 0) {
            plugin.getMessages().send(player, "bank-invalid-amount");
            return;
        }

        if (deposit) {
            if (!economy.has(player, amount)) {
                plugin.getMessages().send(player, "bank-need-money");
                return;
            }
            if (!economy.withdraw(player, amount)) {
                plugin.getMessages().send(player, "bank-need-money");
                return;
            }
            island.depositBank(amount);
            plugin.getIslandManager().saveAsync(island);
            plugin.getBankService().logAsync(island.getUniqueId(), net.cengiz1.uxmskyblock.island.BankService.TYPE_DEPOSIT,
                    player.getUniqueId(), player.getName(), amount, island.getBank());
            plugin.getMessages().send(player, "bank-deposit",
                    "{amount}", formatNumber(amount), "{balance}", formatNumber(island.getBank()));
            return;
        }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.BANK)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        if (island.getBank() < amount) {
            plugin.getMessages().send(player, "bank-insufficient", "{amount}", formatNumber(island.getBank()));
            return;
        }
        double taken = island.withdrawBank(amount);
        if (!economy.deposit(player, taken)) {
            island.depositBank(taken);
            plugin.getMessages().send(player, "bank-failed");
            return;
        }
        plugin.getIslandManager().saveAsync(island);
        plugin.getBankService().logAsync(island.getUniqueId(), net.cengiz1.uxmskyblock.island.BankService.TYPE_WITHDRAW,
                player.getUniqueId(), player.getName(), taken, island.getBank());
        plugin.getMessages().send(player, "bank-withdraw",
                "{amount}", formatNumber(taken), "{balance}", formatNumber(island.getBank()));
    }

    private void showLog(Player player, Island island) {
        int lines = Math.max(1, plugin.getConfig().getInt("bank.log.lines", 10));
        plugin.getBankService().recentAsync(island.getUniqueId(), lines, entries -> {
            if (entries.isEmpty()) {
                plugin.getMessages().send(player, "bank-log-empty");
                return;
            }
            plugin.getMessages().send(player, "bank-log-header");
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd/MM HH:mm");
            for (net.cengiz1.uxmskyblock.storage.BankLogEntry entry : entries) {
                plugin.getMessages().send(player, "bank-log-entry",
                        "{time}", format.format(new java.util.Date(entry.getTimestamp())),
                        "{type}", entry.getType(),
                        "{actor}", entry.getActorName() == null ? "-" : entry.getActorName(),
                        "{amount}", formatNumber(entry.getAmount()),
                        "{balance}", formatNumber(entry.getBalance()));
            }
        });
    }

    private double parseAmount(String arg) {
        if (arg == null)
            return -1;
        try {
            return Double.parseDouble(arg.trim().replace(",", "."));
        } catch (NumberFormatException error) {
            return -1;
        }
    }
}
