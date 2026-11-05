package com.uxplima.uxmskyblock.bukkit.i18n;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

import com.uxplima.uxmlib.text.language.FilePlayerLanguages;
import com.uxplima.uxmlib.text.language.LanguageChoices;
import com.uxplima.uxmlib.text.language.LanguageResolver;
import com.uxplima.uxmlib.text.language.LanguageServices;
import com.uxplima.uxmlib.text.language.LanguageSettings;

/**
 * The language each player reads: the one the server forces, the one they chose on any UXM plugin, their client's
 * while the server follows it, or the default. Kept once per plugin folder, so a reload that builds the catalogue
 * again keeps every choice and every remembered client language.
 */
public final class PlayerLanguage implements Listener {

    private static final Logger LOGGER = Logger.getLogger(PlayerLanguage.class.getName());
    private static final Map<Path, PlayerLanguage> BY_FOLDER = new ConcurrentHashMap<>();

    private final FilePlayerLanguages store;
    private volatile LanguageSettings settings = LanguageSettings.following(Locale.ENGLISH);
    private volatile Set<Locale> locales = Set.of(Locale.ENGLISH);
    private final LanguageResolver resolver;

    private PlayerLanguage(Path dataDir) {
        this.store = FilePlayerLanguages.loadedFrom(dataDir.resolve("languages.conf"));
        this.resolver = new LanguageResolver(() -> settings, store, LanguageServices::registered);
    }

    /** The one held for {@code dataDir}, now reading {@code languageSettings} and offering {@code found}. */
    public static PlayerLanguage of(Path dataDir, LanguageSettings languageSettings, Set<Locale> found) {
        Objects.requireNonNull(languageSettings, "languageSettings");
        Objects.requireNonNull(found, "found");
        PlayerLanguage language =
                BY_FOLDER.computeIfAbsent(dataDir.toAbsolutePath().normalize(), PlayerLanguage::new);
        language.settings = languageSettings;
        language.locales = Set.copyOf(found);
        return language;
    }

    /** Which language a viewer reads. */
    public LanguageResolver resolver() {
        return resolver;
    }

    /** The settings read last: the default, whether the client decides, and what is forced. */
    public LanguageSettings settings() {
        return settings;
    }

    /** Choosing, clearing and naming languages, over the files this plugin found. */
    public LanguageChoices choices() {
        return new LanguageChoices(() -> locales, resolver, settings);
    }

    /** Writes the choices and remembered client languages that changed since the last write. */
    public void save() {
        if (!store.dirty()) {
            return;
        }
        try {
            store.save();
        } catch (RuntimeException unwritable) {
            LOGGER.log(Level.WARNING, "The language choices could not be written to languages.conf.", unwritable);
        }
    }

    /** A player who joins reads what they chose anywhere on the server, from their first line. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        resolver.settle(event.getPlayer());
    }

    /** The client language a player reported, remembered for the next join. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        resolver.rememberClient(event.getPlayer().getUniqueId(), event.locale());
    }
}
