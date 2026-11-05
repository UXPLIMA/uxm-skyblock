package com.uxplima.uxmskyblock.bukkit.integration.placeholder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.OfflinePlayer;

import com.uxplima.uxmlib.hook.placeholder.PlaceholderExpansions;
import com.uxplima.uxmlib.hook.placeholder.PlaceholderProvider;
import com.uxplima.uxmlib.hook.placeholder.PlaceholderRegistry;
import com.uxplima.uxmskyblock.bukkit.session.PlayerSessionCoordinator;
import com.uxplima.uxmskyblock.core.application.bank.IslandBankPort;
import com.uxplima.uxmskyblock.core.application.island.IslandStoragePort;
import com.uxplima.uxmskyblock.core.application.scheduler.SchedulerPort;
import com.uxplima.uxmskyblock.core.application.upgrade.IslandUpgradeStoragePort;
import com.uxplima.uxmskyblock.core.domain.bank.IslandBank;
import com.uxplima.uxmskyblock.core.domain.identity.IslandId;
import com.uxplima.uxmskyblock.core.domain.identity.ProfileId;
import com.uxplima.uxmskyblock.core.domain.island.Island;
import com.uxplima.uxmskyblock.core.domain.island.IslandMember;
import com.uxplima.uxmskyblock.core.domain.upgrade.UpgradeId;
import org.jspecify.annotations.Nullable;

/**
 * PlaceholderAPI expansion for UXM Skyblock.
 *
 * <p>Serves player and island placeholders with zero main-thread database blocking using an
 * asynchronous refreshing in-memory snapshot cache.
 */
public final class SkyblockPlaceholderExpansion implements PlaceholderProvider {

    public static final String IDENTIFIER = "uxmskyblock";

    private static final String ONEBLOCK = "oneblock_";
    private static final String CHUNKBLOCK = "chunkblock_";
    public static final long CACHE_TTL_MS = 5000L;

