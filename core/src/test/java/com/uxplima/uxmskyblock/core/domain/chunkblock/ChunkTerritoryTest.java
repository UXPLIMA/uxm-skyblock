package com.uxplima.uxmskyblock.core.domain.chunkblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.uxplima.uxmskyblock.core.domain.island.IslandBounds;
import org.junit.jupiter.api.Test;

/** How a ChunkBlock island opens and loses ground. */
class ChunkTerritoryTest {

    private static final ChunkPos ORIGIN = new ChunkPos(0, 0);
    /** Five chunks each way from the origin chunk. */
    private static final IslandBounds BOUNDS = IslandBounds.fromCenterAndRadius(8, 8, 80);
    /** One level for the first chunk, three for the second, then two more each. */
    private static final ChunkUnlockRules RULES = new ChunkUnlockRules(List.of(1L, 3L), 2);

    @Test
    void aChunkOpensOnlyBesideOpenGroundInsideTheIslandAtTheLevelTheLadderAsks() {
        ChunkTerritory territory = ChunkTerritory.startingAt(ORIGIN);

        assertThat(territory.unlock(new ChunkPos(2, 0), BOUNDS, RULES, 99))
                .isEqualTo(ChunkTerritory.Unlock.NOT_BESIDE_TERRITORY);
        assertThat(territory.unlock(new ChunkPos(1, 1), BOUNDS, RULES, 99))
                .as("a corner is not a side")
                .isEqualTo(ChunkTerritory.Unlock.NOT_BESIDE_TERRITORY);
        assertThat(territory.unlock(new ChunkPos(1, 0), BOUNDS, RULES, 0))
                .isEqualTo(ChunkTerritory.Unlock.LEVEL_TOO_LOW);
        assertThat(territory.unlock(new ChunkPos(1, 0), BOUNDS, RULES, 1)).isEqualTo(ChunkTerritory.Unlock.UNLOCKED);
        assertThat(territory.unlock(new ChunkPos(1, 0), BOUNDS, RULES, 1))
                .isEqualTo(ChunkTerritory.Unlock.ALREADY_OPEN);
        assertThat(territory.unlock(new ChunkPos(2, 0), BOUNDS, RULES, 2))
                .isEqualTo(ChunkTerritory.Unlock.LEVEL_TOO_LOW);
        assertThat(territory.unlock(new ChunkPos(2, 0), BOUNDS, RULES, 3)).isEqualTo(ChunkTerritory.Unlock.UNLOCKED);
        assertThat(territory.nextRequirement(RULES)).isEqualTo(5);

        ChunkTerritory atTheEdge = new ChunkTerritory(
                ORIGIN,
                List.of(
                        new ChunkPos(1, 0),
                        new ChunkPos(2, 0),
                        new ChunkPos(3, 0),
                        new ChunkPos(4, 0),
                        new ChunkPos(5, 0)));
        assertThat(atTheEdge.placeFor(new ChunkPos(6, 0), BOUNDS)).isEqualTo(ChunkTerritory.Unlock.OUTSIDE_ISLAND);
    }

    @Test
    void aFallingLevelClosesHeldChunksNewestFirstAndNeverOnePaidFor() {
        ChunkTerritory territory = ChunkTerritory.startingAt(ORIGIN);
        territory.open(new ChunkPos(1, 0), BOUNDS, true);
        territory.open(new ChunkPos(0, 1), BOUNDS, false);
        territory.open(new ChunkPos(-1, 0), BOUNDS, true);
        territory.open(new ChunkPos(0, -1), BOUNDS, true);

        // Three held chunks need level 5; at level 3 the island holds two, at level 0 none.
        assertThat(territory.relockFor(3, RULES)).containsExactly(new ChunkPos(0, -1));
        assertThat(territory.relockFor(0, RULES)).containsExactly(new ChunkPos(-1, 0), new ChunkPos(1, 0));
        assertThat(territory.open()).containsExactlyInAnyOrder(ORIGIN, new ChunkPos(0, 1));
    }

    @Test
    void aHeldChunkThatJoinsAPaidOneToTheRestStaysAndTheNextNewestClosesInstead() {
        ChunkTerritory territory = ChunkTerritory.startingAt(ORIGIN);
        territory.open(new ChunkPos(1, 0), BOUNDS, true);
        territory.open(new ChunkPos(0, 1), BOUNDS, true);
        territory.open(new ChunkPos(2, 0), BOUNDS, false);

        assertThat(territory.relockFor(1, RULES)).containsExactly(new ChunkPos(0, 1));
        assertThat(territory.relockFor(0, RULES)).isEmpty();
        assertThat(territory.isOpen(new ChunkPos(1, 0))).isTrue();
    }

    @Test
    void anOperatorClosesAnyOpenChunkButTheFirstAndNoneThatCutsAnotherOff() {
        ChunkTerritory territory = new ChunkTerritory(ORIGIN, List.of(new ChunkPos(1, 0), new ChunkPos(2, 0)));

        assertThat(territory.close(ORIGIN)).isEqualTo(ChunkTerritory.Close.NOT_OPEN);
        assertThat(territory.close(new ChunkPos(1, 0))).isEqualTo(ChunkTerritory.Close.CUTS_OFF);
        assertThat(territory.close(new ChunkPos(2, 0))).isEqualTo(ChunkTerritory.Close.CLOSED);
        assertThat(territory.close(new ChunkPos(1, 0))).isEqualTo(ChunkTerritory.Close.CLOSED);
        assertThat(territory.open()).containsExactly(ORIGIN);
    }

    @Test
    void theLadderGoesOnPastItsListedLevelsByTheStepItNames() {
        assertThat(List.of(RULES.requiredFor(1), RULES.requiredFor(2), RULES.requiredFor(3), RULES.requiredFor(10)))
                .containsExactly(1L, 3L, 5L, 19L);
    }
}
