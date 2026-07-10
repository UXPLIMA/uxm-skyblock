package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Locale;

public class LimitManager implements Listener {

    private final UxmSkyblockPlugin plugin;

    public LimitManager(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public int blockCap(Island island, String material) {
        int base = plugin.getConfig().getInt("limits.blocks." + material.toUpperCase(Locale.ROOT), -1);
        if (base < 0)
            return -1;
        return scaled(island, base);
    }

    public int entityCap(Island island, EntityType type) {
        int base = plugin.getConfig().getInt("limits.entities." + type.name(), -1);
        if (base < 0)
            return -1;
        return scaled(island, base);
    }

    private int scaled(Island island, int base) {
        String upgradeKey = plugin.getConfig().getString("limits.upgrade-key", "");
        if (upgradeKey == null || upgradeKey.isEmpty())
            return base;
        double bonusPerLevel = plugin.getConfig().getDouble("limits.upgrade-bonus-per-level", 0);
        int level = Math.max(1, island.getUpgradeLevel(upgradeKey));
        return base + (int) Math.round(bonusPerLevel * (level - 1));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        String material = event.getBlock().getType().name();
        Island island = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation());
        if (island == null)
            return;
        int cap = blockCap(island, material);
        if (cap < 0)
            return;
        if (island.getBlockLimitCount(material) >= cap) {
            plugin.getMessages().send(event.getPlayer(), "limit-block-reached",
                    "{block}", material, "{limit}", String.valueOf(cap));
            event.setCancelled(true);
            return;
        }
        island.addBlockLimitCount(material, 1);
        plugin.getIslandManager().saveAsync(island);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        String material = event.getBlock().getType().name();
        Island island = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation());
        if (island == null)
            return;
        if (blockCap(island, material) < 0)
            return;
        if (island.getBlockLimitCount(material) > 0) {
            island.addBlockLimitCount(material, -1);
            plugin.getIslandManager().saveAsync(island);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM)
            return;
        Island island = plugin.getIslandManager().getIslandAt(event.getLocation());
        if (island == null)
            return;
        EntityType type = event.getEntityType();
        int cap = entityCap(island, type);
        if (cap < 0)
            return;
        if (countEntities(island, type) >= cap)
            event.setCancelled(true);
    }

    private int countEntities(Island island, EntityType type) {
        Location center = island.getCenter(plugin.getWorldManager().getWorld());
        if (center.getWorld() == null)
            return 0;
        int half = plugin.getIslandManager().getProtectionHalf(island);
        int count = 0;
        for (Entity entity : center.getWorld().getEntities()) {
            if (entity.getType() != type)
                continue;
            Location loc = entity.getLocation();
            if (Math.abs(loc.getBlockX() - island.getCenterX()) <= half
                    && Math.abs(loc.getBlockZ() - island.getCenterZ()) <= half)
                count++;
        }
        return count;
    }
}
