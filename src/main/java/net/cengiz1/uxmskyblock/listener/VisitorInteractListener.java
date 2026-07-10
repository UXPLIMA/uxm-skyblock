package net.cengiz1.uxmskyblock.listener;

import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandFlag;
import net.cengiz1.uxmskyblock.island.IslandManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class VisitorInteractListener implements Listener {

    private final IslandManager islandManager;

    public VisitorInteractListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;

        Island island = islandManager.getIslandAt(event.getClickedBlock().getLocation());
        if (island == null)
            return;

        if (!island.hasAccess(event.getPlayer().getUniqueId())
                && !island.getFlag(IslandFlag.VISITOR_INTERACT))
            event.setCancelled(true);
    }
}
