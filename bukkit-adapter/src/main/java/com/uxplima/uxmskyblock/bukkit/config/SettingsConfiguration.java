package com.uxplima.uxmskyblock.bukkit.config;

import java.util.Objects;

import org.spongepowered.configurate.ConfigurationNode;

/** What happens at an island's edge and on islands with no member online, read from {@code settings.conf}. */
public record SettingsConfiguration(
        boolean stopBorderCrossing, boolean stopRedstoneOffline, boolean stopSpawnersOffline) {

    public static SettingsConfiguration defaultConfiguration() {
        return new SettingsConfiguration(true, true, true);
    }

    public static SettingsConfiguration load(ConfigurationNode rootNode) {
        Objects.requireNonNull(rootNode, "rootNode must not be null");
        SettingsConfiguration defaults = defaultConfiguration();
        ConfigurationNode offline = rootNode.node("offline-islands");
        return new SettingsConfiguration(
                rootNode.node("border", "stop-crossing").getBoolean(defaults.stopBorderCrossing()),
                offline.node("stop-redstone").getBoolean(defaults.stopRedstoneOffline()),
                offline.node("stop-spawners").getBoolean(defaults.stopSpawnersOffline()));
    }

    /** Whether anything of an island with no member online is held still. */
    public boolean anythingOffline() {
        return stopRedstoneOffline || stopSpawnersOffline;
    }
}
