package net.cengiz1.uxmskyblock.placeholder;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandFlag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared resolver for the {@code %skyblock_...%} placeholders. Used both by the
 * PlaceholderAPI expansion ({@link UxmSkyblockExpansion}) and directly by the menu
 * system so menus keep rendering correctly even when PlaceholderAPI is missing
 * or has not registered the expansion yet.
 */
public final class UxmSkyblockPlaceholders {

    private static final Pattern TOKEN = Pattern.compile("%skyblock_([a-zA-Z0-9_]+)%");

    private UxmSkyblockPlaceholders() {
    }

    /**
     * Resolve a single {@code skyblock_<key>} placeholder (without the % signs),
     * or {@code null} if the key is unknown.
     */
    public static String resolve(UxmSkyblockPlugin plugin, OfflinePlayer player, String params) {
        if (player == null)
            return "";

        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        String key = params.toLowerCase(java.util.Locale.ROOT);

        if (key.equals("has_island"))
            return bool(plugin, island != null);

        if (key.equals("total_islands") || key.equals("island_count"))
            return String.valueOf(plugin.getIslandManager().getAllIslands().size());

        if (key.startsWith("top_"))
            return resolveTop(plugin, key.substring("top_".length()));

        if (island == null)
            return noIsland(plugin, key);

        if (key.startsWith("flag_")) {
            IslandFlag flag = IslandFlag.fromString(key.substring("flag_".length()));
            if (flag == null)
                return null;
            return flagState(plugin, island.getFlag(flag));
        }

        if (key.startsWith("upgrade_"))
            return String.valueOf(island.getUpgradeLevel(key.substring("upgrade_".length())));

        switch (key) {
            case "level":
                return String.valueOf(island.getLevel());
            case "next_level":
                return String.valueOf(island.getLevel() + 1);
            case "points":
                return formatNumber(island.getPoints());
            case "bank":
                return formatNumber(island.getBank());
            case "biome":
                return island.getBiome() != null ? island.getBiome() : "";
            case "rating":
                return String.format(java.util.Locale.US, "%.1f",
                        plugin.getRatingService().getAverage(island.getUniqueId()));
            case "rating_count":
                return String.valueOf(plugin.getRatingService().getCount(island.getUniqueId()));
            case "coop":
                return String.valueOf(island.getCoop().size());
            case "next_points": {
                double next = plugin.getLevelManager().pointsForNextLevel(island.getLevel());
                return next < 0 ? "MAX" : formatNumber(next);
            }
            case "points_needed": {
                double next = plugin.getLevelManager().pointsForNextLevel(island.getLevel());
                if (next < 0)
                    return "0";
                return formatNumber(Math.max(0, next - island.getPoints()));
            }
            case "progress":
                return String.valueOf(progressPercent(plugin, island));
            case "progress_bar":
                return progressBar(progressPercent(plugin, island));
            case "members":
                return String.valueOf(island.getMemberCount());
            case "team_limit":
                return String.valueOf((int) plugin.getUpgradeManager().getValue(island, "team-limit", 4));
            case "team_free": {
                int limit = (int) plugin.getUpgradeManager().getValue(island, "team-limit", 4);
                return String.valueOf(Math.max(0, limit - island.getMemberCount()));
            }
            case "owner":
                return nameOf(island);
            case "name":
                return island.getName() != null ? island.getName() : nameOf(island);
            case "rank":
                return island.getRole(player.getUniqueId()).getDisplayName();
            case "top": {
                int top = plugin.getTopService().getRank(island);
                return top < 0 ? "-" : String.valueOf(top);
            }
            case "is_owner":
                return bool(plugin, island.isOwner(player.getUniqueId()));
            case "visitors":
                return island.isLocked()
                        ? plugin.getMessages().get("visit-closed")
                        : plugin.getMessages().get("visit-open");
            case "locked":
                return bool(plugin, island.isLocked());
            case "has_warp":
                return bool(plugin, island.hasWarp());
            case "warps":
                return String.valueOf(island.getWarpCount());
            case "banned":
                return String.valueOf(island.getBanned().size());
            case "border":
                return island.getBorderColor() != null ? island.getBorderColor() : "";
            case "time":
                return island.getTime().name();
            case "grid_index":
                return String.valueOf(island.getGridIndex());
            case "id":
                return island.getUniqueId().toString();
            case "server":
                return island.getServerName() != null ? island.getServerName() : "";
            default:
                return null;
        }
    }

