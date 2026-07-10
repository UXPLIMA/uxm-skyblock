package net.cengiz1.uxmskyblock.storage;

import net.cengiz1.uxmskyblock.island.Island;

import java.util.Collection;
import java.util.UUID;

public interface Storage {

    void init() throws Exception;

    Collection<Island> loadAll();

    Island load(UUID islandId);

    void save(Island island);

    void delete(UUID islandId);

    void appendBankLog(UUID islandId, BankLogEntry entry);

    java.util.List<BankLogEntry> loadBankLog(UUID islandId, int limit);

    void saveRating(UUID islandId, UUID rater, int rating);

    double[] loadRating(UUID islandId);

    java.util.List<UUID> loadTopRated(int limit, int minVotes);

    void close();
}
