package net.cengiz1.uxmskyblock.listener;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandManager;
import net.cengiz1.uxmskyblock.island.IslandPermission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ObsidianBucketListener implements Listener {

    private final UxmSkyblockPlugin plugin;
    private final IslandManager islandManager;

    public ObsidianBucketListener(UxmSkyblockPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND)
            return;
        if (!plugin.getSettings().isObsidianBucketToLava())
            return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.OBSIDIAN)
            return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.BUCKET)
            return;

        Island island = islandManager.getIslandAt(block.getLocation());
        if (island != null && !island.hasPermission(player.getUniqueId(), IslandPermission.BLOCK_BREAK))
            return;

        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled())
            return;

        event.setCancelled(true);
        block.setType(Material.AIR);
        giveLavaBucket(player, hand);
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1.0f, 1.0f);
    }

    private void giveLavaBucket(Player player, ItemStack hand) {
        ItemStack lava = new ItemStack(Material.LAVA_BUCKET);
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(lava);
            return;
        }
        ItemStack remaining = hand.clone();
        remaining.setAmount(hand.getAmount() - 1);
        player.getInventory().setItemInMainHand(remaining);
        for (ItemStack leftover : player.getInventory().addItem(lava).values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }
}
