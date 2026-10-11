package net.cengiz1.uxmskyblock.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class ConfigMigrator {

    private ConfigMigrator() {
    }

    public static void sync(JavaPlugin plugin, String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
            return;
        }

        InputStream resource = plugin.getResource(resourcePath);
        if (resource == null)
            return;

        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException error) {
            plugin.getLogger().warning("Could not read bundled resource " + resourcePath + ": " + error.getMessage());
            return;
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration additions = new YamlConfiguration();
        List<String> manual = new ArrayList<>();
        int added = 0;

        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || current.contains(key))
                continue;
            String top = key.split("\\.")[0];
            if (current.contains(top)) {
                manual.add(key + ": " + defaults.get(key));
            } else {
                additions.set(key, defaults.get(key));
                added++;
            }
        }

        if (added > 0) {
            String block = "\n# Added automatically by uxmSkyblock update\n" + additions.saveToString();
            try {
                Files.write(file.toPath(), block.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                plugin.getLogger().info("Added " + added + " new key(s) to " + resourcePath + ".");
            } catch (IOException error) {
                plugin.getLogger().warning("Could not update " + resourcePath + ": " + error.getMessage());
            }
        }

        if (!manual.isEmpty()) {
            plugin.getLogger().warning(resourcePath + " is missing these keys inside sections you already have. "
                    + "Add them by hand:");
            for (String line : manual)
                plugin.getLogger().warning("  " + line);
        }
    }
}
