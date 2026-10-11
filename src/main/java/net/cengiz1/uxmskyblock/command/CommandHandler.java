package net.cengiz1.uxmskyblock.command;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandPermission;
import net.cengiz1.uxmskyblock.island.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.StringJoiner;
import java.util.UUID;

public abstract class CommandHandler {

    protected final UxmSkyblockPlugin plugin;

    protected CommandHandler(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    protected Island requirePermission(Player player, IslandPermission permission) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return null;
        }
        if (!island.hasPermission(player.getUniqueId(), permission)) {
            plugin.getMessages().send(player, "no-island-permission");
            return null;
        }
        return island;
    }

    protected OfflinePlayer resolveOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null)
            return online;
        OfflinePlayer offline = net.cengiz1.uxmskyblock.util.PlayerNames.offline(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    protected String nameOf(UUID id) {
        String name = net.cengiz1.uxmskyblock.util.PlayerNames.nameOf(id);
        return name != null ? name : id.toString().substring(0, 8);
    }

    protected String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }

    protected String joinTypes() {
        StringJoiner joiner = new StringJoiner(", ");
        plugin.getIslandManager().getSchematicService().getDefinitions()
                .forEach(definition -> joiner.add(definition.getKey()));
        return joiner.toString();
    }

    protected String joinRoles() {
        StringJoiner joiner = new StringJoiner(", ");
        for (net.cengiz1.uxmskyblock.island.RoleData role : plugin.getRoleManager().assignableBuiltins())
            joiner.add(role.getId());
        return joiner.toString();
    }
}
