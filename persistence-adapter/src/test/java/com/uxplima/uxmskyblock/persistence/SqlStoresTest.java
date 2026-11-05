package com.uxplima.uxmskyblock.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import com.uxplima.uxmlib.storage.testing.EveryBackend;
import com.uxplima.uxmlib.storage.testing.RealDatabase;
import com.uxplima.uxmskyblock.core.application.bank.BankCurrencies;
import com.uxplima.uxmskyblock.core.domain.bank.BankTransactionOutcome;
import com.uxplima.uxmskyblock.core.domain.bank.IslandBank;
import com.uxplima.uxmskyblock.core.domain.home.Home;
import com.uxplima.uxmskyblock.core.domain.home.HomeId;
import com.uxplima.uxmskyblock.core.domain.home.HomeScope;
import com.uxplima.uxmskyblock.core.domain.identity.IslandId;
import com.uxplima.uxmskyblock.core.domain.identity.PlayerUuid;
import com.uxplima.uxmskyblock.core.domain.identity.ProfileId;
import com.uxplima.uxmskyblock.core.domain.island.Island;
import com.uxplima.uxmskyblock.core.domain.island.IslandAuthorityOutcome;
import com.uxplima.uxmskyblock.core.domain.island.IslandFlags;
import com.uxplima.uxmskyblock.core.domain.island.IslandLocation;
import com.uxplima.uxmskyblock.core.domain.island.IslandMember;
import com.uxplima.uxmskyblock.core.domain.island.IslandRole;
import com.uxplima.uxmskyblock.core.domain.session.ServerNodeId;
import com.uxplima.uxmskyblock.core.domain.upgrade.UpgradeId;
import com.uxplima.uxmskyblock.core.domain.warp.IslandBan;
import com.uxplima.uxmskyblock.core.domain.warp.IslandWarp;
import com.uxplima.uxmskyblock.core.domain.warp.IslandWarpId;
import com.uxplima.uxmskyblock.core.domain.warp.WarpCategory;
import com.uxplima.uxmskyblock.core.domain.warp.WarpLocation;
import com.uxplima.uxmskyblock.core.domain.warp.WarpName;
import com.uxplima.uxmskyblock.persistence.bootstrap.PersistenceBootstrap;

/** The stores an island lives in, on every database an operator may pick, opened the way the plugin opens them. */
class SqlStoresTest {

