package com.uxplima.uxmskyblock.core.application.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.uxplima.uxmskyblock.core.application.island.IslandMutationLock;
import com.uxplima.uxmskyblock.core.application.island.IslandStoragePort;
import com.uxplima.uxmskyblock.core.application.membership.IslandMembershipService.InviteOutcome;
import com.uxplima.uxmskyblock.core.application.membership.IslandMembershipService.JoinOutcome;
import com.uxplima.uxmskyblock.core.application.membership.IslandMembershipService.RemovalOutcome;
import com.uxplima.uxmskyblock.core.application.membership.IslandMembershipService.RoleOutcome;
import com.uxplima.uxmskyblock.core.domain.identity.IslandId;
import com.uxplima.uxmskyblock.core.domain.identity.PlayerUuid;
import com.uxplima.uxmskyblock.core.domain.identity.ProfileId;
import com.uxplima.uxmskyblock.core.domain.island.Island;
import com.uxplima.uxmskyblock.core.domain.island.IslandLocation;
import com.uxplima.uxmskyblock.core.domain.island.IslandMember;
import com.uxplima.uxmskyblock.core.domain.island.IslandRole;
import org.junit.jupiter.api.Test;

/** Who may join, leave, remove and move whom on an island. */
class IslandMembershipServiceTest {

    private static final Instant START = Instant.parse("2026-11-01T12:00:00Z");

    private static final Player OWNER = Player.named(1);
    private static final Player CO_OWNER = Player.named(2);
    private static final Player MODERATOR = Player.named(3);
    private static final Player MEMBER = Player.named(4);
    private static final Player NEWCOMER = Player.named(5);

    private record Player(PlayerUuid uuid, ProfileId profile) {
        static Player named(int n) {
            return new Player(PlayerUuid.of(new UUID(0, n)), ProfileId.of(new UUID(1, n)));
        }

        IslandMember as(IslandRole role) {
            return new IslandMember(uuid, profile, role, START);
        }
    }

    /** Islands kept in memory, found by their members as the database finds them. */
    private static final class InMemoryIslands implements IslandStoragePort {
        private final Map<IslandId, Island> islands = new HashMap<>();
        private final Map<IslandId, IslandLocation> locations = new HashMap<>();

        @Override
        public void saveIsland(Island island, IslandLocation location) {
            islands.put(island.id(), island);
            locations.put(island.id(), location);
        }

        @Override
        public Optional<Island> findIslandById(IslandId id) {
            return Optional.ofNullable(islands.get(id));
        }

        @Override
        public Optional<IslandLocation> findLocationByIslandId(IslandId id) {
            return Optional.ofNullable(locations.get(id));
        }

        @Override
        public Optional<IslandId> findIslandIdByProfileId(ProfileId profileId) {
            return islands.values().stream()
                    .filter(island -> island.isMember(profileId))
                    .map(Island::id)
                    .findFirst();
        }

        @Override
        public void deleteIsland(IslandId id) {
            islands.remove(id);
            locations.remove(id);
        }
    }

    /** A clock a test moves by hand. */
    private static final class HandClock extends Clock {
        private Instant now = START;

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final InMemoryIslands islands = new InMemoryIslands();
    private final HandClock clock = new HandClock();
    private int allowance = 10;
    private final IslandMembershipService service = new IslandMembershipService(
            islands, new IslandMutationLock(), any -> allowance, Duration.ofMinutes(5), clock);

    /** An island with an owner, a co-owner, a moderator and a member. */
    private IslandId fullTeam() {
        IslandId id = IslandId.of(new UUID(2, 1));
        IslandLocation location = IslandLocation.fromCenterAndRadius(id, "skyblock", 0, 0, 50);
        Island created = Island.create(id, location.bounds(), OWNER.uuid(), OWNER.profile(), START);
        Map<ProfileId, IslandMember> members = new HashMap<>(created.members());
        members.put(CO_OWNER.profile(), CO_OWNER.as(IslandRole.CO_OWNER));
        members.put(MODERATOR.profile(), MODERATOR.as(IslandRole.MODERATOR));
        members.put(MEMBER.profile(), MEMBER.as(IslandRole.MEMBER));
        islands.saveIsland(
                new Island(
                        id,
                        created.bounds(),
                        OWNER.uuid(),
                        OWNER.profile(),
                        members,
                        created.roles(),
                        created.flags(),
                        START),
                location);
        return id;
    }

