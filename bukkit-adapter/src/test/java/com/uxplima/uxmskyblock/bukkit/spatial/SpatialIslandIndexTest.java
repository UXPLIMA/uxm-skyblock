package com.uxplima.uxmskyblock.bukkit.spatial;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.uxplima.uxmskyblock.core.domain.identity.IslandId;
import com.uxplima.uxmskyblock.core.domain.identity.PlayerUuid;
import com.uxplima.uxmskyblock.core.domain.identity.ProfileId;
import com.uxplima.uxmskyblock.core.domain.island.Island;
import com.uxplima.uxmskyblock.core.domain.island.IslandBounds;
import com.uxplima.uxmskyblock.core.domain.island.IslandMember;
import com.uxplima.uxmskyblock.core.domain.island.IslandRole;
import org.junit.jupiter.api.Test;

/** The island the protection index answers with, after its members change. */
class SpatialIslandIndexTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final ProfileId OWNER = ProfileId.of(new UUID(1, 1));
    private static final ProfileId JOINER = ProfileId.of(new UUID(1, 2));

    private static Island island(IslandId id, boolean withJoiner) {
        Island created = Island.create(
                id, IslandBounds.fromCenterAndRadius(5120, 0, 25), PlayerUuid.of(new UUID(0, 1)), OWNER, NOW);
        if (!withJoiner) {
            return created;
        }
        Map<ProfileId, IslandMember> members = new HashMap<>(created.members());
        members.put(JOINER, new IslandMember(PlayerUuid.of(new UUID(0, 2)), JOINER, IslandRole.MEMBER, NOW));
        return new Island(
                id, created.bounds(), created.ownerPlayerUuid(), OWNER, members, created.roles(), created.flags(), NOW);
    }

    @Test
    void aPlayerWhoJoinedBelongsToTheIslandTheIndexAnswersWith() {
        IslandId id = IslandId.of(new UUID(2, 1));
        SpatialIslandIndex index = new SpatialIslandIndex();
        index.indexIsland(island(id, false), "world");

        index.refresh(island(id, true));

        assertThat(index.findIslandAt("world", 5120, 0))
                .hasValueSatisfying(found -> assertThat(found.isMember(JOINER)).isTrue());
    }

    @Test
    void anIslandTheIndexDoesNotHoldStaysOut() {
        SpatialIslandIndex index = new SpatialIslandIndex();

        index.refresh(island(IslandId.of(new UUID(2, 2)), true));

        assertThat(index.cachedIslandCount()).isZero();
    }
}
