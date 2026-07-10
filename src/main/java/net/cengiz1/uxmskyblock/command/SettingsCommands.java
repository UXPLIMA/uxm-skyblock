package net.cengiz1.uxmskyblock.command;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandPermission;
import org.bukkit.entity.Player;

public class SettingsCommands extends CommandHandler {

    public SettingsCommands(UxmSkyblockPlugin plugin) {
        super(plugin);
    }

    public void openIslandMenu(Player player, String menuId) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        if (!plugin.getMenuManager().has(menuId)) {
            plugin.getMessages().send(player, "unknown-subcommand");
            return;
        }
        plugin.getMenuManager().open(player, menuId, island.getUniqueId());
    }

    public void setSpawn(Player player) {
        Island island = requirePermission(player, IslandPermission.SET_HOME);
        if (island == null)
            return;
        if (plugin.getIslandManager().getIslandAt(player.getLocation()) != island) {
            plugin.getMessages().send(player, "must-be-on-island");
            return;
        }
        island.setHome(player.getLocation());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "spawn-set");
    }

    public void toggleFly(Player player) {
        Island island = requirePermission(player, IslandPermission.FLY);
        if (island == null)
            return;
        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);
        player.setFlying(enable);
        plugin.getMessages().send(player, enable ? "fly-on" : "fly-off");
    }

    public void setBorderColor(Player player, String colorArg) {
        Island island = requirePermission(player, IslandPermission.TOGGLE_SETTINGS);
        if (island == null)
            return;
        if (colorArg == null) {
            plugin.getMessages().send(player, "border-usage");
            return;
        }
        String color = colorArg.trim().toUpperCase(java.util.Locale.ROOT);
        if (!color.equals("BLUE") && !color.equals("GREEN") && !color.equals("RED")) {
            plugin.getMessages().send(player, "border-invalid");
            return;
        }
        island.setBorderColor(color);
        plugin.getIslandManager().saveAsync(island);
        if (plugin.getIslandManager().getBorderManager() != null)
            plugin.getIslandManager().getBorderManager().refresh(island);
        plugin.getMessages().send(player, "border-set", "{color}", color);
    }

    public void setBiome(Player player, String biomeArg) {
        Island island = requirePermission(player, IslandPermission.TOGGLE_SETTINGS);
        if (island == null)
            return;

        net.cengiz1.uxmskyblock.proxy.ProxyManager proxy = plugin.getProxyManager();
        if (proxy != null && proxy.isEnabled() && island.getServerName() != null
                && !island.getServerName().equals(proxy.getServerName())) {
            plugin.getMessages().send(player, "biome-wrong-server", "{server}", island.getServerName());
            return;
        }

        if (biomeArg == null) {
            plugin.getMessages().send(player, "biome-usage",
                    "{list}", String.join(", ", plugin.getBiomeService().getAllowed()));
            return;
        }
        String label = biomeArg.trim().toUpperCase(java.util.Locale.ROOT);
        if (!plugin.getBiomeService().isAllowed(label)) {
            plugin.getMessages().send(player, "biome-invalid",
                    "{list}", String.join(", ", plugin.getBiomeService().getAllowed()));
            return;
        }
        org.bukkit.block.Biome biome = plugin.getBiomeService().resolve(label);
        plugin.getBiomeService().apply(player, island, biome, label);
    }

    public void toggleLock(Player player) {
        Island island = requirePermission(player, IslandPermission.TOGGLE_SETTINGS);
        if (island == null)
            return;
        island.setLocked(!island.isLocked());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, island.isLocked() ? "island-locked" : "island-unlocked");
    }
}
