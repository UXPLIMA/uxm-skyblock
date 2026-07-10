package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RatingService {

    private final UxmSkyblockPlugin plugin;
    private final Map<UUID, double[]> cache = new ConcurrentHashMap<>();

    public RatingService(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
        long period = 20L * 60L * 5L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshCache, 20L * 15L, period);
    }

    private void refreshCache() {
        for (Island island : plugin.getIslandManager().getAllIslands()) {
            double[] value = plugin.getStorage().loadRating(island.getUniqueId());
            cache.put(island.getUniqueId(), value);
        }
    }

    public double getAverage(UUID islandId) {
        double[] value = cache.get(islandId);
        return value == null ? 0 : value[0];
    }

    public int getCount(UUID islandId) {
        double[] value = cache.get(islandId);
        return value == null ? 0 : (int) value[1];
    }

    public void rate(UUID rater, Island island, int rating, Runnable onSuccess) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStorage().saveRating(island.getUniqueId(), rater, rating);
            double[] value = plugin.getStorage().loadRating(island.getUniqueId());
            cache.put(island.getUniqueId(), value);
            Bukkit.getScheduler().runTask(plugin, onSuccess);
        });
    }

    public void topAsync(int limit, int minVotes, Consumer<List<UUID>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<UUID> ids = plugin.getStorage().loadTopRated(limit, minVotes);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(ids));
        });
    }
}
