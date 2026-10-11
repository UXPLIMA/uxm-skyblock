package net.cengiz1.uxmskyblock.level;

import net.cengiz1.uxmskyblock.storage.PlacedChange;
import net.cengiz1.uxmskyblock.storage.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Blocks players placed on islands. Only these count toward points when broken. */
public class PlacedBlocks {

    private static final int XZ_OFFSET = 1 << 25;
    private static final int Y_OFFSET = 2048;

    private final Map<UUID, Map<Long, String>> placed = new ConcurrentHashMap<>();
    private final Queue<PlacedChange> pending = new ConcurrentLinkedQueue<>();

    public static long key(int x, int y, int z) {
        return ((long) (x + XZ_OFFSET) << 38) | ((long) (z + XZ_OFFSET) << 12) | (long) (y + Y_OFFSET);
    }

    public void track(UUID islandId, long key, String material) {
        placed.computeIfAbsent(islandId, id -> new ConcurrentHashMap<>()).put(key, material);
        pending.add(new PlacedChange(islandId, key, material, true));
    }

    /** Removes the block from the placed set and returns its material, or null if it was never placed. */
    public String untrack(UUID islandId, long key) {
        Map<Long, String> blocks = placed.get(islandId);
        if (blocks == null)
            return null;
        String material = blocks.remove(key);
        if (material != null)
            pending.add(new PlacedChange(islandId, key, material, false));
        return material;
    }

    public Map<String, Integer> countsByMaterial(UUID islandId) {
        Map<String, Integer> counts = new HashMap<>();
        Map<Long, String> blocks = placed.get(islandId);
        if (blocks != null)
            for (String material : blocks.values())
                counts.merge(material, 1, Integer::sum);
        return counts;
    }

    public void load(Map<UUID, Map<Long, String>> loaded) {
        for (Map.Entry<UUID, Map<Long, String>> entry : loaded.entrySet())
            placed.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
    }

    public void forget(UUID islandId) {
        placed.remove(islandId);
    }

    public void flush(Storage storage) {
        List<PlacedChange> batch = new ArrayList<>();
        PlacedChange change;
        while ((change = pending.poll()) != null) {
            if (placed.containsKey(change.island()))
                batch.add(change);
        }
        if (!batch.isEmpty())
            storage.applyPlaced(batch);
    }
}
