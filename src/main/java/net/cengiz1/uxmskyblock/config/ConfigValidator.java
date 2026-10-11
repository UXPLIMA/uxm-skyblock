package net.cengiz1.uxmskyblock.config;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static List<String> validate(UxmSkyblockPlugin plugin) {
        SettingsManager settings = plugin.getSettings();
        FileConfiguration config = plugin.getConfig();
        List<String> problems = new ArrayList<>();

        if (settings.isIslandEnabled()) {
            if (blank(settings.getWorldName()))
                problems.add("world name is empty");
            if (settings.getIslandDistance() <= 0)
                problems.add("island distance must be greater than 0");
            if (settings.getIslandSize() <= 0)
                problems.add("island size must be greater than 0");
            if (settings.getIslandHeight() < -64 || settings.getIslandHeight() > 320)
                problems.add("island height " + settings.getIslandHeight() + " is outside -64..320");
            if (settings.getCreationThreads() < 1)
                problems.add("creation threads must be at least 1");
            if (settings.getMaxConcurrentCreations() < 1)
                problems.add("max concurrent creations must be at least 1");
        }

        if ("mysql".equalsIgnoreCase(settings.getStorageType())) {
            if (blank(settings.getHost()))
                problems.add("storage is mysql but host is empty");
            if (blank(settings.getDatabase()))
                problems.add("storage is mysql but database is empty");
            if (settings.getPort() < 1 || settings.getPort() > 65535)
                problems.add("mysql port " + settings.getPort() + " is invalid");
        }

        if (settings.isProxyEnabled()) {
            int port = settings.getProxyRedisPort();
            if (port < 1 || port > 65535)
                problems.add("proxy redis port " + port + " is invalid");
            if (blank(settings.getProxyRedisHost()))
                problems.add("proxy redis host is empty");
        }

        double rate = config.getDouble("bank.interest.rate", 0);
        if (rate < 0 || rate > 1)
            problems.add("bank.interest.rate must be between 0 and 1 (is " + rate + ")");
        if (config.getDouble("bank.interest.max-balance", 0) < 0)
            problems.add("bank.interest.max-balance cannot be negative");

        for (String line : config.getStringList("creation.starter-chest.items")) {
            if (line == null || line.isBlank())
                continue;
            String token = line.trim().split("[ :]+")[0];
            if (Material.matchMaterial(token) == null)
                problems.add("starter chest item '" + token + "' is not a valid material");
        }
        return problems;
    }

    public static void report(UxmSkyblockPlugin plugin, List<String> problems) {
        if (problems.isEmpty()) {
            plugin.getLogger().info("Config check passed.");
            return;
        }
        plugin.getLogger().warning("Config check found " + problems.size() + " problem(s):");
        for (String problem : problems)
            plugin.getLogger().warning("  - " + problem);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