    /**
     * Replace every {@code %skyblock_...%} token in {@code text} using the
     * internal resolver. Unknown skyblock tokens and all non-skyblock
     * placeholders are left untouched so PlaceholderAPI can still process them.
     */
    public static String apply(UxmSkyblockPlugin plugin, OfflinePlayer player, String text) {
        if (text == null)
            return "";
        if (text.indexOf('%') < 0)
            return text;

        Matcher matcher = TOKEN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String value = resolve(plugin, player, matcher.group(1));
            matcher.appendReplacement(buffer,
                    Matcher.quoteReplacement(value == null ? matcher.group(0) : value));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String noIsland(UxmSkyblockPlugin plugin, String key) {
        switch (key) {
            case "level":
            case "next_level":
            case "members":
            case "team_limit":
            case "team_free":
            case "points":
            case "next_points":
            case "points_needed":
            case "progress":
            case "bank":
            case "warps":
            case "banned":
            case "grid_index":
                return "0";
            case "top":
                return "-";
            case "owner":
            case "name":
            case "rank":
            case "server":
            case "border":
            case "time":
            case "id":
            case "progress_bar":
                return "";
            case "visitors":
            case "locked":
            case "has_warp":
            case "is_owner":
                return bool(plugin, false);
            default:
                if (key.startsWith("flag_"))
                    return flagState(plugin, false);
                if (key.startsWith("upgrade_"))
                    return "0";
                return "";
        }
    }

    private static String resolveTop(UxmSkyblockPlugin plugin, String rest) {
        String positionPart;
        String field;
        int underscore = rest.indexOf('_');
        if (underscore < 0) {
            positionPart = rest;
            field = "name";
        } else {
            positionPart = rest.substring(0, underscore);
            field = rest.substring(underscore + 1);
        }

        int position;
        try {
            position = Integer.parseInt(positionPart);
        } catch (NumberFormatException error) {
            return null;
        }
        if (position < 1)
            return null;

        Island island = plugin.getTopService().getAt(position);
        if (island == null)
            return emptyTop(field);

        switch (field) {
            case "name":
                return island.getName() != null ? island.getName() : nameOf(island);
            case "owner":
                return nameOf(island);
            case "level":
                return String.valueOf(island.getLevel());
            case "points":
            case "point":
                return formatNumber(island.getPoints());
            case "bank":
                return formatNumber(island.getBank());
            case "members":
                return String.valueOf(island.getMemberCount());
            default:
                return null;
        }
    }

    private static String emptyTop(String field) {
        switch (field) {
            case "level":
            case "points":
            case "point":
            case "bank":
            case "members":
                return "0";
            default:
                return "-";
        }
    }

    private static int progressPercent(UxmSkyblockPlugin plugin, Island island) {
        double current = plugin.getLevelManager().requiredPoints(island.getLevel());
        double next = plugin.getLevelManager().requiredPoints(island.getLevel() + 1);
        if (next == Double.MAX_VALUE || next <= current)
            return 100;
        double ratio = (island.getPoints() - current) / (next - current);
        if (ratio < 0)
            ratio = 0;
        if (ratio > 1)
            ratio = 1;
        return (int) Math.round(ratio * 100);
    }

    private static String progressBar(int percent) {
        int filled = Math.round(percent / 10f);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++)
            bar.append(i < filled ? "&a▮" : "&7▯");
        return bar.toString();
    }

    private static String flagState(UxmSkyblockPlugin plugin, boolean value) {
        return value
                ? plugin.getMessages().get("flag-on")
                : plugin.getMessages().get("flag-off");
    }

    private static String bool(UxmSkyblockPlugin plugin, boolean value) {
        return value
                ? plugin.getMessages().get("placeholder-yes")
                : plugin.getMessages().get("placeholder-no");
    }

    private static String nameOf(Island island) {
        String name = Bukkit.getOfflinePlayer(island.getOwner()).getName();
        return name != null ? name : island.getOwner().toString().substring(0, 8);
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }
}