    private static final PlayerUuid ALEX = PlayerUuid.of(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
    private static final ProfileId ALEX_PROFILE = ProfileId.of(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final PlayerUuid BLAKE = PlayerUuid.of(UUID.fromString("00000000-0000-0000-0000-00000000000b"));
    private static final ProfileId BLAKE_PROFILE =
            ProfileId.of(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final ServerNodeId NODE = ServerNodeId.of("node-a");
    private static final String WORLD = "skyblock";

    private static PersistenceBootstrap open(RealDatabase real) {
        if (real.backend().isFile()) {
            return PersistenceBootstrap.createSqlite(real.settings().sqliteFile(real.dataFolder()));
        }
        return PersistenceBootstrap.createRemote(
                real.settings().jdbcUrl(real.dataFolder()),
                real.settings().username(),
                real.settings().password(),
                real.settings().poolSize());
    }

    /** An island Alex owns at the grid origin, with Blake as a member, saved. */
    private static Island alexIsland(PersistenceBootstrap stores) {
        stores.registerProfile(ALEX, ALEX_PROFILE);
        stores.registerProfile(BLAKE, BLAKE_PROFILE);
        IslandId id = IslandId.of(UUID.randomUUID());
        IslandLocation location = IslandLocation.fromCenterAndRadius(id, WORLD, 0, 0, 50);
        Island created = Island.create(id, location.bounds(), ALEX, ALEX_PROFILE, NOW);
        java.util.Map<ProfileId, IslandMember> members = new java.util.HashMap<>(created.members());
        members.put(BLAKE_PROFILE, new IslandMember(BLAKE, BLAKE_PROFILE, IslandRole.MODERATOR, NOW));
        Island island = new Island(
                id,
                created.bounds(),
                ALEX,
                ALEX_PROFILE,
                members,
                created.roles(),
                created.flags().withFlag(IslandFlags.PVP, true),
                NOW);
        stores.islandStoragePort().saveIsland(island, location);
        return island;
    }

    @EveryBackend
    void anIslandComesBackWithItsMembersFlagsAndPlace(RealDatabase real) {
        try (PersistenceBootstrap stores = open(real)) {
            Island island = alexIsland(stores);

            Island read = stores.islandStoragePort().findIslandById(island.id()).orElseThrow();
            assertThat(read.ownerProfileId()).isEqualTo(ALEX_PROFILE);
            assertThat(read.roleOf(BLAKE_PROFILE)).isEqualTo(IslandRole.MODERATOR);
            assertThat(read.flags().isEnabled(IslandFlags.PVP)).isTrue();
            assertThat(read.bounds()).isEqualTo(island.bounds());
            assertThat(stores.islandStoragePort().findIslandIdByProfileId(BLAKE_PROFILE))
                    .contains(island.id());
            assertThat(stores.islandStoragePort().findLocationByIslandId(island.id()))
                    .hasValueSatisfying(at -> assertThat(at.worldName()).isEqualTo(WORLD));
            assertThat(stores.islandStoragePort().findIslandByLocation(WORLD, 10, -10))
                    .hasValueSatisfying(found -> assertThat(found.id()).isEqualTo(island.id()));
            assertThat(stores.islandStoragePort().findIslandByLocation(WORLD, 5000, 5000))
                    .isEmpty();
        }
    }

    @EveryBackend
    void aDeletedIslandLeavesNobodyAMemberOfIt(RealDatabase real) {
        try (PersistenceBootstrap stores = open(real)) {
            Island island = alexIsland(stores);

            stores.islandStoragePort().deleteIsland(island.id());

            assertThat(stores.islandStoragePort().findIslandById(island.id())).isEmpty();
            assertThat(stores.islandStoragePort().findIslandIdByProfileId(ALEX_PROFILE))
                    .isEmpty();
            assertThat(stores.islandStoragePort().findIslandIdByProfileId(BLAKE_PROFILE))
                    .isEmpty();
        }
    }

    private static long epoch(PersistenceBootstrap stores, IslandId island) {
        IslandAuthorityOutcome outcome = stores.islandAuthorityPort().acquireAuthority(island, NODE, 60);
        assertThat(outcome).isInstanceOf(IslandAuthorityOutcome.Success.class);
        return ((IslandAuthorityOutcome.Success) outcome).epoch();
    }

    private static BankTransactionOutcome move(
            PersistenceBootstrap stores,
            IslandId island,
            long delta,
            String node,
            long epoch,
            long version,
            String key) {
        return stores.islandBankPort()
                .executeTransaction(
                        island,
                        ALEX.value(),
                        BankCurrencies.PRIMARY_COLUMN,
                        2,
                        delta,
                        "test",
                        node,
                        epoch,
                        version,
                        UUID.randomUUID(),
                        key);
    }

    @EveryBackend
    void theBankMovesMoneyOnceOnTheVersionItReadUnderTheNodeThatHoldsTheIsland(RealDatabase real) {
        try (PersistenceBootstrap stores = open(real)) {
            IslandId island = alexIsland(stores).id();
            long epoch = epoch(stores, island);
            IslandBank empty = stores.islandBankPort().createBank(island);

            BankTransactionOutcome deposit =
                    move(stores, island, 50_000, NODE.value(), epoch, empty.version(), "deposit-1");
            assertThat(deposit).isInstanceOf(BankTransactionOutcome.Success.class);
            IslandBank afterDeposit =
                    stores.islandBankPort().findBankByIslandId(island).orElseThrow();
            assertThat(afterDeposit.primaryBalanceMinorUnits()).isEqualTo(50_000);

            // The same key again is the same deposit, not a second one.
            move(stores, island, 50_000, NODE.value(), epoch, afterDeposit.version(), "deposit-1");
            assertThat(stores.islandBankPort()
                            .findBankByIslandId(island)
                            .orElseThrow()
                            .primaryBalanceMinorUnits())
                    .isEqualTo(50_000);

            assertThat(move(stores, island, -60_000, NODE.value(), epoch, afterDeposit.version(), "overdraw"))
                    .isInstanceOf(BankTransactionOutcome.InsufficientFunds.class);
            assertThat(move(stores, island, -1_000, NODE.value(), epoch, empty.version(), "stale"))
                    .isInstanceOf(BankTransactionOutcome.StaleVersion.class);
            assertThat(move(stores, island, -1_000, "node-b", epoch, afterDeposit.version(), "elsewhere"))
                    .isInstanceOf(BankTransactionOutcome.AuthorityRejected.class);
            assertThat(move(stores, island, -1_000, NODE.value(), epoch + 1, afterDeposit.version(), "old-epoch"))
                    .isInstanceOf(BankTransactionOutcome.AuthorityRejected.class);

            assertThat(stores.islandBankPort()
                            .findBankByIslandId(island)
                            .orElseThrow()
                            .primaryBalanceMinorUnits())
                    .isEqualTo(50_000);
        }
    }

    @EveryBackend
    void warpsAndBansBelongToTheirIsland(RealDatabase real) {
        try (PersistenceBootstrap stores = open(real)) {
            IslandId island = alexIsland(stores).id();
            Instant at = NOW.truncatedTo(ChronoUnit.SECONDS);
            IslandWarp farm = new IslandWarp(
                    IslandWarpId.random(),
                    island,
                    WarpName.of("farm"),
                    new WarpLocation(WORLD, 1.5, 70, -2.5, 90, 0),
                    "WHEAT",
                    WarpCategory.FARMS,
                    false,
                    at,
                    at);
            IslandWarp vault = new IslandWarp(
                    IslandWarpId.random(),
                    island,
                    WarpName.of("vault"),
                    new WarpLocation(WORLD, 0, 64, 0, 0, 0),
                    "CHEST",
                    WarpCategory.GENERAL,
                    true,
                    at,
                    at);
            var warps = stores.islandWarpStoragePort();
            warps.saveWarp(farm);
            warps.saveWarp(vault);

            assertThat(warps.findWarpByName(island, WarpName.of("farm"))).hasValueSatisfying(read -> {
                assertThat(read.location()).isEqualTo(farm.location());
                assertThat(read.category()).isEqualTo(WarpCategory.FARMS);
            });
            assertThat(warps.countWarpsByIsland(island)).isEqualTo(2);
            assertThat(warps.findPublicWarpsByCategory(WarpCategory.FARMS, 10, 0))
                    .extracting(IslandWarp::id)
                    .containsExactly(farm.id());
            assertThat(warps.deleteWarp(island, WarpName.of("farm"))).isTrue();
            assertThat(warps.deleteWarp(island, WarpName.of("farm"))).isFalse();
            assertThat(warps.findWarpsByIsland(island))
                    .extracting(IslandWarp::id)
                    .containsExactly(vault.id());

            warps.banPlayer(new IslandBan(island, BLAKE, ALEX_PROFILE, "griefing", at));
            assertThat(warps.isPlayerBanned(island, BLAKE)).isTrue();
            assertThat(warps.findBansByIsland(island))
                    .singleElement()
                    .satisfies(ban -> assertThat(ban.reason()).isEqualTo("griefing"));
            assertThat(warps.unbanPlayer(island, BLAKE)).isTrue();
            assertThat(warps.isPlayerBanned(island, BLAKE)).isFalse();
        }
    }

    @EveryBackend
    void homesAreKeptPerPlayerByName(RealDatabase real) {
        try (PersistenceBootstrap stores = open(real)) {
            IslandId island = alexIsland(stores).id();
            Instant at = NOW.truncatedTo(ChronoUnit.SECONDS);
            var homes = stores.homeStoragePort();
            Home base = new Home(
                    HomeId.random(), ALEX_PROFILE, island, "base", HomeScope.PERSONAL, WORLD, 3, 65, 4, 180, 0, at, at);
            homes.saveHome(base);
            homes.saveHome(new Home(
                    HomeId.random(), ALEX_PROFILE, island, "mine", HomeScope.PERSONAL, WORLD, 9, 12, 9, 0, 0, at, at));

            assertThat(homes.findHome(ALEX_PROFILE, "base"))
                    .hasValueSatisfying(read -> assertThat(read.x()).isEqualTo(3));
            assertThat(homes.countHomes(ALEX_PROFILE)).isEqualTo(2);
            assertThat(homes.findHomesByProfileId(BLAKE_PROFILE)).isEmpty();
            assertThat(homes.deleteHome(ALEX_PROFILE, "base")).isTrue();
            assertThat(homes.findHome(ALEX_PROFILE, "base")).isEmpty();
            assertThat(homes.countHomes(ALEX_PROFILE)).isEqualTo(1);
        }
    }

    @EveryBackend
    void anUpgradeTierMovesOnlyFromTheTierItWasReadAt(RealDatabase real) {
        try (PersistenceBootstrap stores = open(real)) {
            IslandId island = alexIsland(stores).id();
            var upgrades = stores.islandUpgradeStoragePort();

            assertThat(upgrades.getUpgradeTier(island, UpgradeId.SIZE)).isZero();
            assertThat(upgrades.compareAndSetUpgradeTier(island, UpgradeId.SIZE, 0, 1))
                    .isTrue();
            assertThat(upgrades.compareAndSetUpgradeTier(island, UpgradeId.SIZE, 0, 2))
                    .isFalse();
            upgrades.setUpgradeTier(island, UpgradeId.WARPS, 3);

            assertThat(upgrades.getUpgrades(island))
                    .containsAllEntriesOf(Map.of(UpgradeId.SIZE, 1, UpgradeId.WARPS, 3));
        }
    }
}
