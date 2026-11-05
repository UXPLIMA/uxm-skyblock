package com.uxplima.uxmskyblock.bukkit.config;

import java.time.Duration;
import java.util.Objects;

import org.spongepowered.configurate.ConfigurationNode;

/**
 * Protection policies and anti-exploit configuration.
 */
public record ProtectionConfiguration(
        boolean obsidianRecoveryEnabled,
        boolean obsidianRecoveryAccidentalOnly,
        Duration obsidianRecoveryExpiration,
        boolean voidRecoveryEnabled,
        boolean voidRecoveryMembers,
        boolean voidRecoveryVisitors,
        int voidRecoveryThresholdY,
        Duration voidRecoveryFallDamageShield,
        Duration voidRecoveryPvpShield,
        boolean kineticWardEnabled,
        double kineticWardRadius,
        double kineticWardForce,
        double kineticWardVerticalLift) {

    public static final boolean DEFAULT_OBSIDIAN_ENABLED = true;
    public static final boolean DEFAULT_OBSIDIAN_ACCIDENTAL = true;
    public static final Duration DEFAULT_OBSIDIAN_EXPIRATION = Duration.ofSeconds(60);

    public static final boolean DEFAULT_VOID_ENABLED = true;
    public static final int DEFAULT_VOID_THRESHOLD_Y = -64;
    public static final Duration DEFAULT_VOID_FALL_SHIELD = Duration.ofSeconds(10);
    public static final Duration DEFAULT_VOID_PVP_SHIELD = Duration.ofSeconds(10);

    public static final boolean DEFAULT_WARD_ENABLED = true;
    public static final double DEFAULT_WARD_RADIUS = 5.0;
    public static final double DEFAULT_WARD_FORCE = 1.5;
    public static final double DEFAULT_WARD_LIFT = 0.35;

    public static ProtectionConfiguration defaultConfiguration() {
        return new ProtectionConfiguration(
                DEFAULT_OBSIDIAN_ENABLED,
                DEFAULT_OBSIDIAN_ACCIDENTAL,
                DEFAULT_OBSIDIAN_EXPIRATION,
                DEFAULT_VOID_ENABLED,
                true,
                true,
                DEFAULT_VOID_THRESHOLD_Y,
                DEFAULT_VOID_FALL_SHIELD,
                DEFAULT_VOID_PVP_SHIELD,
                DEFAULT_WARD_ENABLED,
                DEFAULT_WARD_RADIUS,
                DEFAULT_WARD_FORCE,
                DEFAULT_WARD_LIFT);
    }

    public static ProtectionConfiguration load(ConfigurationNode rootNode) {
        Objects.requireNonNull(rootNode, "rootNode must not be null");
        if (rootNode.virtual() || rootNode.empty()) {
            return defaultConfiguration();
        }

        ConfigurationNode obsNode = rootNode.node("obsidian-recovery");
        boolean obsEnabled = obsNode.node("enabled").getBoolean(DEFAULT_OBSIDIAN_ENABLED);
        boolean obsAccidental = obsNode.node("accidental-only").getBoolean(DEFAULT_OBSIDIAN_ACCIDENTAL);
        int obsExpSec = obsNode.node("expiration-seconds").getInt(60);

        ConfigurationNode voidNode = rootNode.node("void-recovery");
        boolean voidEnabled = voidNode.node("enabled").getBoolean(DEFAULT_VOID_ENABLED);
        int voidThresholdY = voidNode.node("threshold-y").getInt(DEFAULT_VOID_THRESHOLD_Y);
        boolean voidMembers = voidNode.node("members").getBoolean(true);
        boolean voidVisitors = voidNode.node("visitors").getBoolean(true);
        int voidShieldSec = voidNode.node("fall-damage-shield-seconds").getInt(10);
        int voidPvpShieldSec = Math.max(0, voidNode.node("pvp-shield-seconds").getInt(10));

        ConfigurationNode wardNode = rootNode.node("kinetic-ward");
        boolean wardEnabled = wardNode.node("enabled").getBoolean(DEFAULT_WARD_ENABLED);
        double wardRadius = wardNode.node("radius").getDouble(DEFAULT_WARD_RADIUS);
        double wardForce = wardNode.node("repulsion-force").getDouble(DEFAULT_WARD_FORCE);
        double wardLift = wardNode.node("vertical-lift").getDouble(DEFAULT_WARD_LIFT);

        return new ProtectionConfiguration(
                obsEnabled,
                obsAccidental,
                Duration.ofSeconds(obsExpSec),
                voidEnabled,
                voidMembers,
                voidVisitors,
                voidThresholdY,
                Duration.ofSeconds(voidShieldSec),
                Duration.ofSeconds(voidPvpShieldSec),
                wardEnabled,
                wardRadius,
                wardForce,
                wardLift);
    }
}
