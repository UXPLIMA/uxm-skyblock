package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class BiomeService {

    private final UxmSkyblockPlugin plugin;
    private final Set<UUID> applying = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public BiomeService(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> getAllowed() {
        List<String> configured = plugin.getConfig().getStringList("biome.allowed");
        List<String> result = new ArrayList<>();
        for (String name : configured) {
            if (resolve(name) != null)
                result.add(name.toUpperCase(Locale.ROOT));
        }
        return result;
    }

    public Biome resolve(String name) {
        if (name == null)
            return null;
        try {
            return Biome.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public boolean isAllowed(String name) {
        for (String allowed : getAllowed())
            if (allowed.equalsIgnoreCase(name))
                return true;
        return false;
    }

    public void apply(Player player, Island island, Biome biome, String label) {
        World world = plugin.getWorldManager().getWorld();
        if (world == null) {
            plugin.getMessages().send(player, "biome-unavailable");
            return;
        }
        UUID islandId = island.getUniqueId();
        if (!applying.add(islandId)) {
            plugin.getMessages().send(player, "biome-in-progress");
            return;
        }

        int half = plugin.getIslandManager().getProtectionHalf(island);
        int minX = island.getCenterX() - half;
        int maxX = island.getCenterX() + half;
        int minZ = island.getCenterZ() - half;
        int maxZ = island.getCenterZ() + half;

        int bandBelow = Math.max(0, plugin.getConfig().getInt("biome.band-below", 40));
        int bandAbove = Math.max(0, plugin.getConfig().getInt("biome.band-above", 72));
        int minY = Math.max(world.getMinHeight(), island.getCenterY() - bandBelow);
        int maxY = Math.min(world.getMaxHeight() - 1, island.getCenterY() + bandAbove);

        List<int[]> columns = new ArrayList<>();
        for (int x = minX; x <= maxX; x += 4)
            for (int z = minZ; z <= maxZ; z += 4)
                columns.add(new int[]{x, z});

        int perTick = Math.max(16, plugin.getConfig().getInt("biome.columns-per-tick", 256));
        Set<Long> chunks = new HashSet<>();

        island.setBiome(label);
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "biome-applying", "{biome}", label);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < columns.size() && processed < perTick) {
                    int[] col = columns.get(index++);
                    for (int y = minY; y <= maxY; y += 4)
                        world.setBiome(col[0], y, col[1], biome);
                    chunks.add(chunkKey(col[0] >> 4, col[1] >> 4));
                    processed++;
                }
                if (index >= columns.size()) {
                    for (long key : chunks) {
                        int cx = (int) (key >> 32);
                        int cz = (int) (key & 0xffffffffL);
                        if (world.isChunkLoaded(cx, cz))
                            world.refreshChunk(cx, cz);
                    }
                    applying.remove(islandId);
                    Player online = player.isOnline() ? player : null;
                    if (online != null)
                        plugin.getMessages().send(online, "biome-done", "{biome}", label);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }
}
