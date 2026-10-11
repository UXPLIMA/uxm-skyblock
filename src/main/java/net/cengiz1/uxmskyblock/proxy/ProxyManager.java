package net.cengiz1.uxmskyblock.proxy;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.config.SettingsManager;
import net.cengiz1.uxmskyblock.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyManager {

    private final UxmSkyblockPlugin plugin;

    private boolean enabled;
    private boolean debug;
    private String serverName;
    private String spawnServer;
    private List<String> createServers = new ArrayList<>();
    private String channel;
    private int pendingTtlSeconds;

    private RedisMessenger messenger;
    private ServerConnector connector;

    private final Set<UUID> pendingSync = ConcurrentHashMap.newKeySet();
    private BukkitTask syncFlushTask;

    public ProxyManager(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerName() {
        return serverName;
    }

    public String getSpawnServer() {
        return spawnServer;
    }

    public List<String> getCreateServers() {
        return createServers;
    }

    public boolean isLocalCreate() {
        return createServers.isEmpty() || createServers.contains(serverName);
    }

    public String chooseCreateServer() {
        if (createServers.isEmpty())
            return serverName;
        if (createServers.size() == 1)
            return createServers.get(0);

        Map<String, Integer> counts = new HashMap<>();
        for (String server : createServers)
            counts.put(server, 0);
        for (Island island : plugin.getIslandManager().getAllIslands()) {
            String host = island.getServerName();
            if (host != null && counts.containsKey(host))
                counts.put(host, counts.get(host) + 1);
        }

        String best = createServers.get(0);
        int bestCount = Integer.MAX_VALUE;
        for (String server : createServers) {
            int count = counts.getOrDefault(server, 0);
            if (count < bestCount) {
                bestCount = count;
                best = server;
            }
        }
        return best;
    }

    private void debug(String message) {
        if (debug)
            plugin.getLogger().info("[Proxy] " + message);
    }

    public void start() {
        SettingsManager settings = plugin.getSettings();
        if (!settings.isProxyEnabled()) {
            this.enabled = false;
            return;
        }

        this.serverName = settings.getProxyServerName();
        this.spawnServer = settings.getProxySpawnServer();
        this.createServers = new ArrayList<>(settings.getProxyCreateServers());
        this.channel = settings.getProxyRedisChannel();
        this.pendingTtlSeconds = settings.getProxyPendingSeconds();
        this.debug = settings.isProxyDebug();

        if (serverName == null || serverName.isEmpty()) {
            plugin.getLogger().severe("Proxy: 'proxy.server-name' cannot be empty; proxy module disabled.");
            this.enabled = false;
            return;
        }

        try {
            this.messenger = new RedisMessenger(plugin.getLogger(),
                    settings.getProxyRedisHost(), settings.getProxyRedisPort(),
                    settings.getProxyRedisUsername(), settings.getProxyRedisPassword(),
                    settings.getProxyRedisTimeout(), channel, this::onMessage);
        } catch (Throwable error) {
            plugin.getLogger().severe("Proxy: could not connect to Redis, proxy module disabled: " + error.getMessage());
            this.enabled = false;
            return;
        }

        this.connector = new ServerConnector(plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ServerConnector.BUNGEE_CHANNEL);

        this.enabled = true;
        plugin.getIslandManager().setLocalServerName(serverName);
        plugin.getIslandManager().setProxyManager(this);

        this.syncFlushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushSync, 100L, 100L);

        plugin.getLogger().info("Proxy module enabled. This server's name: " + serverName);
    }

    public void stop() {
        if (syncFlushTask != null)
            syncFlushTask.cancel();
        if (enabled)
            flushSync();
        if (messenger != null)
            messenger.shutdown();
        this.enabled = false;
    }

    public boolean isLocal(Island island) {
        if (!enabled)
            return true;
        String host = island.getServerName();
        return host == null || host.equals(serverName);
    }

    public boolean handleTeleport(Player player, Island island) {
        if (!enabled)
            return false;

        String target = island.getServerName();
        if (target == null) {

            debug("Island " + island.getUniqueId() + " had no server; stamped as " + serverName + " (local teleport).");
            island.setServerName(serverName);
            plugin.getIslandManager().saveAsync(island);
            return false;
        }
        if (target.equals(serverName)) {
            debug("Island " + island.getUniqueId() + " is already on this server; local teleport.");
            return false;
        }

        debug("Routing player " + player.getName() + " -> server '" + target + "' (island teleport).");
        messenger.setWithExpiry(pendingKey(player.getUniqueId()), island.getUniqueId().toString(), pendingTtlSeconds);
        plugin.getMessages().send(player, "proxy-sending", "{server}", target);
        connector.connect(player, target);
        return true;
    }

    public boolean handleRemoteCreate(Player player, String schematicKey) {
        if (!enabled || isLocalCreate())
            return false;

        String target = chooseCreateServer();
        if (target == null || target.equals(serverName))
            return false;

        debug("Routing player " + player.getName() + " -> server '" + target + "' (island creation).");

        String value = schematicKey == null ? "-" : schematicKey;
        messenger.setWithExpiry(createKey(player.getUniqueId()), value, pendingTtlSeconds);
        plugin.getMessages().send(player, "proxy-creating", "{server}", target);
        connector.connect(player, target);
        return true;
    }

    public boolean handleWarpTeleport(Player player, Island island) {
        return handleWarpTeleport(player, island, null);
    }

    public boolean handleWarpTeleport(Player player, Island island, String warpName) {
        if (!enabled)
            return false;

        String target = island.getServerName();
        if (target == null || target.equals(serverName))
            return false;

        debug("Routing player " + player.getName() + " -> server '" + target + "' (island warp).");
        String value = island.getUniqueId().toString();
        if (warpName != null && !warpName.isEmpty())
            value += "|" + warpName;
        messenger.setWithExpiry(warpKey(player.getUniqueId()), value, pendingTtlSeconds);
        plugin.getMessages().send(player, "proxy-sending", "{server}", target);
        connector.connect(player, target);
        return true;
    }

    public boolean sendToSpawn(Player player) {
        if (!enabled || spawnServer == null || spawnServer.isEmpty() || spawnServer.equals(serverName))
            return false;
        debug("Sending player " + player.getName() + " -> '" + spawnServer + "' (spawn) server.");
        connector.connect(player, spawnServer);
        return true;
    }

    public void sendToServer(Player player, String server) {
        if (enabled && server != null && !server.isEmpty())
            connector.connect(player, server);
    }

    public void onPlayerJoin(Player player) {
        if (!enabled)
            return;
        tryCompletePendingCreate(player);
        tryCompletePendingTeleport(player);
        tryCompletePendingWarp(player);
    }

    public void tryCompletePendingWarp(Player player) {
        if (!enabled)
            return;
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String value = messenger.takeValue(warpKey(playerId));
            if (value == null)
                return;
            String idPart = value;
            final String warpName;
            int sep = value.indexOf('|');
            if (sep >= 0) {
                idPart = value.substring(0, sep);
                warpName = value.substring(sep + 1);
            } else {
                warpName = null;
            }
            UUID islandId;
            try {
                islandId = UUID.fromString(idPart);
            } catch (IllegalArgumentException error) {
                return;
            }
            if (plugin.getIslandManager().getById(islandId) == null)
                plugin.getIslandManager().reloadIsland(islandId);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(playerId);
                Island island = plugin.getIslandManager().getById(islandId);
                if (online != null && online.isOnline() && island != null) {
                    plugin.getMessages().send(online, "warping", "{player}",
                            net.cengiz1.uxmskyblock.util.PlayerNames.nameOf(island.getOwner()));
                    plugin.getIslandManager().teleportToWarp(online, island, warpName);
                }
            }, 20L);
        });
    }

    public void tryCompletePendingCreate(Player player) {
        if (!enabled)
            return;
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String value = messenger.takeValue(createKey(playerId));
            if (value == null)
                return;
            String schematic = "-".equals(value) ? null : value;
            debug("Found a pending creation for " + player.getName() + "; creating the island.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(playerId);
                if (online != null && online.isOnline())
                    startIslandCreation(online, schematic);
            }, 20L);
        });
    }

    private void startIslandCreation(Player player, String schematicKey) {
        if (plugin.getIslandManager().getByMember(player.getUniqueId()) != null) {
            plugin.getMessages().send(player, "already-have-island");
            return;
        }
        plugin.getMessages().send(player, "creating");
        plugin.getIslandManager().getCreationService().create(player, schematicKey).whenComplete((result, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null || result == null) {
                        plugin.getMessages().send(player, "create-failed");
                        return;
                    }
                    switch (result) {
                        case SUCCESS: plugin.getMessages().send(player, "created"); break;
                        case ALREADY_HAS_ISLAND: plugin.getMessages().send(player, "already-have-island"); break;
                        case ALREADY_CREATING: plugin.getMessages().send(player, "creating-in-progress"); break;
                        case INVALID_SCHEMATIC: plugin.getMessages().send(player, "create-failed"); break;
                        default: plugin.getMessages().send(player, "create-failed"); break;
                    }
                }));
    }

    public void tryCompletePendingTeleport(Player player) {
        if (!enabled)
            return;
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String value = messenger.takeValue(pendingKey(playerId));
            if (value == null)
                return;
            UUID islandId;
            try {
                islandId = UUID.fromString(value);
            } catch (IllegalArgumentException error) {
                return;
            }

            if (plugin.getIslandManager().getById(islandId) == null)
                plugin.getIslandManager().reloadIsland(islandId);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(playerId);
                Island island = plugin.getIslandManager().getById(islandId);
                if (online != null && online.isOnline() && island != null) {
                    plugin.getMessages().send(online, "teleporting");
                    plugin.getIslandManager().teleportHome(online, island);
                }
            }, 20L);
        });
    }

    public void queueIslandSync(UUID islandId) {
        if (enabled)
            pendingSync.add(islandId);
    }

    public void publishIslandUpdate(UUID islandId) {
        if (enabled)
            messenger.publish(ProxyMessage.island(MessageType.ISLAND_UPDATE, serverName, islandId).serialize());
    }

    public void publishIslandDelete(UUID islandId) {
        if (enabled)
            messenger.publish(ProxyMessage.island(MessageType.ISLAND_DELETE, serverName, islandId).serialize());
    }

    private void flushSync() {
        if (!enabled || pendingSync.isEmpty())
            return;
        for (UUID islandId : new ArrayList<>(pendingSync)) {
            pendingSync.remove(islandId);
            messenger.publish(ProxyMessage.island(MessageType.ISLAND_UPDATE, serverName, islandId).serialize());
        }
    }

    private void onMessage(String raw) {
        ProxyMessage message = ProxyMessage.parse(raw);
        if (message == null)
            return;
        if (serverName.equals(message.getOrigin()))
            return;
        UUID islandId = message.getIslandId();
        if (islandId == null)
            return;

        debug("Gelen mesaj: " + message.getType() + " (ada " + islandId + ", kaynak " + message.getOrigin() + ")");
        switch (message.getType()) {
            case ISLAND_UPDATE:
                runAsyncSafe(() -> plugin.getIslandManager().reloadIsland(islandId));
                break;
            case ISLAND_DELETE:
                plugin.getIslandManager().removeFromCache(islandId);
                break;
            default:
                break;
        }
    }

    private void runAsyncSafe(Runnable task) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } catch (IllegalStateException disabling) {

        }
    }

    private String pendingKey(UUID playerId) {
        return channel + ":pending:" + playerId;
    }

    private String createKey(UUID playerId) {
        return channel + ":pending-create:" + playerId;
    }

    private String warpKey(UUID playerId) {
        return channel + ":pending-warp:" + playerId;
    }
}
