package net.cengiz1.uxmskyblock.listener;

import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandFlag;
import net.cengiz1.uxmskyblock.island.IslandManager;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosionListener implements Listener {

    private final IslandManager islandManager;

    public ExplosionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Island island = islandManager.getIslandAt(event.getLocation());
        if (island == null)
            return;

        boolean creeper = event.getEntity() instanceof Creeper;
        boolean tnt = event.getEntity() instanceof TNTPrimed || event.getEntity() instanceof ExplosiveMinecart;

        if ((creeper && !island.getFlag(IslandFlag.CREEPER_EXPLOSION))
                || (tnt && !island.getFlag(IslandFlag.TNT_EXPLOSION)))
            event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Island island = islandManager.getIslandAt(event.getBlock().getLocation());
        if (island != null && !island.getFlag(IslandFlag.TNT_EXPLOSION))
            event.blockList().clear();
    }
}
