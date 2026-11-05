package com.uxplima.uxmskyblock.bukkit.config;

import java.util.Objects;

import org.spongepowered.configurate.ConfigurationNode;

/** Island warps: how many an island has before upgrades, and how far a safe landing is searched for. */
public record WarpConfiguration(boolean enabled, int baseWarpLimit, int searchRadius) {

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_BASE_WARP_LIMIT = 2;
    public static final int DEFAULT_SEARCH_RADIUS = 5;

    public WarpConfiguration {
        if (baseWarpLimit < 1) {
            throw new IllegalArgumentException("baseWarpLimit must be >= 1: " + baseWarpLimit);
        }
        if (searchRadius < 0) {
            throw new IllegalArgumentException("searchRadius cannot be negative: " + searchRadius);
        }
    }

    public static WarpConfiguration defaultConfiguration() {
        return new WarpConfiguration(DEFAULT_ENABLED, DEFAULT_BASE_WARP_LIMIT, DEFAULT_SEARCH_RADIUS);
    }

    public static WarpConfiguration load(ConfigurationNode rootNode) {
        Objects.requireNonNull(rootNode, "rootNode must not be null");
        ConfigurationNode node = rootNode.node("warps");
        if (node.virtual() || node.empty()) {
            return defaultConfiguration();
        }
        return new WarpConfiguration(
                node.node("enabled").getBoolean(DEFAULT_ENABLED),
                node.node("base-warp-limit").getInt(DEFAULT_BASE_WARP_LIMIT),
                node.node("search-radius").getInt(DEFAULT_SEARCH_RADIUS));
    }
}
