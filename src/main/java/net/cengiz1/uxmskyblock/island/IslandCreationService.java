package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.config.SettingsManager;
import net.cengiz1.uxmskyblock.proxy.ProxyManager;
import net.cengiz1.uxmskyblock.schematic.SchematicDefinition;
import net.cengiz1.uxmskyblock.schematic.SchematicService;
import net.cengiz1.uxmskyblock.storage.Storage;
import net.cengiz1.uxmskyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IslandCreationService {

    public enum Result {
        SUCCESS,
        ALREADY_HAS_ISLAND,
        ALREADY_CREATING,
        INVALID_SCHEMATIC,
        FAILED
    }

    private final UxmSkyblockPlugin plugin;
    private final SettingsManager settings;
    private final Storage storage;
    private final WorldManager worldManager;
    private final IslandManager islandManager;
    private final SchematicService schematicService;

    private final ExecutorService executor;
    private final Semaphore concurrencyLimit;
    private final Set<UUID> creating = ConcurrentHashMap.newKeySet();

    public IslandCreationService(UxmSkyblockPlugin plugin, SettingsManager settings, Storage storage,
                                 WorldManager worldManager, IslandManager islandManager,
                                 SchematicService schematicService) {
        this.plugin = plugin;
        this.settings = settings;
        this.storage = storage;
        this.worldManager = worldManager;
        this.islandManager = islandManager;
        this.schematicService = schematicService;
        this.executor = createExecutor(settings.getCreationThreads());
        this.concurrencyLimit = new Semaphore(settings.getMaxConcurrentCreations());
    }

    public CompletableFuture<Result> create(Player player) {
        return create(player, null);
    }

    public CompletableFuture<Result> create(Player player, String schematicKey) {
        UUID playerId = player.getUniqueId();

        if (islandManager.getByOwner(playerId) != null)
            return CompletableFuture.completedFuture(Result.ALREADY_HAS_ISLAND);

        String resolvedKey = schematicKey != null ? schematicKey : schematicService.getDefaultKey();
        boolean useSchematic = schematicService.isReady() && resolvedKey != null && schematicService.has(resolvedKey);

        if (schematicKey != null && schematicService.isReady() && !schematicService.has(schematicKey))
            return CompletableFuture.completedFuture(Result.INVALID_SCHEMATIC);

        if (!this.creating.add(playerId))
            return CompletableFuture.completedFuture(Result.ALREADY_CREATING);

        boolean natural = !this.settings.isVoidWorld();
        SchematicDefinition definition = (useSchematic && !natural) ? schematicService.get(resolvedKey) : null;

        CompletableFuture<Result> result = new CompletableFuture<>();
        AtomicBoolean acquired = new AtomicBoolean(false);

        this.executor.submit(() -> {
            try {
                this.concurrencyLimit.acquire();
                acquired.set(true);

                if (islandManager.getByOwner(playerId) != null) {
                    result.complete(Result.ALREADY_HAS_ISLAND);
                    return;
                }

                World world = this.worldManager.getWorld();
                int index = islandManager.getGrid().reserveIndex();
                int centerX = islandManager.getGrid().getCenterX(index);
                int centerY = this.settings.getIslandHeight();
                int centerZ = islandManager.getGrid().getCenterZ(index);

                double offsetX = definition != null ? definition.getHomeOffsetX() : 0.5;
                double offsetY = definition != null ? definition.getHomeOffsetY() : 1.0;
                double offsetZ = definition != null ? definition.getHomeOffsetZ() : 0.5;

                Island island = new Island(UUID.randomUUID(), playerId, world.getName(),
                        index, centerX, centerY, centerZ);
                island.setHome(centerX + offsetX, centerY + offsetY, centerZ + offsetZ, 0f, 0f);

                ProxyManager proxy = plugin.getProxyManager();
                if (proxy != null && proxy.isEnabled())
                    island.setServerNameRaw(proxy.getServerName());

                this.storage.save(island);

                if (proxy != null && proxy.isEnabled())
                    proxy.publishIslandUpdate(island.getUniqueId());

                boolean pasted = false;
                if (definition != null) {
                    try {
                        this.schematicService.paste(definition, world, centerX, centerY, centerZ);
                        pasted = true;
                    } catch (Throwable error) {
                        plugin.getLogger().warning("Schematic paste failed (" + definition.getKey()
                                + "), using fallback platform: " + error.getMessage());
                        island.setHome(centerX + 0.5, centerY + 1, centerZ + 0.5, 0f, 0f);
                        this.storage.save(island);
                    }
                }

                boolean fallback = !pasted;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (natural) {
                            world.getChunkAt(centerX >> 4, centerZ >> 4).load(true);
                            org.bukkit.block.Block top = world.getHighestBlockAt(centerX, centerZ);
                            int hx = centerX;
                            int hz = centerZ;
                            int surfaceY;
                            if (top.isLiquid() || top.getY() < world.getSeaLevel()) {
                                int[] land = findNearbyLand(world, centerX, centerZ);
                                if (land != null) {
                                    hx = land[0];
                                    hz = land[1];
                                    surfaceY = land[2];
                                    island.relocateCenter(hx, hz);
                                } else {
                                    surfaceY = world.getSeaLevel();
                                    islandManager.buildDefaultPlatform(world, centerX, surfaceY, centerZ);
                                }
                            } else {
                                surfaceY = top.getY();
                            }
                            island.setHome(hx + 0.5, surfaceY + 1, hz + 0.5, 0f, 0f);
                            islandManager.saveAsync(island);
                        } else if (fallback) {
                            islandManager.buildDefaultPlatform(world, centerX, centerY, centerZ);
                        }
                        islandManager.register(island);

                        fillStarterChest(world, island.getCenterX(), centerY, island.getCenterZ());

                        Player online = Bukkit.getPlayer(playerId);
                        if (online != null && online.isOnline()) {
                            Location home = island.getHome(world);
                            world.getChunkAt(home);
                            online.teleport(home);

                            if (islandManager.getBorderManager() != null)
                                islandManager.getBorderManager().apply(online, island);
                        }

                        Bukkit.getPluginManager().callEvent(
                                new net.cengiz1.uxmskyblock.event.IslandCreateEvent(island));
                        result.complete(Result.SUCCESS);
                    } catch (Throwable error) {
                        plugin.getLogger().warning("Island finalize failed for " + playerId + ": " + error.getMessage());
                        result.complete(Result.FAILED);
                    }
                });
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                result.complete(Result.FAILED);
            } catch (Throwable error) {
                plugin.getLogger().warning("Island creation failed for " + playerId + ": " + error.getMessage());
                result.complete(Result.FAILED);
            }
        });

        return result.whenComplete((value, error) -> {
            if (acquired.getAndSet(false))
                this.concurrencyLimit.release();
            this.creating.remove(playerId);
        });
    }

    private void fillStarterChest(World world, int centerX, int centerY, int centerZ) {
        if (!plugin.getConfig().getBoolean("creation.starter-chest.enabled", true))
            return;
        java.util.List<String> lines = plugin.getConfig().getStringList("creation.starter-chest.items");
        if (lines.isEmpty())
            return;
        boolean onlyIfEmpty = plugin.getConfig().getBoolean("creation.starter-chest.only-if-empty", true);
        int radius = Math.max(1, plugin.getConfig().getInt("creation.starter-chest.search-radius", 16));

        org.bukkit.block.Block chestBlock = findChest(world, centerX, centerY, centerZ, radius);
        if (chestBlock == null)
            return;
        if (!(chestBlock.getState() instanceof org.bukkit.block.Chest chest))
            return;
        org.bukkit.inventory.Inventory inventory = chest.getBlockInventory();
        if (onlyIfEmpty && !isEmpty(inventory))
            return;

        for (String line : lines) {
            org.bukkit.inventory.ItemStack item = parseItem(line);
            if (item != null)
                inventory.addItem(item);
        }
    }

    private org.bukkit.block.Block findChest(World world, int centerX, int centerY, int centerZ, int radius) {
        int minY = Math.max(world.getMinHeight(), centerY - 4);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + radius);
        for (int dx = -2; dx <= radius; dx++)
            for (int dz = -2; dz <= radius; dz++)
                for (int y = minY; y <= maxY; y++) {
                    org.bukkit.block.Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                    if (block.getType() == org.bukkit.Material.CHEST
                            || block.getType() == org.bukkit.Material.TRAPPED_CHEST)
                        return block;
                }
        return null;
    }

    private boolean isEmpty(org.bukkit.inventory.Inventory inventory) {
        for (org.bukkit.inventory.ItemStack item : inventory.getContents())
            if (item != null && item.getType() != org.bukkit.Material.AIR)
                return false;
        return true;
    }

    private org.bukkit.inventory.ItemStack parseItem(String line) {
        if (line == null || line.trim().isEmpty())
            return null;
        String[] parts = line.trim().split("[ :]+");
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(parts[0]);
        if (material == null || material == org.bukkit.Material.AIR)
            return null;
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return new org.bukkit.inventory.ItemStack(material, amount);
    }

    private int[] findNearbyLand(World world, int centerX, int centerZ) {
        int seaLevel = world.getSeaLevel();
        int dist = Math.max(1, settings.getIslandDistance());
        int cellHalf = dist / 2 - 16;
        int maxRadiusChunks = Math.max(1, Math.min(4, cellHalf / 16));
        int ccx = centerX >> 4;
        int ccz = centerZ >> 4;
        for (int r = 1; r <= maxRadiusChunks; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r)
                        continue;
                    int bx = ((ccx + dx) << 4) + 8;
                    int bz = ((ccz + dz) << 4) + 8;
                    if (Math.abs(bx - centerX) > cellHalf || Math.abs(bz - centerZ) > cellHalf)
                        continue;
                    world.getChunkAt(ccx + dx, ccz + dz).load(true);
                    org.bukkit.block.Block top = world.getHighestBlockAt(bx, bz);
                    if (!top.isLiquid() && top.getY() >= seaLevel)
                        return new int[]{bx, bz, top.getY()};
                }
            }
        }
        return null;
    }

    public boolean isCreating(UUID playerId) {
        return this.creating.contains(playerId);
    }

    public void shutdown() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(10, TimeUnit.SECONDS))
                this.executor.shutdownNow();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            this.executor.shutdownNow();
        }
    }

    private ExecutorService createExecutor(int threads) {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "skyblock-creation-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
