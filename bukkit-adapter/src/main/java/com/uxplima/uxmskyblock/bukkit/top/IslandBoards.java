package com.uxplima.uxmskyblock.bukkit.top;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Function;

import com.uxplima.uxmlib.board.Board;
import com.uxplima.uxmlib.board.BoardPeriod;
import com.uxplima.uxmlib.board.BoardStore;
import com.uxplima.uxmlib.board.BoardText;
import com.uxplima.uxmlib.board.Ranked;
import com.uxplima.uxmlib.board.Snapshot;
import com.uxplima.uxmskyblock.core.application.island.IslandStoragePort;
import com.uxplima.uxmskyblock.core.application.leaderboard.IslandLeaderboardPort;
import com.uxplima.uxmskyblock.core.domain.island.Island;
import com.uxplima.uxmskyblock.core.domain.island.IslandMember;
import com.uxplima.uxmskyblock.core.domain.leaderboard.LeaderboardCategory;
import com.uxplima.uxmskyblock.core.domain.leaderboard.LeaderboardEntry;
import org.jspecify.annotations.Nullable;

/**
 * The island leaderboards on uxmLib's shared board module: level, worth and bank.
 *
 * <p>A place on a board is an island, held under its owner, so a place's name and head are the owner's. A player's
 * own place and value are their island's, whether they own it or are a member of it. Each board is read from the
 * islands as they stand, so it runs for all time only and nothing is counted into a table.
 */
public final class IslandBoards {

    /** The board of island levels. */
    public static final String LEVEL = "level";

    /** The board of island worth, in hundredths. */
    public static final String WORTH = "worth";

    /** The board of island bank balances, in hundredths. */
    public static final String BANK = "bank";

    /** How many islands one reading takes in. A server with more ranks the first this many. */
    private static final int READ_LIMIT = 100_000;

    /** How long one reading answers the three questions a refresh asks of a board. */
    private static final Duration READING_LIFETIME = Duration.ofSeconds(5);

    private final IslandLeaderboardPort leaderboard;
    private final IslandStoragePort islands;
    private final Function<UUID, @Nullable String> names;
    private final Clock clock;
    private final Ranking level;
    private final Ranking worth;
    private final Ranking bank;

    public IslandBoards(
            IslandLeaderboardPort leaderboard, IslandStoragePort islands, Function<UUID, @Nullable String> names) {
        this(leaderboard, islands, names, Clock.systemUTC());
    }

    IslandBoards(
            IslandLeaderboardPort leaderboard,
            IslandStoragePort islands,
            Function<UUID, @Nullable String> names,
            Clock clock) {
        this.leaderboard = Objects.requireNonNull(leaderboard, "leaderboard");
        this.islands = Objects.requireNonNull(islands, "islands");
        this.names = Objects.requireNonNull(names, "names");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.level = new Ranking(LeaderboardCategory.LEVEL);
        this.worth = new Ranking(LeaderboardCategory.WORTH);
        this.bank = new Ranking(LeaderboardCategory.BANK);
    }

    /** The three boards. */
    public List<Board> boards() {
        return List.of(Board.snapshot(LEVEL, level), Board.snapshot(WORTH, worth), Board.snapshot(BANK, bank));
    }

    /** One reading of a board: every island above zero, best first, and the score each member stands at. */
    private record Reading(List<Ranked> places, Map<UUID, Long> byPlayer, Instant at) {}

    private final class Ranking implements Snapshot {

        private final LeaderboardCategory category;
        private volatile @Nullable Reading reading;

        Ranking(LeaderboardCategory category) {
            this.category = category;
        }

        /** The reading a refresh is working with, read again once it is older than a refresh takes. */
        private Reading reading() {
            Reading held = reading;
            Instant now = clock.instant();
            if (held != null && Duration.between(held.at(), now).compareTo(READING_LIFETIME) < 0) {
                return held;
            }
            List<Ranked> places = new ArrayList<>();
            Map<UUID, Long> byPlayer = new HashMap<>();
            for (LeaderboardEntry entry : leaderboard.fetchTopIslands(category, READ_LIMIT)) {
                if (entry.score() <= 0) {
                    continue;
                }
                Island island = islands.findIslandById(entry.islandId()).orElse(null);
                if (island == null) {
                    continue;
                }
                UUID owner = island.ownerPlayerUuid().value();
                places.add(new Ranked(owner, names.apply(owner), entry.score()));
                byPlayer.put(owner, entry.score());
                for (IslandMember member : island.members().values()) {
                    byPlayer.put(member.playerUuid().value(), entry.score());
                }
            }
            places.sort(Comparator.comparingLong(Ranked::score).reversed());
            Reading read = new Reading(List.copyOf(places), Map.copyOf(byPlayer), now);
            reading = read;
            return read;
        }

        @Override
        public List<Ranked> top(int limit) {
            List<Ranked> places = reading().places();
            return places.subList(0, Math.min(limit, places.size()));
        }

        @Override
        public Map<Long, Integer> countByScore() {
            Map<Long, Integer> counts = new HashMap<>();
            for (Ranked place : reading().places()) {
                counts.merge(place.score(), 1, Integer::sum);
            }
            return counts;
        }

        @Override
        public OptionalLong scoreOf(UUID player) {
            Reading held = reading;
            if (held == null) {
                return OptionalLong.empty();
            }
            Long score = held.byPlayer().get(player);
            return OptionalLong.of(score == null ? 0L : score);
        }
    }

    /** A catalogue line as plain text, in the language of the player who reads it, its tokens filled. */
    @FunctionalInterface
    public interface Words {
        String of(String key, @Nullable UUID reader, Map<String, String> values);
    }

    /** The words a board answers with when a place is empty, and for the time until a reset. */
    public static BoardText text(Words words) {
        Objects.requireNonNull(words, "words");
        return new BoardText() {
            @Override
            public String nobody(@Nullable UUID reader) {
                return words.of("placeholder.nobody", reader, Map.of());
            }

            @Override
            public String none(@Nullable UUID reader) {
                return words.of("placeholder.none", reader, Map.of());
            }

            @Override
            public String resets(@Nullable UUID reader, Left left) {
                return switch (left) {
                    case Left.Days days ->
                        words.of(
                                "placeholder.resets_days",
                                reader,
                                Map.of("days", String.valueOf(days.days()), "hours", String.valueOf(days.hours())));
                    case Left.Hours hours ->
                        words.of(
                                "placeholder.resets_hours",
                                reader,
                                Map.of(
                                        "hours",
                                        String.valueOf(hours.hours()),
                                        "minutes",
                                        String.valueOf(hours.minutes())));
                    case Left.Minutes minutes ->
                        words.of(
                                "placeholder.resets_minutes",
                                reader,
                                Map.of("minutes", String.valueOf(minutes.minutes())));
                };
            }
        };
    }

    /** A board store with nothing in it: every board here is read from the islands, never counted. */
    public static BoardStore noHistory() {
        return new BoardStore() {
            @Override
            public void add(UUID player, Board board, Map<BoardPeriod, Long> windows, long amount, long at) {}

            @Override
            public List<Ranked> top(Board board, BoardPeriod period, long window, int limit) {
                return List.of();
            }

            @Override
            public Map<Long, Integer> countByScore(Board board, BoardPeriod period, long window) {
                return Map.of();
            }

            @Override
            public Map<Slot, Long> scoresOf(UUID player, Map<BoardPeriod, Long> windows) {
                return Map.of();
            }

            @Override
            public void forgetBefore(BoardPeriod period, long window) {}

            @Override
            public void erase(UUID player) {}

            @Override
            public void name(UUID player, String name) {}
        };
    }
}