    @Test
    void anInvitedPlayerJoinsWithinTheTimeAndNotAfter() {
        IslandId island = fullTeam();

        assertThat(service.invite(MODERATOR.profile(), NEWCOMER.profile())).isInstanceOf(InviteOutcome.Sent.class);
        clock.now = START.plus(Duration.ofMinutes(4));
        assertThat(service.accept(NEWCOMER.profile(), NEWCOMER.uuid())).isEqualTo(new JoinOutcome.Joined(island));
        assertThat(islands.findIslandIdByProfileId(NEWCOMER.profile())).contains(island);

        service.leave(NEWCOMER.profile());
        service.invite(OWNER.profile(), NEWCOMER.profile());
        clock.now = clock.now.plus(Duration.ofMinutes(6));
        assertThat(service.accept(NEWCOMER.profile(), NEWCOMER.uuid())).isInstanceOf(JoinOutcome.NoInvite.class);
        assertThat(islands.findIslandIdByProfileId(NEWCOMER.profile())).isEmpty();
    }

    @Test
    void aFullIslandTakesNobodyInAndTellsHowManyItAllows() {
        fullTeam();
        allowance = 4;

        assertThat(service.invite(OWNER.profile(), NEWCOMER.profile())).isEqualTo(new InviteOutcome.IslandFull(4));
    }

    @Test
    void aPlayerOnTheIslandOrOnAnotherIsNotInvited() {
        fullTeam();

        assertThat(service.invite(OWNER.profile(), MEMBER.profile())).isInstanceOf(InviteOutcome.AlreadyAMember.class);
        assertThat(service.invite(MEMBER.profile(), NEWCOMER.profile())).isInstanceOf(InviteOutcome.NotAllowed.class);
        assertThat(service.invite(NEWCOMER.profile(), MEMBER.profile())).isInstanceOf(InviteOutcome.NoIsland.class);
    }

    @Test
    void aMemberIsRemovedOnlyByARoleAboveTheirsAndTheOwnerByNobody() {
        IslandId island = fullTeam();

        assertThat(service.kick(MODERATOR.profile(), CO_OWNER.profile())).isInstanceOf(RemovalOutcome.OutOfReach.class);
        assertThat(service.kick(CO_OWNER.profile(), OWNER.profile()))
                .isInstanceOf(RemovalOutcome.CannotRemoveOwner.class);
        assertThat(service.kick(MEMBER.profile(), MODERATOR.profile())).isInstanceOf(RemovalOutcome.NotAllowed.class);
        assertThat(service.kick(MODERATOR.profile(), MEMBER.profile()))
                .isEqualTo(new RemovalOutcome.Removed(island, MEMBER.profile()));
        assertThat(service.kick(MODERATOR.profile(), MEMBER.profile())).isInstanceOf(RemovalOutcome.NotAMember.class);
        assertThat(service.leave(OWNER.profile())).isInstanceOf(RemovalOutcome.CannotRemoveOwner.class);
        assertThat(islands.findIslandById(island).orElseThrow().members().keySet())
                .containsExactlyInAnyOrder(OWNER.profile(), CO_OWNER.profile(), MODERATOR.profile());
    }

    @Test
    void theOwnerMovesAMemberToAnyRoleBetweenTheAnchors() {
        IslandId island = fullTeam();

        assertThat(service.setRole(OWNER.profile(), MEMBER.profile(), "moderator"))
                .isEqualTo(new RoleOutcome.Changed(MEMBER.profile(), "moderator"));
        assertThat(islands.findIslandById(island).orElseThrow().roleOf(MEMBER.profile()))
                .isEqualTo(IslandRole.MODERATOR);
        assertThat(service.setRole(OWNER.profile(), MEMBER.profile(), "visitor"))
                .isInstanceOf(RoleOutcome.NotAssignable.class);
        assertThat(service.setRole(OWNER.profile(), MEMBER.profile(), "pirate"))
                .isInstanceOf(RoleOutcome.UnknownRole.class);
        assertThat(service.setRole(CO_OWNER.profile(), OWNER.profile(), "member"))
                .isInstanceOf(RoleOutcome.CannotChangeOwner.class);
    }
}
