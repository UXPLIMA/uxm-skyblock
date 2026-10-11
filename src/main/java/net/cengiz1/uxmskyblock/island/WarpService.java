package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.proxy.ProxyManager;
import net.cengiz1.uxmskyblock.util.SafeLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarpService {

    private final UxmSkyblockPlugin plugin;

    public WarpService(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Island> getWarps() {
        List<Island> result = new ArrayList<>();
        for (Island island : plugin.getIslandManager().getAllIslands())
            if (island.hasWarp())
                result.add(island);
        result.sort((a, b) -> {
            int byLevel = Integer.compare(b.getLevel(), a.getLevel());
            return byLevel != 0 ? byLevel : Double.compare(b.getPoints(), a.getPoints());
        });
        return result;
    }

    public void openWarpMenuOrWarp(Player viewer, Location signLocation) {
        Island island = plugin.getIslandManager().getIslandAt(signLocation);
        if (island != null && island.hasWarp()) {
            warp(viewer, island);
            return;
        }
        if (plugin.getMenuManager().has("warp"))
            plugin.getMenuManager().open(viewer, "warp", null);
    }

    public void warpByName(Player viewer, String targetName) {
        warpByName(viewer, targetName, null);
    }

    public void warpByName(Player viewer, String targetName, String warpName) {
        OfflinePlayer target = resolveOffline(targetName);
        if (target == null) {
            plugin.getMessages().send(viewer, "player-not-found", "{player}", targetName);
            return;
        }
        Island island = plugin.getIslandManager().getByOwner(target.getUniqueId());
        if (island == null || !island.hasWarp()) {
            plugin.getMessages().send(viewer, "warp-no-warp", "{player}", targetName);
            return;
        }
        warp(viewer, island, warpName);
    }

    public void warpToOwner(Player viewer, UUID ownerId) {
        Island island = plugin.getIslandManager().getByOwner(ownerId);
        if (island == null || !island.hasWarp()) {
            plugin.getMessages().send(viewer, "warp-no-warp", "{player}", nameOf(ownerId));
            return;
        }
        warp(viewer, island, null);
    }

    public void warp(Player viewer, Island island) {
        warp(viewer, island, null);
    }

    public void warp(Player viewer, Island island, String warpName) {
        UUID playerId = viewer.getUniqueId();
        if (island.isBanned(playerId)) {
            plugin.getMessages().send(viewer, "visit-banned");
            return;
        }
        if (!island.hasWarp()) {
            plugin.getMessages().send(viewer, "warp-no-warp", "{player}", nameOf(island.getOwner()));
            return;
        }

        if (warpName != null && !warpName.isEmpty() && !island.hasWarp(warpName)) {
            plugin.getMessages().send(viewer, "warp-unknown-name", "{name}", warpName);
            return;
        }

        ProxyManager proxy = plugin.getProxyManager();
        if (proxy != null && proxy.handleWarpTeleport(viewer, island, warpName))
            return;

        World world = plugin.getWorldManager().getWorld();
        Location warp = island.getWarp(world, warpName);
        if (!SafeLocation.isSafe(warp)) {
            plugin.getMessages().send(viewer, "warp-unsafe");
            return;
        }
        plugin.getMessages().send(viewer, "warping", "{player}", nameOf(island.getOwner()));
        plugin.getIslandManager().teleportToWarp(viewer, island, warpName);
    }

    /** Maximum number of named warps a player's island may have. */
    public int getWarpLimit(Player player) {
        int limit = plugin.getSettings().getWarpDefaultLimit();
        String prefix = plugin.getSettings().getWarpPermissionPrefix();
        if (player.hasPermission(prefix + ".*"))
            return Integer.MAX_VALUE;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue())
                continue;
            String perm = info.getPermission();
            if (perm.length() <= prefix.length() + 1 || !perm.startsWith(prefix + "."))
                continue;
            try {
                int n = Integer.parseInt(perm.substring(prefix.length() + 1));
                if (n > limit)
                    limit = n;
            } catch (NumberFormatException ignored) {
            }
        }
        return limit;
    }

    private OfflinePlayer resolveOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null)
            return online;
        OfflinePlayer offline = net.cengiz1.uxmskyblock.util.PlayerNames.offline(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private String nameOf(UUID id) {
        String name = net.cengiz1.uxmskyblock.util.PlayerNames.nameOf(id);
        return name != null ? name : id.toString().substring(0, 8);
    }
}
