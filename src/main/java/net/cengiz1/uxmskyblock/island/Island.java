package net.cengiz1.uxmskyblock.island;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Island {

    /** A single named warp point on the island. */
    public static final class WarpPoint {
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;

        public WarpPoint(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private final UUID uniqueId;
    private UUID owner;
    private final String worldName;
    private final int gridIndex;
    private int centerX;
    private final int centerY;
    private int centerZ;

    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private float homePitch;

    private final Map<String, WarpPoint> warps = new LinkedHashMap<>();

    private String name;
    private boolean locked;
    private IslandTime time = IslandTime.NORMAL;
    private String borderColor;

    private String serverName;

    private double points;
    private int level;
    private double bank;
    private long bankInterestAt;
    private String biome;
    private final Map<String, Integer> blockLimits = new ConcurrentHashMap<>();

    private final EnumMap<IslandFlag, Boolean> flags = new EnumMap<>(IslandFlag.class);
    private final Map<UUID, String> members = new ConcurrentHashMap<>();
    private final Map<String, RoleData> customRoles = new ConcurrentHashMap<>();
    private final Set<UUID> banned = ConcurrentHashMap.newKeySet();
    private final Set<UUID> coop = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> upgrades = new ConcurrentHashMap<>();
    private static RoleResolver resolver;

    private boolean dirty;

    public Island(UUID uniqueId, UUID owner, String worldName, int gridIndex,
                  int centerX, int centerY, int centerZ) {
        this.uniqueId = uniqueId;
        this.owner = owner;
        this.worldName = worldName;
        this.gridIndex = gridIndex;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.homeX = centerX + 0.5;
        this.homeY = centerY + 1;
        this.homeZ = centerZ + 0.5;
        for (IslandFlag flag : IslandFlag.values())
            this.flags.put(flag, flag.getDefault());
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.dirty = true;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getGridIndex() {
        return gridIndex;
    }

    public void relocateCenter(int centerX, int centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.dirty = true;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public Location getCenter(World world) {
        return new Location(world, centerX + 0.5, centerY, centerZ + 0.5);
    }

    public Location getHome(World world) {
        return new Location(world, homeX, homeY, homeZ, homeYaw, homePitch);
    }

    public void setHome(Location location) {
        this.homeX = location.getX();
        this.homeY = location.getY();
        this.homeZ = location.getZ();
        this.homeYaw = location.getYaw();
        this.homePitch = location.getPitch();
        this.dirty = true;
    }

    public void setHome(double x, double y, double z, float yaw, float pitch) {
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
    }

    public boolean hasWarp() {
        return !warps.isEmpty();
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(key(name));
    }

    public int getWarpCount() {
        return warps.size();
    }

    public Set<String> getWarpNames() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    /** The name of the default warp (the first one set), or null if there are none. */
    public String getDefaultWarpName() {
        return warps.isEmpty() ? null : warps.keySet().iterator().next();
    }

    private WarpPoint resolveWarp(String name) {
        if (warps.isEmpty())
            return null;
        if (name == null || name.isEmpty())
            return warps.values().iterator().next();
        return warps.get(key(name));
    }

    /** The default warp location (first warp set), for callers that don't pick a name. */
    public Location getWarp(World world) {
        return getWarp(world, null);
    }

    public Location getWarp(World world, String name) {
        WarpPoint point = resolveWarp(name);
        if (point == null)
            return null;
        return new Location(world, point.x, point.y, point.z, point.yaw, point.pitch);
    }

    public void setWarp(String name, Location location) {
        this.warps.put(key(name), new WarpPoint(
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));
        this.dirty = true;
    }

    /** Legacy single-warp setter; stores under the default name. */
    public void setWarp(Location location) {
        setWarp("ada", location);
    }

    public boolean removeWarp(String name) {
        if (this.warps.remove(key(name)) != null) {
            this.dirty = true;
            return true;
        }
        return false;
    }

    public void clearWarp() {
        if (!this.warps.isEmpty()) {
            this.warps.clear();
            this.dirty = true;
        }
    }

    /** Restores a legacy single warp (from the old per-coordinate DB columns). */
    public void setWarpRaw(boolean hasWarp, double x, double y, double z, float yaw, float pitch) {
        if (hasWarp)
            this.warps.put("ada", new WarpPoint(x, y, z, yaw, pitch));
    }

    private WarpPoint defaultWarp() {
        return resolveWarp(null);
    }

    public double getWarpX() {
        WarpPoint p = defaultWarp();
        return p == null ? 0 : p.x;
    }

    public double getWarpY() {
        WarpPoint p = defaultWarp();
        return p == null ? 0 : p.y;
    }

    public double getWarpZ() {
        WarpPoint p = defaultWarp();
        return p == null ? 0 : p.z;
    }

    public float getWarpYaw() {
        WarpPoint p = defaultWarp();
        return p == null ? 0 : p.yaw;
    }

    public float getWarpPitch() {
        WarpPoint p = defaultWarp();
        return p == null ? 0 : p.pitch;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String serializeWarps() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, WarpPoint> entry : this.warps.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            WarpPoint p = entry.getValue();
            builder.append(entry.getKey()).append(',')
                    .append(p.x).append(',').append(p.y).append(',').append(p.z).append(',')
                    .append(p.yaw).append(',').append(p.pitch);
        }
        return builder.toString();
    }

    public void loadWarps(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] f = part.split(",");
            if (f.length != 6)
                continue;
            try {
                this.warps.put(key(f[0]), new WarpPoint(
                        Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                        Float.parseFloat(f[4]), Float.parseFloat(f[5])));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public double getHomeX() {
        return homeX;
    }

    public double getHomeY() {
        return homeY;
    }

    public double getHomeZ() {
        return homeZ;
    }

    public float getHomeYaw() {
        return homeYaw;
    }

    public float getHomePitch() {
        return homePitch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public void setNameRaw(String name) {
        this.name = name;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        this.dirty = true;
    }

    public void setLockedRaw(boolean locked) {
        this.locked = locked;
    }

    /** Per-island border color (BLUE/GREEN/RED), or null to inherit the config default. */
    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
        this.dirty = true;
    }

    public void setBorderColorRaw(String borderColor) {
        this.borderColor = borderColor;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
        this.dirty = true;
    }

    public void setServerNameRaw(String serverName) {
        this.serverName = serverName;
    }

    public IslandTime getTime() {
        return time;
    }

    public void setTime(IslandTime time) {
        this.time = time == null ? IslandTime.NORMAL : time;
        this.dirty = true;
    }

    public void setTimeRaw(IslandTime time) {
        this.time = time == null ? IslandTime.NORMAL : time;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = Math.max(0, points);
        this.dirty = true;
    }

    public void setPointsRaw(double points) {
        this.points = Math.max(0, points);
    }

    public void addPoints(double amount) {
        setPoints(this.points + amount);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
        this.dirty = true;
    }

    public void setLevelRaw(int level) {
        this.level = Math.max(0, level);
    }

    public double getBank() {
        return bank;
    }

    public void setBank(double bank) {
        this.bank = Math.max(0, bank);
        this.dirty = true;
    }

    public void setBankRaw(double bank) {
        this.bank = Math.max(0, bank);
    }

    public void depositBank(double amount) {
        setBank(this.bank + amount);
    }

    /** Withdraw up to {@code amount} from the bank; returns the amount actually removed. */
    public double withdrawBank(double amount) {
        double taken = Math.min(Math.max(0, amount), this.bank);
        setBank(this.bank - taken);
        return taken;
    }

    public boolean getFlag(IslandFlag flag) {
        return this.flags.getOrDefault(flag, flag.getDefault());
    }

    public void setFlag(IslandFlag flag, boolean value) {
        this.flags.put(flag, value);
        this.dirty = true;
    }

    public static void setResolver(RoleResolver roleResolver) {
        resolver = roleResolver;
    }

    private RoleData resolveRole(String id) {
        if (id != null) {
            RoleData custom = customRoles.get(id.toLowerCase(java.util.Locale.ROOT));
            if (custom != null)
                return custom;
            if (resolver != null) {
                RoleData builtin = resolver.builtin(id);
                if (builtin != null)
                    return builtin;
            }
        }
        return resolver != null ? resolver.visitor() : null;
    }

    /** Resolve a role id against this island's custom roles, then the built-ins. */
    public RoleData resolveRoleById(String id) {
        if (id == null)
            return null;
        RoleData custom = customRoles.get(id.toLowerCase(java.util.Locale.ROOT));
        if (custom != null)
            return custom;
        return resolver != null ? resolver.builtin(id) : null;
    }

    public Map<UUID, RoleData> getMembers() {
        Map<UUID, RoleData> result = new LinkedHashMap<>();
        for (Map.Entry<UUID, String> entry : members.entrySet())
            result.put(entry.getKey(), resolveRole(entry.getValue()));
        return Collections.unmodifiableMap(result);
    }

    public Set<UUID> getAllMemberIds() {
        Set<UUID> ids = new HashSet<>(members.keySet());
        ids.add(owner);
        return ids;
    }

    public boolean isOwner(UUID id) {
        return owner.equals(id);
    }

    public boolean isMember(UUID id) {
        return owner.equals(id) || members.containsKey(id);
    }

    public RoleData getRole(UUID id) {
        if (owner.equals(id))
            return resolver != null ? resolver.owner() : null;
        String roleId = members.get(id);
        return resolveRole(roleId);
    }

    /** The raw stored role id for a member (lowercase), or null. */
    public String getRoleId(UUID id) {
        if (owner.equals(id))
            return RoleManager.OWNER_ID;
        return members.get(id);
    }

    public void setRole(UUID id, String roleId) {
        if (owner.equals(id) || roleId == null)
            return;
        if (roleId.equalsIgnoreCase(RoleManager.VISITOR_ID))
            return;
        this.members.put(id, roleId.toLowerCase(java.util.Locale.ROOT));
        this.dirty = true;
    }

    public void addMember(UUID id) {
        setRole(id, RoleManager.MEMBER_ID);
    }


    public RoleData getCustomRole(String id) {
        return id == null ? null : customRoles.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean hasCustomRole(String id) {
        return id != null && customRoles.containsKey(id.toLowerCase(java.util.Locale.ROOT));
    }

    public java.util.Collection<RoleData> getCustomRoles() {
        return Collections.unmodifiableCollection(customRoles.values());
    }

    public RoleData createCustomRole(String id, String displayName, int weight) {
        RoleData data = new RoleData(id, displayName, weight, false);
        customRoles.put(data.getId(), data);
        this.dirty = true;
        return data;
    }

    public boolean removeCustomRole(String id) {
        if (id == null)
            return false;
        String key = id.toLowerCase(java.util.Locale.ROOT);
        if (customRoles.remove(key) == null)
            return false;

        for (Map.Entry<UUID, String> entry : members.entrySet())
            if (key.equals(entry.getValue()))
                entry.setValue(RoleManager.MEMBER_ID);
        this.dirty = true;
        return true;
    }

    public void removeMember(UUID id) {
        if (this.members.remove(id) != null)
            this.dirty = true;
    }

    public boolean hasPermission(UUID id, IslandPermission permission) {
        return getRole(id).has(permission);
    }

    public int getMemberCount() {
        return members.size() + 1;
    }

    public Set<UUID> getBanned() {
        return Collections.unmodifiableSet(banned);
    }

    public boolean isBanned(UUID id) {
        return banned.contains(id);
    }

    public void ban(UUID id) {
        if (banned.add(id)) {
            members.remove(id);
            this.dirty = true;
        }
    }

    public void unban(UUID id) {
        if (banned.remove(id))
            this.dirty = true;
    }

    public long getBankInterestAt() {
        return bankInterestAt;
    }

    public void setBankInterestAt(long timestamp) {
        this.bankInterestAt = timestamp;
        this.dirty = true;
    }

    public void setBankInterestAtRaw(long timestamp) {
        this.bankInterestAt = timestamp;
    }

    public String getBiome() {
        return biome;
    }

    public void setBiome(String biome) {
        this.biome = biome;
        this.dirty = true;
    }

    public void setBiomeRaw(String biome) {
        this.biome = biome;
    }

    public int getBlockLimitCount(String material) {
        return this.blockLimits.getOrDefault(material.toUpperCase(java.util.Locale.ROOT), 0);
    }

    public int addBlockLimitCount(String material, int delta) {
        String key = material.toUpperCase(java.util.Locale.ROOT);
        int next = Math.max(0, this.blockLimits.getOrDefault(key, 0) + delta);
        if (next == 0)
            this.blockLimits.remove(key);
        else
            this.blockLimits.put(key, next);
        this.dirty = true;
        return next;
    }

    public String serializeBlockLimits() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : this.blockLimits.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    public void loadBlockLimits(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2)
                continue;
            try {
                this.blockLimits.put(kv[0].toUpperCase(java.util.Locale.ROOT), Integer.parseInt(kv[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public boolean isCoop(UUID id) {
        return coop.contains(id);
    }

    public boolean addCoop(UUID id) {
        if (coop.add(id)) {
            this.dirty = true;
            return true;
        }
        return false;
    }

    public boolean removeCoop(UUID id) {
        if (coop.remove(id)) {
            this.dirty = true;
            return true;
        }
        return false;
    }

    public Set<UUID> getCoop() {
        return Collections.unmodifiableSet(coop);
    }

    public void clearCoop() {
        if (!coop.isEmpty()) {
            coop.clear();
            this.dirty = true;
        }
    }

    public String serializeCoop() {
        StringBuilder builder = new StringBuilder();
        for (UUID id : this.coop) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(id);
        }
        return builder.toString();
    }

    public void loadCoop(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            try {
                this.coop.add(UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean hasAccess(UUID id) {
        return isMember(id) || coop.contains(id);
    }

    public int getUpgradeLevel(String key) {
        return this.upgrades.getOrDefault(key.toLowerCase(java.util.Locale.ROOT), 1);
    }

    public void setUpgradeLevel(String key, int level) {
        this.upgrades.put(key.toLowerCase(java.util.Locale.ROOT), Math.max(1, level));
        this.dirty = true;
    }

    public Map<String, Integer> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    public String serializeFlags() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<IslandFlag, Boolean> entry : this.flags.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey().name()).append('=').append(entry.getValue() ? 1 : 0);
        }
        return builder.toString();
    }

    public void loadFlags(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] keyValue = part.split("=");
            if (keyValue.length != 2)
                continue;
            IslandFlag flag = IslandFlag.fromString(keyValue[0]);
            if (flag != null)
                this.flags.put(flag, keyValue[1].equals("1"));
        }
    }

    public String serializeMembers() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<UUID, String> entry : this.members.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return builder.toString();
    }

    public void loadMembers(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            int idx = part.indexOf(':');
            if (idx < 0)
                continue;
            try {
                UUID uuid = UUID.fromString(part.substring(0, idx).trim());
                String roleId = part.substring(idx + 1).trim().toLowerCase(java.util.Locale.ROOT);
                if (!roleId.isEmpty() && !roleId.equals(RoleManager.VISITOR_ID))
                    this.members.put(uuid, roleId);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public String serializeCustomRoles() {
        StringBuilder builder = new StringBuilder();
        for (RoleData role : this.customRoles.values()) {
            if (builder.length() > 0)
                builder.append(';');
            StringBuilder perms = new StringBuilder();
            for (IslandPermission permission : role.getPermissions()) {
                if (perms.length() > 0)
                    perms.append(',');
                perms.append(permission.name());
            }
            builder.append(role.getId()).append('|')
                    .append(role.getDisplayName().replace("|", " ").replace(";", " ")).append('|')
                    .append(role.getWeight()).append('|').append(perms);
        }
        return builder.toString();
    }

    public void loadCustomRoles(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] f = part.split("\\|", 4);
            if (f.length < 3)
                continue;
            int weight;
            try {
                weight = Integer.parseInt(f[2].trim());
            } catch (NumberFormatException error) {
                weight = 1;
            }
            RoleData role = new RoleData(f[0], f[1], weight, false);
            if (f.length == 4 && !f[3].isEmpty()) {
                for (String permName : f[3].split(",")) {
                    IslandPermission permission = IslandPermission.fromString(permName);
                    if (permission != null)
                        role.setPermission(permission, true);
                }
            }
            this.customRoles.put(role.getId(), role);
        }
    }

    public String serializeBanned() {
        StringBuilder builder = new StringBuilder();
        for (UUID id : this.banned) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(id);
        }
        return builder.toString();
    }

    public void loadBanned(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            try {
                this.banned.add(UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public String serializeUpgrades() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : this.upgrades.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    public void loadUpgrades(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2)
                continue;
            try {
                this.upgrades.put(kv[0].toLowerCase(java.util.Locale.ROOT), Integer.parseInt(kv[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }
}
