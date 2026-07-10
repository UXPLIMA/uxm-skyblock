package net.cengiz1.uxmskyblock.storage;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.config.SettingsManager;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandTime;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SqlStorage implements Storage {

    private final UxmSkyblockPlugin plugin;
    private final SettingsManager settings;
    private final boolean mysql;

    private Connection connection;

    public SqlStorage(UxmSkyblockPlugin plugin, SettingsManager settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.mysql = settings.getStorageType().equals("mysql");
    }

    @Override
    public void init() throws Exception {
        openConnection();
        try (PreparedStatement statement = this.connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS islands (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "owner VARCHAR(36) NOT NULL," +
                        "world VARCHAR(64) NOT NULL," +
                        "grid_index INT NOT NULL," +
                        "center_x INT NOT NULL," +
                        "center_y INT NOT NULL," +
                        "center_z INT NOT NULL," +
                        "home_x DOUBLE NOT NULL," +
                        "home_y DOUBLE NOT NULL," +
                        "home_z DOUBLE NOT NULL," +
                        "home_yaw FLOAT NOT NULL," +
                        "home_pitch FLOAT NOT NULL," +
                        "flags TEXT)")) {
            statement.executeUpdate();
        }

        addColumnIfMissing("flags", "TEXT");
        addColumnIfMissing("name", "VARCHAR(64)");
        addColumnIfMissing("locked", "INT");
        addColumnIfMissing("island_time", "VARCHAR(32)");
        addColumnIfMissing("points", "DOUBLE");
        addColumnIfMissing("level", "INT");
        addColumnIfMissing("members", "TEXT");
        addColumnIfMissing("banned", "TEXT");
        addColumnIfMissing("upgrades", "TEXT");
        addColumnIfMissing("server", "VARCHAR(64)");
        addColumnIfMissing("has_warp", "INT");
        addColumnIfMissing("warp_x", "DOUBLE");
        addColumnIfMissing("warp_y", "DOUBLE");
        addColumnIfMissing("warp_z", "DOUBLE");
        addColumnIfMissing("warp_yaw", "FLOAT");
        addColumnIfMissing("warp_pitch", "FLOAT");
        addColumnIfMissing("border_color", "VARCHAR(16)");
        addColumnIfMissing("bank", "DOUBLE");
        addColumnIfMissing("warps", "TEXT");
        addColumnIfMissing("custom_roles", "TEXT");
        addColumnIfMissing("coop", "TEXT");
        addColumnIfMissing("biome", "VARCHAR(48)");
        addColumnIfMissing("block_limits", "TEXT");
        addColumnIfMissing("bank_interest_at", "BIGINT");

        try (PreparedStatement statement = this.connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS island_bank_log (" +
                        "island_uuid VARCHAR(36) NOT NULL," +
                        "ts BIGINT NOT NULL," +
                        "type VARCHAR(16) NOT NULL," +
                        "actor VARCHAR(36)," +
                        "actor_name VARCHAR(48)," +
                        "amount DOUBLE NOT NULL," +
                        "balance DOUBLE NOT NULL)")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = this.connection.prepareStatement(
                "CREATE INDEX IF NOT EXISTS idx_bank_log_island ON island_bank_log (island_uuid, ts)")) {
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }

        try (PreparedStatement statement = this.connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS island_ratings (" +
                        "island_uuid VARCHAR(36) NOT NULL," +
                        "rater VARCHAR(36) NOT NULL," +
                        "rating INT NOT NULL," +
                        "ts BIGINT NOT NULL," +
                        "PRIMARY KEY (island_uuid, rater))")) {
            statement.executeUpdate();
        }
    }

    private void addColumnIfMissing(String column, String type) {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "ALTER TABLE islands ADD COLUMN " + column + " " + type)) {
            statement.executeUpdate();
        } catch (SQLException ignored) {

        }
    }

    private void openConnection() throws SQLException {
        try {
            if (this.mysql)
                Class.forName("com.mysql.cj.jdbc.Driver");
            else
                Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException error) {
            throw new SQLException("JDBC driver not found: " + error.getMessage());
        }

        if (this.mysql) {
            String url = "jdbc:mysql://" + settings.getHost() + ":" + settings.getPort() + "/" + settings.getDatabase()
                    + "?useSSL=" + settings.isUseSsl() + "&autoReconnect=true&characterEncoding=utf8";
            this.connection = DriverManager.getConnection(url, settings.getUsername(), settings.getPassword());
        } else {
            File file = new File(plugin.getDataFolder(), "data.db");
            file.getParentFile().mkdirs();
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        }
    }

    private Connection connection() throws SQLException {
        if (this.connection == null || this.connection.isClosed())
            openConnection();
        return this.connection;
    }

    @Override
    public synchronized Collection<Island> loadAll() {
        List<Island> islands = new LinkedList<>();
        try (PreparedStatement statement = connection().prepareStatement("SELECT * FROM islands");
             ResultSet result = statement.executeQuery()) {
            while (result.next())
                islands.add(mapRow(result));
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load islands: " + error.getMessage());
        }
        return islands;
    }

    @Override
    public synchronized Island load(UUID islandId) {
        try (PreparedStatement statement = connection().prepareStatement("SELECT * FROM islands WHERE uuid = ?")) {
            statement.setString(1, islandId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next())
                    return mapRow(result);
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load island " + islandId + ": " + error.getMessage());
        }
        return null;
    }

    private Island mapRow(ResultSet result) throws SQLException {
        Island island = new Island(
                UUID.fromString(result.getString("uuid")),
                UUID.fromString(result.getString("owner")),
                result.getString("world"),
                result.getInt("grid_index"),
                result.getInt("center_x"),
                result.getInt("center_y"),
                result.getInt("center_z"));
        island.setHome(
                result.getDouble("home_x"),
                result.getDouble("home_y"),
                result.getDouble("home_z"),
                result.getFloat("home_yaw"),
                result.getFloat("home_pitch"));
        island.loadFlags(result.getString("flags"));

        island.setNameRaw(result.getString("name"));
        island.setLockedRaw(result.getInt("locked") == 1);
        island.setTimeRaw(IslandTime.fromString(result.getString("island_time")));
        island.setPointsRaw(result.getDouble("points"));
        island.setLevelRaw(result.getInt("level"));
        island.loadMembers(result.getString("members"));
        island.loadCustomRoles(result.getString("custom_roles"));
        island.loadBanned(result.getString("banned"));
        island.loadUpgrades(result.getString("upgrades"));
        island.setServerNameRaw(result.getString("server"));
        String warpsData = result.getString("warps");
        if (warpsData != null && !warpsData.isEmpty()) {
            island.loadWarps(warpsData);
        } else {
            island.setWarpRaw(
                    result.getInt("has_warp") == 1,
                    result.getDouble("warp_x"),
                    result.getDouble("warp_y"),
                    result.getDouble("warp_z"),
                    result.getFloat("warp_yaw"),
                    result.getFloat("warp_pitch"));
        }
        island.setBorderColorRaw(result.getString("border_color"));
        island.setBankRaw(result.getDouble("bank"));
        island.loadCoop(result.getString("coop"));
        island.setBiomeRaw(result.getString("biome"));
        island.loadBlockLimits(result.getString("block_limits"));
        island.setBankInterestAtRaw(result.getLong("bank_interest_at"));

        island.markClean();
        return island;
    }

    @Override
    public synchronized void save(Island island) {
        String columns = "uuid, owner, world, grid_index, center_x, center_y, center_z, " +
                "home_x, home_y, home_z, home_yaw, home_pitch, flags, " +
                "name, locked, island_time, points, level, members, banned, upgrades, server, " +
                "has_warp, warp_x, warp_y, warp_z, warp_yaw, warp_pitch, border_color, bank, warps, custom_roles, " +
                "coop, biome, block_limits, bank_interest_at";
        String placeholders = "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
        String updateAssignments = "owner=?, world=?, grid_index=?, center_x=?, center_y=?, center_z=?, " +
                "home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?, flags=?, " +
                "name=?, locked=?, island_time=?, points=?, level=?, members=?, banned=?, upgrades=?, server=?, " +
                "has_warp=?, warp_x=?, warp_y=?, warp_z=?, warp_yaw=?, warp_pitch=?, border_color=?, bank=?, warps=?, custom_roles=?, " +
                "coop=?, biome=?, block_limits=?, bank_interest_at=?";

        String sql = this.mysql
                ? "INSERT INTO islands (" + columns + ") VALUES (" + placeholders + ") " +
                "ON DUPLICATE KEY UPDATE " + updateAssignments
                : "INSERT INTO islands (" + columns + ") VALUES (" + placeholders + ") " +
                "ON CONFLICT(uuid) DO UPDATE SET " + updateAssignments;

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            int i = 1;

            statement.setString(i++, island.getUniqueId().toString());
            i = bindCommon(statement, island, i);

            bindCommon(statement, island, i);

            statement.executeUpdate();
            island.markClean();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not save island " + island.getUniqueId() + ": " + error.getMessage());
        }
    }

    private int bindCommon(PreparedStatement statement, Island island, int i) throws SQLException {
        statement.setString(i++, island.getOwner().toString());
        statement.setString(i++, island.getWorldName());
        statement.setInt(i++, island.getGridIndex());
        statement.setInt(i++, island.getCenterX());
        statement.setInt(i++, island.getCenterY());
        statement.setInt(i++, island.getCenterZ());
        statement.setDouble(i++, island.getHomeX());
        statement.setDouble(i++, island.getHomeY());
        statement.setDouble(i++, island.getHomeZ());
        statement.setFloat(i++, island.getHomeYaw());
        statement.setFloat(i++, island.getHomePitch());
        statement.setString(i++, island.serializeFlags());
        statement.setString(i++, island.getName());
        statement.setInt(i++, island.isLocked() ? 1 : 0);
        statement.setString(i++, island.getTime().name());
        statement.setDouble(i++, island.getPoints());
        statement.setInt(i++, island.getLevel());
        statement.setString(i++, island.serializeMembers());
        statement.setString(i++, island.serializeBanned());
        statement.setString(i++, island.serializeUpgrades());
        statement.setString(i++, island.getServerName());
        statement.setInt(i++, island.hasWarp() ? 1 : 0);
        statement.setDouble(i++, island.getWarpX());
        statement.setDouble(i++, island.getWarpY());
        statement.setDouble(i++, island.getWarpZ());
        statement.setFloat(i++, island.getWarpYaw());
        statement.setFloat(i++, island.getWarpPitch());
        statement.setString(i++, island.getBorderColor());
        statement.setDouble(i++, island.getBank());
        statement.setString(i++, island.serializeWarps());
        statement.setString(i++, island.serializeCustomRoles());
        statement.setString(i++, island.serializeCoop());
        statement.setString(i++, island.getBiome());
        statement.setString(i++, island.serializeBlockLimits());
        statement.setLong(i++, island.getBankInterestAt());
        return i;
    }

    @Override
    public synchronized void delete(UUID islandId) {
        try (PreparedStatement statement = connection().prepareStatement("DELETE FROM islands WHERE uuid = ?")) {
            statement.setString(1, islandId.toString());
            statement.executeUpdate();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not delete island " + islandId + ": " + error.getMessage());
        }
    }

    @Override
    public synchronized void appendBankLog(UUID islandId, BankLogEntry entry) {
        try (PreparedStatement statement = connection().prepareStatement(
                "INSERT INTO island_bank_log (island_uuid, ts, type, actor, actor_name, amount, balance) " +
                        "VALUES (?,?,?,?,?,?,?)")) {
            statement.setString(1, islandId.toString());
            statement.setLong(2, entry.getTimestamp());
            statement.setString(3, entry.getType());
            statement.setString(4, entry.getActor() == null ? null : entry.getActor().toString());
            statement.setString(5, entry.getActorName());
            statement.setDouble(6, entry.getAmount());
            statement.setDouble(7, entry.getBalance());
            statement.executeUpdate();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not append bank log for " + islandId + ": " + error.getMessage());
        }
    }

    @Override
    public synchronized List<BankLogEntry> loadBankLog(UUID islandId, int limit) {
        List<BankLogEntry> entries = new LinkedList<>();
        try (PreparedStatement statement = connection().prepareStatement(
                "SELECT ts, type, actor, actor_name, amount, balance FROM island_bank_log " +
                        "WHERE island_uuid = ? ORDER BY ts DESC LIMIT ?")) {
            statement.setString(1, islandId.toString());
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String actorRaw = result.getString("actor");
                    UUID actor = actorRaw == null ? null : safeUuid(actorRaw);
                    entries.add(new BankLogEntry(
                            result.getLong("ts"),
                            result.getString("type"),
                            actor,
                            result.getString("actor_name"),
                            result.getDouble("amount"),
                            result.getDouble("balance")));
                }
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load bank log for " + islandId + ": " + error.getMessage());
        }
        return entries;
    }

    private UUID safeUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    @Override
    public synchronized void saveRating(UUID islandId, UUID rater, int rating) {
        long now = System.currentTimeMillis();
        String sql = this.mysql
                ? "INSERT INTO island_ratings (island_uuid, rater, rating, ts) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE rating=?, ts=?"
                : "INSERT INTO island_ratings (island_uuid, rater, rating, ts) VALUES (?,?,?,?) " +
                "ON CONFLICT(island_uuid, rater) DO UPDATE SET rating=?, ts=?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, islandId.toString());
            statement.setString(2, rater.toString());
            statement.setInt(3, rating);
            statement.setLong(4, now);
            statement.setInt(5, rating);
            statement.setLong(6, now);
            statement.executeUpdate();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not save rating for " + islandId + ": " + error.getMessage());
        }
    }

    @Override
    public synchronized double[] loadRating(UUID islandId) {
        try (PreparedStatement statement = connection().prepareStatement(
                "SELECT AVG(rating) AS avg_rating, COUNT(*) AS total FROM island_ratings WHERE island_uuid = ?")) {
            statement.setString(1, islandId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next())
                    return new double[]{result.getDouble("avg_rating"), result.getInt("total")};
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load rating for " + islandId + ": " + error.getMessage());
        }
        return new double[]{0, 0};
    }

    @Override
    public synchronized List<UUID> loadTopRated(int limit, int minVotes) {
        List<UUID> ids = new LinkedList<>();
        try (PreparedStatement statement = connection().prepareStatement(
                "SELECT island_uuid, AVG(rating) AS avg_rating, COUNT(*) AS total FROM island_ratings " +
                        "GROUP BY island_uuid HAVING total >= ? ORDER BY avg_rating DESC, total DESC LIMIT ?")) {
            statement.setInt(1, Math.max(1, minVotes));
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID id = safeUuid(result.getString("island_uuid"));
                    if (id != null)
                        ids.add(id);
                }
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load top-rated islands: " + error.getMessage());
        }
        return ids;
    }

    @Override
    public synchronized void close() {
        try {
            if (this.connection != null && !this.connection.isClosed())
                this.connection.close();
        } catch (SQLException ignored) {
        }
    }
}
