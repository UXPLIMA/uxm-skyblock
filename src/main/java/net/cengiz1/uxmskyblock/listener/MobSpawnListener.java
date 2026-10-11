package net.cengiz1.uxmskyblock.listener;

import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandFlag;
import net.cengiz1.uxmskyblock.island.IslandManager;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobSpawnListener implements Listener {

    private final IslandManager islandManager;

    public MobSpawnListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
            return;
        if (!(event.getEntity() instanceof Enemy))
            return;

        Island island = islandManager.getIslandAt(event.getLocation());
        if (island != null && !island.getFlag(IslandFlag.MOB_SPAWNING))
            event.setCancelled(true);
    }
}
