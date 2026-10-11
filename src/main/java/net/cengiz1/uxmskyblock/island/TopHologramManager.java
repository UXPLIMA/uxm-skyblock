package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A personal floating leaderboard hologram (a {@link TextDisplay}) that follows
 * the player around and is only visible to them. Toggled with /island top holo.
 * Repositions every couple of ticks so it follows smoothly; refreshes its text
 * on a slower interval.
 */
public class TopHologramManager implements Listener {

    private final UxmSkyblockPlugin plugin;
    private final TopService topService;
    private final Map<UUID, TextDisplay> active = new ConcurrentHashMap<>();

    private final int limit;
    private final double height;
    private final int refreshTicks;
    private final String title;
    private final String entryFormat;
    private final String emptyLine;

    private BukkitTask task;
    private int tickCounter;

    public TopHologramManager(UxmSkyblockPlugin plugin, TopService topService) {
        this.plugin = plugin;
        this.topService = topService;
        this.limit = Math.max(1, plugin.getConfig().getInt("top.hologram.lines", 10));
        this.height = plugin.getConfig().getDouble("top.hologram.height", 2.8);
        this.refreshTicks = Math.max(20, plugin.getConfig().getInt("top.hologram.refresh-ticks", 40));
        this.title = plugin.getConfig().getString("top.hologram.title", "&6&lTOP ISLANDS");
        this.entryFormat = plugin.getConfig().getString("top.hologram.entry", "&7{rank}. &f{owner} &8- &a{level}");
        this.emptyLine = plugin.getConfig().getString("top.hologram.empty", "&7No islands yet.");
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 2L);
    }

    public void stop() {
        if (this.task != null)
            this.task.cancel();
        for (UUID playerId : new ArrayList<>(active.keySet()))
            remove(playerId);
    }

    /** Toggle the hologram for a player. Returns true if it is now shown. */
    public boolean toggle(Player player) {
        if (active.containsKey(player.getUniqueId())) {
            remove(player.getUniqueId());
            return false;
        }
        TextDisplay display = spawnEntity(player);
        active.put(player.getUniqueId(), display);
        return true;
    }

    private void remove(UUID playerId) {
        TextDisplay display = active.remove(playerId);
        if (display != null && display.isValid())
            display.remove();
    }

    private TextDisplay spawnEntity(Player player) {
        String text = buildText();
        TextDisplay display = player.getWorld().spawn(followLocation(player), TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
            entity.setSeeThrough(true);
            entity.setText(text);
        });

        for (Player other : Bukkit.getOnlinePlayers())
            if (!other.getUniqueId().equals(player.getUniqueId()))
                other.hideEntity(plugin, display);
        return display;
    }

    private void tick() {
        if (active.isEmpty())
            return;

        tickCounter += 2;
        boolean refresh = tickCounter >= refreshTicks;
        if (refresh)
            tickCounter = 0;
        String text = refresh ? buildText() : null;

        for (Map.Entry<UUID, TextDisplay> entry : active.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                TextDisplay stale = entry.getValue();
                if (stale != null && stale.isValid())
                    stale.remove();
                active.remove(entry.getKey());
                continue;
            }

            TextDisplay display = entry.getValue();
            if (display == null || !display.isValid()) {
                display = spawnEntity(player);
                entry.setValue(display);
            }
            display.teleport(followLocation(player));
            if (refresh)
                display.setText(text);
        }
    }

    private Location followLocation(Player player) {
        return player.getLocation().add(0, height, 0);
    }

    private String buildText() {
        List<Island> top = topService.getTop(limit);
        StringBuilder builder = new StringBuilder(color(title));
        if (top.isEmpty()) {
            builder.append('\n').append(color(emptyLine));
            return builder.toString();
        }
        int rank = 1;
        for (Island island : top) {
            builder.append('\n').append(color(entryFormat
                    .replace("{rank}", String.valueOf(rank++))
                    .replace("{owner}", ownerName(island))
                    .replace("{level}", String.valueOf(island.getLevel()))
                    .replace("{points}", formatNumber(island.getPoints()))));
        }
        return builder.toString();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }

    private String ownerName(Island island) {
        String name = net.cengiz1.uxmskyblock.util.PlayerNames.nameOf(island.getOwner());
        return name != null ? name : island.getOwner().toString().substring(0, 8);
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
