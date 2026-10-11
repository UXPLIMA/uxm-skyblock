package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.config.SettingsManager;
import net.cengiz1.uxmskyblock.proxy.ProxyManager;
import net.cengiz1.uxmskyblock.schematic.FaweSchematicService;
import net.cengiz1.uxmskyblock.schematic.SchematicService;
import net.cengiz1.uxmskyblock.storage.Storage;
import net.cengiz1.uxmskyblock.upgrade.UpgradeManager;
import net.cengiz1.uxmskyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandManager {

    private final UxmSkyblockPlugin plugin;
    private final SettingsManager settings;
    private final Storage storage;
    private final WorldManager worldManager;
    private final IslandGrid grid;
    private final SchematicService schematicService;
    private final IslandCreationService creationService;

    private final Map<UUID, Island> islandsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerToIsland = new ConcurrentHashMap<>();
    private final Map<Long, UUID> cellToIsland = new ConcurrentHashMap<>();

    private UpgradeManager upgradeManager;

    private ProxyManager proxyManager;

    private String localServerName;

    private BorderManager borderManager;

    public IslandManager(UxmSkyblockPlugin plugin, SettingsManager settings, Storage storage, WorldManager worldManager) {
        this.plugin = plugin;
        this.settings = settings;
        this.storage = storage;
        this.worldManager = worldManager;
        this.grid = new IslandGrid(settings);
        this.schematicService = new FaweSchematicService(plugin);
        this.creationService = new IslandCreationService(plugin, settings, storage, worldManager, this, this.schematicService);
    }

    private final net.cengiz1.uxmskyblock.level.PlacedBlocks placedBlocks = new net.cengiz1.uxmskyblock.level.PlacedBlocks();

    public net.cengiz1.uxmskyblock.level.PlacedBlocks getPlacedBlocks() {
        return placedBlocks;
    }

    public void loadAll() {
        int highestIndex = -1;
        for (Island island : this.storage.loadAll()) {
            this.islandsById.put(island.getUniqueId(), island);
            this.ownerToIsland.put(island.getOwner(), island.getUniqueId());
            this.cellToIsland.put(cellOfCenter(island.getCenterX(), island.getCenterZ()), island.getUniqueId());
            if (island.getGridIndex() > highestIndex)
                highestIndex = island.getGridIndex();
        }
        this.placedBlocks.load(this.storage.loadAllPlaced());
        this.grid.setNextIndex(highestIndex + 1);
        plugin.getLogger().info("Loaded " + this.islandsById.size() + " islands.");
    }

    public IslandGrid getGrid() {
        return grid;
    }

    public IslandCreationService getCreationService() {
        return creationService;
    }

    public SchematicService getSchematicService() {
        return schematicService;
    }

    public Island getByOwner(UUID owner) {
        UUID islandId = this.ownerToIsland.get(owner);
        return islandId == null ? null : this.islandsById.get(islandId);
    }

    public Island getById(UUID islandId) {
        return this.islandsById.get(islandId);
    }

    public void setUpgradeManager(UpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    public void setProxyManager(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public void setLocalServerName(String localServerName) {
        this.localServerName = localServerName;
    }

    public void setBorderManager(BorderManager borderManager) {
        this.borderManager = borderManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    public Collection<Island> getAllIslands() {
        return this.islandsById.values();
    }

    public void reloadIsland(UUID islandId) {
        Island fresh = this.storage.load(islandId);
        if (fresh == null) {
            removeFromCache(islandId);
            return;
        }
        Island existing = this.islandsById.get(islandId);
        if (existing != null && !existing.getOwner().equals(fresh.getOwner()))
            this.ownerToIsland.remove(existing.getOwner());
        this.islandsById.put(islandId, fresh);
        this.ownerToIsland.put(fresh.getOwner(), islandId);
        this.cellToIsland.put(cellOfCenter(fresh.getCenterX(), fresh.getCenterZ()), islandId);
    }

    public void removeFromCache(UUID islandId) {
        Island existing = this.islandsById.remove(islandId);
        if (existing != null) {
            this.ownerToIsland.remove(existing.getOwner());
            this.cellToIsland.remove(cellOfCenter(existing.getCenterX(), existing.getCenterZ()), islandId);
        }
    }

    private long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
    }

    private long cellOfCenter(int centerX, int centerZ) {
        int dist = Math.max(1, settings.getIslandDistance());
        return cellKey(Math.round(centerX / (float) dist), Math.round(centerZ / (float) dist));
    }

    public int getProtectionHalf(Island island) {
        double size = this.upgradeManager != null
                ? this.upgradeManager.getValue(island, "size", settings.getIslandSize())
                : settings.getIslandSize();
        return (int) Math.max(1, size / 2);
    }

    public Island getIslandAt(Location location) {
        if (location.getWorld() == null)
            return null;


        int dist = Math.max(1, settings.getIslandDistance());
        int cellX = Math.round(location.getBlockX() / (float) dist);
        int cellZ = Math.round(location.getBlockZ() / (float) dist);
        UUID islandId = this.cellToIsland.get(cellKey(cellX, cellZ));
        if (islandId == null)
            return null;

        Island island = this.islandsById.get(islandId);
        if (island == null)
            return null;
        if (!isIslandDimension(location.getWorld().getName(), island))
            return null;
        if (this.localServerName != null && island.getServerName() != null
                && !island.getServerName().equals(this.localServerName))
            return null;

        int half = getProtectionHalf(island);
        if (Math.abs(location.getBlockX() - island.getCenterX()) <= half &&
                Math.abs(location.getBlockZ() - island.getCenterZ()) <= half)
            return island;

        return null;
    }

    /**
     * The island governing a location by grid cell, regardless of which of the
     * three island dimensions (overworld / nether / end) it sits in. Used for
     * portal routing and cross-dimension protection.
     */
    public Island getGoverningIsland(Location location) {
        if (location.getWorld() == null)
            return null;

        int dist = Math.max(1, settings.getIslandDistance());
        int cellX = Math.round(location.getBlockX() / (float) dist);
        int cellZ = Math.round(location.getBlockZ() / (float) dist);
        UUID islandId = this.cellToIsland.get(cellKey(cellX, cellZ));
        if (islandId == null)
            return null;

        Island island = this.islandsById.get(islandId);
        if (island == null)
            return null;
        if (this.localServerName != null && island.getServerName() != null
                && !island.getServerName().equals(this.localServerName))
            return null;

        int half = getProtectionHalf(island);
        if (Math.abs(location.getBlockX() - island.getCenterX()) <= half &&
                Math.abs(location.getBlockZ() - island.getCenterZ()) <= half)
            return island;

        return null;
    }

    /** True if {@code worldName} is the island's overworld plot or its nether/end plot. */
    private boolean isIslandDimension(String worldName, Island island) {
        return worldName.equals(island.getWorldName())
                || worldName.equals(settings.getNetherWorldName())
                || worldName.equals(settings.getEndWorldName());
    }

    public void register(Island island) {
        this.islandsById.put(island.getUniqueId(), island);
        this.ownerToIsland.put(island.getOwner(), island.getUniqueId());
        this.cellToIsland.put(cellOfCenter(island.getCenterX(), island.getCenterZ()), island.getUniqueId());
    }

    public Island getByMember(UUID playerId) {
        Island own = getByOwner(playerId);
        if (own != null)
            return own;
        for (Island island : this.islandsById.values())
            if (island.isMember(playerId))
                return island;
        return null;
    }

    public void transferOwnership(Island island, UUID newOwner) {
        UUID oldOwner = island.getOwner();
        this.ownerToIsland.remove(oldOwner);
        island.removeMember(newOwner);
        island.setOwner(newOwner);
        island.setRole(oldOwner, "moderator");
        this.ownerToIsland.put(newOwner, island.getUniqueId());
        saveAsync(island);
    }

    public void deleteIsland(Island island) {
        Bukkit.getPluginManager().callEvent(new net.cengiz1.uxmskyblock.event.IslandDeleteEvent(island));

        this.islandsById.remove(island.getUniqueId());
        this.placedBlocks.forget(island.getUniqueId());
        this.ownerToIsland.remove(island.getOwner());

        World world = this.worldManager.getWorld();

        for (UUID id : island.getAllMemberIds()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && member.isOnline() && isWithin(island, member.getLocation()))
                teleportToSpawn(member);
        }

        int half = Math.max(2, settings.getIslandDistance() / 2 - 1);
        int centerX = island.getCenterX();
        int centerY = island.getCenterY();
        int centerZ = island.getCenterZ();

        World netherWorld = this.worldManager.getNetherWorld();
        World endWorld = this.worldManager.getEndWorld();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            boolean cleared = this.schematicService.clearRegion(world, centerX, centerZ, half);
            if (!cleared)
                Bukkit.getScheduler().runTask(plugin, () -> clearPlatform(world, centerX, centerY, centerZ));
            if (netherWorld != null)
                this.schematicService.clearRegion(netherWorld, centerX, centerZ, half);
            if (endWorld != null)
                this.schematicService.clearRegion(endWorld, centerX, centerZ, half);

            this.storage.delete(island.getUniqueId());
            if (this.proxyManager != null && this.proxyManager.isEnabled())
                this.proxyManager.publishIslandDelete(island.getUniqueId());
        });
    }

    private boolean isWithin(Island island, Location location) {
        if (location.getWorld() == null || !isIslandDimension(location.getWorld().getName(), island))
            return false;
        int half = getProtectionHalf(island);
        return Math.abs(location.getBlockX() - island.getCenterX()) <= half
                && Math.abs(location.getBlockZ() - island.getCenterZ()) <= half;
    }

    public void deleteIslandConfirmed(Player owner, Island island) {
        deleteIsland(island);
        plugin.getMessages().send(owner, "deleted");
    }

    public void teleportToWarp(Player player, Island island) {
        teleportToWarp(player, island, null);
    }

    public void teleportToWarp(Player player, Island island, String warpName) {
        World world = this.worldManager.getWorld();
        Location target = island.getWarp(world, warpName);
        if (target == null)
            target = island.getHome(world);
        world.getChunkAt(target);
        player.teleport(target);
        if (this.borderManager != null)
            this.borderManager.apply(player, island);
    }

    public void teleportHome(Player player, Island island) {
        World world = this.worldManager.getWorld();
        Location home = island.getHome(world);
        world.getChunkAt(home);
        player.teleport(home);

        if (this.borderManager != null)
            this.borderManager.apply(player, island);
    }

    public void teleportToSpawn(Player player) {
        if (this.proxyManager != null && this.proxyManager.isEnabled() && this.proxyManager.sendToSpawn(player))
            return;
        Location spawn = settings.getSpawnLocation();
        if (spawn == null)
            spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.setWorldBorder(null);
        player.teleport(spawn);
    }

    public void messageMembers(Island island, String messageKey, String... replacements) {
        for (UUID id : island.getAllMemberIds()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && member.isOnline())
                plugin.getMessages().send(member, messageKey, replacements);
        }
    }

    void buildDefaultPlatform(World world, int centerX, int centerY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block block = world.getBlockAt(centerX + dx, centerY, centerZ + dz);
                block.setType(Material.GRASS_BLOCK, false);
            }
        }
        world.getBlockAt(centerX, centerY - 1, centerZ).setType(Material.BEDROCK, false);
    }

    private void clearPlatform(World world, int centerX, int centerY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(centerX + dx, centerY, centerZ + dz).setType(Material.AIR, false);
            }
        }
        world.getBlockAt(centerX, centerY - 1, centerZ).setType(Material.AIR, false);
    }

    public void saveAll() {
        Collection<Island> islands = this.islandsById.values();
        for (Island island : islands)
            this.storage.save(island);
    }

    public void saveAsync(Island island) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.storage.save(island);
            if (this.proxyManager != null && this.proxyManager.isEnabled())
                this.proxyManager.queueIslandSync(island.getUniqueId());
        });
    }

    private final java.util.Set<UUID> dirtyIslands = ConcurrentHashMap.newKeySet();

    public void markDirty(Island island) {
        this.dirtyIslands.add(island.getUniqueId());
    }

    public void startDirtyFlusher(long periodTicks) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, periodTicks, periodTicks);
    }

    private void flushDirty() {
        if (this.dirtyIslands.isEmpty())
            return;
        java.util.List<Island> batch = new java.util.ArrayList<>();
        for (UUID id : this.dirtyIslands) {
            if (this.dirtyIslands.remove(id)) {
                Island island = this.islandsById.get(id);
                if (island != null)
                    batch.add(island);
            }
        }
        for (Island island : batch) {
            this.storage.save(island);
            if (this.proxyManager != null && this.proxyManager.isEnabled())
                this.proxyManager.queueIslandSync(island.getUniqueId());
        }
    }

    public void shutdown() {
        this.creationService.shutdown();
        saveAll();
    }
}