    public record CachedPlayerIsland(
            boolean hasIsland,
            @Nullable UUID islandId,
            @Nullable String role,
            int membersCount,
            long bankBalanceMinorUnits,
            long crystals,
            Map<String, Integer> upgradeTiers,
            long cachedAtEpochMs) {

        public static CachedPlayerIsland empty() {
            return new CachedPlayerIsland(
                    false, null, null, 0, 0L, 0L, Collections.emptyMap(), System.currentTimeMillis());
        }

        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - cachedAtEpochMs > ttlMs;
        }
    }

    private final IslandStoragePort islandStoragePort;
    private final IslandBankPort islandBankPort;
    private final IslandUpgradeStoragePort upgradeStoragePort;
    private final SchedulerPort schedulerPort;
    private final java.util.function.Function<UUID, Optional<ProfileId>> activeProfileProvider;

    private final PlaceholderRegistry registry;
    private final Map<UUID, CachedPlayerIsland> playerCache = new ConcurrentHashMap<>();
    private final Set<UUID> refreshingPlayers = ConcurrentHashMap.newKeySet();
    private volatile boolean published;
    private volatile java.util.function.@Nullable UnaryOperator<Map<UpgradeId, Integer>> upgradeStanding;

    private volatile com.uxplima.uxmlib.board.@Nullable BoardAnswers boards;

    public SkyblockPlaceholderExpansion(
            IslandStoragePort islandStoragePort,
            IslandBankPort islandBankPort,
            IslandUpgradeStoragePort upgradeStoragePort,
            SchedulerPort schedulerPort,
            java.util.function.Function<UUID, Optional<ProfileId>> activeProfileProvider) {
        this.islandStoragePort = Objects.requireNonNull(islandStoragePort, "islandStoragePort must not be null");
        this.islandBankPort = Objects.requireNonNull(islandBankPort, "islandBankPort must not be null");
        this.upgradeStoragePort = Objects.requireNonNull(upgradeStoragePort, "upgradeStoragePort must not be null");
        this.schedulerPort = Objects.requireNonNull(schedulerPort, "schedulerPort must not be null");
        this.activeProfileProvider =
                Objects.requireNonNull(activeProfileProvider, "activeProfileProvider must not be null");

        this.registry = new PlaceholderRegistry();
        this.registry.fallback(this);
    }

    public SkyblockPlaceholderExpansion(
            IslandStoragePort islandStoragePort,
            IslandBankPort islandBankPort,
            IslandUpgradeStoragePort upgradeStoragePort,
            SchedulerPort schedulerPort,
            @Nullable PlayerSessionCoordinator sessionCoordinator) {
        this(
                islandStoragePort,
                islandBankPort,
                upgradeStoragePort,
                schedulerPort,
                sessionCoordinator != null ? sessionCoordinator::activeProfile : uuid -> Optional.empty());
    }

    public SkyblockPlaceholderExpansion(
            IslandStoragePort islandStoragePort,
            IslandBankPort islandBankPort,
            IslandUpgradeStoragePort upgradeStoragePort,
            SchedulerPort schedulerPort) {
        this(islandStoragePort, islandBankPort, upgradeStoragePort, schedulerPort, uuid -> Optional.empty());
    }

    /**
     * The tier each upgrade stands on, given what the island bought, so a free base tier reads as held
     * in a placeholder the way the upgrade list reads it.
     */
    public void useUpgradeStanding(java.util.function.@Nullable UnaryOperator<Map<UpgradeId, Integer>> standing) {
        this.upgradeStanding = standing;
    }

    private com.uxplima.uxmskyblock.bukkit.oneblock.@Nullable OneBlockPanel oneBlock;
    private com.uxplima.uxmskyblock.bukkit.chunkblock.@Nullable ChunkBlockPanel chunkBlock;

    /** Answers {@code chunkblock_<name>} for the island a player belongs to, while ChunkBlock is on. */
    public void useChunkBlock(com.uxplima.uxmskyblock.bukkit.chunkblock.@Nullable ChunkBlockPanel chunkBlock) {
        this.chunkBlock = chunkBlock;
    }

    /** Answers {@code oneblock_<name>} for the island a player belongs to, while OneBlock is on. */
    public void useOneBlock(com.uxplima.uxmskyblock.bukkit.oneblock.@Nullable OneBlockPanel oneBlock) {
        this.oneBlock = oneBlock;
    }

    /** The island boards placeholders answer from; nothing answers them until this is given. */
    public void useBoards(com.uxplima.uxmlib.board.@Nullable BoardAnswers boards) {
        this.boards = boards;
    }

    public PlaceholderRegistry registry() {
        return registry;
    }

    public boolean registerExpansion(String author, String version) {
        boolean registered = PlaceholderExpansions.register(IDENTIFIER, registry, author, version);
        this.published = registered;
        return registered;
    }

    /** Whether PlaceholderAPI took the expansion, which the doctor asks. */
    public boolean isPublished() {
        return published;
    }

    public void cacheData(UUID playerUuid, CachedPlayerIsland data) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(data, "data");
        playerCache.put(playerUuid, data);
    }

    public void invalidate(UUID playerUuid) {
        if (playerUuid != null) {
            playerCache.remove(playerUuid);
        }
    }

    /** Whether this node still holds a cached line for the player. */
    public boolean holds(UUID playerUuid) {
        return playerCache.containsKey(playerUuid);
    }

    public CachedPlayerIsland refreshPlayerDataSync(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ProfileId profileId = activeProfileProvider.apply(playerUuid).orElse(null);
        if (profileId == null) {
            CachedPlayerIsland empty = CachedPlayerIsland.empty();
            playerCache.put(playerUuid, empty);
            return empty;
        }
        Optional<IslandId> optIslandId = islandStoragePort.findIslandIdByProfileId(profileId);

        if (optIslandId.isEmpty()) {
            CachedPlayerIsland empty = CachedPlayerIsland.empty();
            playerCache.put(playerUuid, empty);
            return empty;
        }

        IslandId islandId = optIslandId.get();
        Optional<Island> optIsland = islandStoragePort.findIslandById(islandId);
        if (optIsland.isEmpty()) {
            CachedPlayerIsland empty = CachedPlayerIsland.empty();
            playerCache.put(playerUuid, empty);
            return empty;
        }

        Island island = optIsland.get();
        IslandMember member = island.members().get(profileId);
        String roleStr =
                (member != null && member.role() != null) ? member.role().id() : "";

        IslandBank bank = islandBankPort.findBankByIslandId(islandId).orElse(null);
        long bankBal = (bank != null) ? bank.primaryBalanceMinorUnits() : 0L;
        long bankCryst = (bank != null) ? bank.crystalsBalance() : 0L;

        Map<UpgradeId, Integer> bought = upgradeStoragePort.getUpgrades(islandId);
        java.util.function.UnaryOperator<Map<UpgradeId, Integer>> standingOf = this.upgradeStanding;
        Map<UpgradeId, Integer> rawUpgrades = bought == null || standingOf == null ? bought : standingOf.apply(bought);
        Map<String, Integer> tiers = new HashMap<>();
        if (rawUpgrades != null) {
            for (Map.Entry<UpgradeId, Integer> entry : rawUpgrades.entrySet()) {
                tiers.put(entry.getKey().key().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }

        CachedPlayerIsland cached = new CachedPlayerIsland(
                true,
                islandId.value(),
                roleStr,
                island.members().size(),
                bankBal,
                bankCryst,
                Collections.unmodifiableMap(tiers),
                System.currentTimeMillis());

        playerCache.put(playerUuid, cached);
        return cached;
    }

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(SkyblockPlaceholderExpansion.class.getName());

    private void scheduleAsyncRefresh(UUID playerUuid) {
        if (refreshingPlayers.add(playerUuid)) {
            schedulerPort.async(() -> {
                try {
                    refreshPlayerDataSync(playerUuid);
                } catch (Exception ex) {
                    LOGGER.log(java.util.logging.Level.FINEST, "Async placeholder refresh failed", ex);
                } finally {
                    refreshingPlayers.remove(playerUuid);
                }
            });
        }
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, String params) {
        Objects.requireNonNull(params, "params");
        String normalized = params.toLowerCase(Locale.ROOT).trim();

        // The island boards: top_<board>_alltime_<n>_name|value|head, rank_ and value_ of the reader, resets_.
        com.uxplima.uxmlib.board.BoardAnswers answers = boards;
        if (answers != null && answers.handles(normalized)) {
            String answer = answers.answer(
                    player == null ? null : player.getUniqueId(), player == null ? null : player.getName(), normalized);
            return answer == null ? "" : answer;
        }

        if (player == null || player.getUniqueId() == null) {
            return null;
        }

        UUID uuid = player.getUniqueId();
        CachedPlayerIsland cached = playerCache.get(uuid);

        if (cached == null) {
            scheduleAsyncRefresh(uuid);
            cached = CachedPlayerIsland.empty();
        } else if (cached.isExpired(CACHE_TTL_MS)) {
            scheduleAsyncRefresh(uuid);
        }

        return switch (normalized) {
            case "has_island" -> String.valueOf(cached.hasIsland());
            case "island_id" -> cached.islandId() != null ? cached.islandId().toString() : "";
            case "island_role", "role" -> cached.role() != null ? cached.role() : "";
            case "island_members_count", "members_count" -> String.valueOf(cached.membersCount());
            case "island_bank_balance", "bank_balance" ->
                String.format(Locale.US, "%.2f", (double) cached.bankBalanceMinorUnits() / 100.0);
            case "island_bank_balance_raw", "bank_balance_raw" -> String.valueOf(cached.bankBalanceMinorUnits());
            case "island_bank_crystals", "bank_crystals" -> String.valueOf(cached.crystals());
            default -> {
                if (normalized.startsWith("island_upgrade_tier_")) {
                    String upgradeKey = normalized.substring("island_upgrade_tier_".length());
                    yield String.valueOf(cached.upgradeTiers().getOrDefault(upgradeKey, 0));
                }
                com.uxplima.uxmskyblock.bukkit.oneblock.OneBlockPanel panel = this.oneBlock;
                if (normalized.startsWith(ONEBLOCK) && panel != null) {
                    UUID islandId = cached.islandId();
                    String name = normalized.substring(ONEBLOCK.length());
                    if (islandId == null) {
                        yield name.equals("is_oneblock") ? "false" : "";
                    }
                    yield panel.placeholder(player, islandId, name);
                }
                com.uxplima.uxmskyblock.bukkit.chunkblock.ChunkBlockPanel chunks = this.chunkBlock;
                if (normalized.startsWith(CHUNKBLOCK) && chunks != null) {
                    UUID islandId = cached.islandId();
                    String name = normalized.substring(CHUNKBLOCK.length());
                    if (islandId == null) {
                        yield name.equals("is_chunkblock") ? "false" : "";
                    }
                    yield chunks.placeholder(islandId, name);
                }
                if (normalized.startsWith("upgrade_tier_")) {
                    String upgradeKey = normalized.substring("upgrade_tier_".length());
                    yield String.valueOf(cached.upgradeTiers().getOrDefault(upgradeKey, 0));
                }
                yield null;
            }
        };
    }
}
