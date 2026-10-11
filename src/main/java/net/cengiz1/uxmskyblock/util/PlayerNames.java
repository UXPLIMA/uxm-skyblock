package net.cengiz1.uxmskyblock.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerNames {

    private static final Map<UUID, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<String, OfflinePlayer> PLAYERS = new ConcurrentHashMap<>();

    private PlayerNames() {
    }

    public static String nameOf(UUID id) {
        String cached = NAMES.get(id);
        if (cached != null)
            return cached;
        String name = Bukkit.getOfflinePlayer(id).getName();
        if (name != null)
            NAMES.put(id, name);
        return name;
    }

    /** Only players who have joined before are cached, so typed junk names cannot grow the map. */
    public static OfflinePlayer offline(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        OfflinePlayer cached = PLAYERS.get(key);
        if (cached != null)
            return cached;
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        if (player.hasPlayedBefore()) {
            PLAYERS.put(key, player);
            if (player.getName() != null)
                NAMES.put(player.getUniqueId(), player.getName());
        }
        return player;
    }
}
