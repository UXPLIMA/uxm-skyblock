package com.uxplima.uxmskyblock.bukkit.config;

import java.time.Duration;
import java.util.Objects;

import com.uxplima.uxmlib.common.Durations;
import org.spongepowered.configurate.ConfigurationNode;

/** The offline reward inbox: rewards a player could not be handed wait there until they claim them. */
public record RewardInboxConfiguration(boolean enabled, Duration expiryCheckInterval, Duration claimRecoveryWindow) {

    public static final boolean DEFAULT_ENABLED = true;
    public static final Duration DEFAULT_EXPIRY_CHECK_INTERVAL = Duration.ofMinutes(5);
    public static final Duration DEFAULT_CLAIM_RECOVERY_WINDOW = Duration.ofMinutes(2);

    public RewardInboxConfiguration {
        Objects.requireNonNull(expiryCheckInterval, "expiryCheckInterval must not be null");
        if (expiryCheckInterval.isNegative() || expiryCheckInterval.isZero()) {
            throw new IllegalArgumentException("expiryCheckInterval must be positive: " + expiryCheckInterval);
        }
        Objects.requireNonNull(claimRecoveryWindow, "claimRecoveryWindow must not be null");
        if (claimRecoveryWindow.isNegative() || claimRecoveryWindow.isZero()) {
            throw new IllegalArgumentException("claimRecoveryWindow must be positive: " + claimRecoveryWindow);
        }
    }

    public static RewardInboxConfiguration defaultConfiguration() {
        return new RewardInboxConfiguration(
                DEFAULT_ENABLED, DEFAULT_EXPIRY_CHECK_INTERVAL, DEFAULT_CLAIM_RECOVERY_WINDOW);
    }

    public static RewardInboxConfiguration load(ConfigurationNode rootNode) {
        Objects.requireNonNull(rootNode, "rootNode must not be null");
        ConfigurationNode node = rootNode.node("rewards");
        if (node.virtual() || node.empty()) {
            return defaultConfiguration();
        }

        boolean enabled = node.node("enabled").getBoolean(DEFAULT_ENABLED);

        String expiryIntRaw = node.node("expiry-check-interval").getString();
        Duration expiryCheckInterval = expiryIntRaw != null && !expiryIntRaw.isBlank()
                ? Durations.parse(expiryIntRaw)
                : DEFAULT_EXPIRY_CHECK_INTERVAL;

        String recoveryRaw = node.node("claim-recovery-window").getString();
        Duration claimRecoveryWindow = recoveryRaw != null && !recoveryRaw.isBlank()
                ? Durations.parse(recoveryRaw)
                : DEFAULT_CLAIM_RECOVERY_WINDOW;

        return new RewardInboxConfiguration(enabled, expiryCheckInterval, claimRecoveryWindow);
    }
}
