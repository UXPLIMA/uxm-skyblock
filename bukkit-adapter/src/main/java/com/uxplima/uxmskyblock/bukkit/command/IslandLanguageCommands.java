package com.uxplima.uxmskyblock.bukkit.command;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.uxplima.uxmlib.command.Cmd;
import com.uxplima.uxmlib.command.Gate;
import com.uxplima.uxmlib.text.language.LanguageChoices;
import com.uxplima.uxmlib.text.language.LanguageNames;
import com.uxplima.uxmskyblock.bukkit.i18n.Messages;
import com.uxplima.uxmskyblock.bukkit.i18n.PlayerLanguage;
import com.uxplima.uxmskyblock.bukkit.permission.CatalogPermissions;

/**
 * {@code /is lang}: the language a player reads, which they choose once for every UXM plugin on the server, and
 * the two branches an operator uses on another player's.
 */
public final class IslandLanguageCommands {

    private final Messages messages;
    private final PlayerLanguage players;

    public IslandLanguageCommands(Messages messages, PlayerLanguage players) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.players = Objects.requireNonNull(players, "players");
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Cmd.literal("lang")
                .executes(this::show)
                .then(Cmd.literal("reset").executes(this::reset))
                .then(Cmd.literal("set-player")
                        .requires(Gate.permission(CatalogPermissions.ADMIN_LANGUAGE.node()))
                        .then(Cmd.argument("player", StringArgumentType.word())
                                .then(Cmd.argument("language", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            players.choices()
                                                    .suggestions(builder.getRemaining())
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::setPlayer))))
                .then(Cmd.literal("reset-player")
                        .requires(Gate.permission(CatalogPermissions.ADMIN_LANGUAGE.node()))
                        .then(Cmd.argument("player", StringArgumentType.word()).executes(this::resetPlayer)))
                .then(Cmd.argument("language", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            players.choices()
                                    .suggestions(builder.getRemaining())
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::choose));
    }

    private int show(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Locale reads = players.resolver().localeOf(sender);
        messages.send(sender, "language.current", Placeholder.unparsed("language", LanguageNames.of(reads)));
        messages.send(sender, "language.choices", Placeholder.unparsed("languages", available()));
        return Cmd.OK;
    }

    private int choose(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "language.players_only");
            return Cmd.OK;
        }
        String typed = StringArgumentType.getString(ctx, "language");
        LanguageChoices choices = players.choices();
        Optional<Locale> wanted = choices.parse(typed);
        if (wanted.isEmpty()) {
            messages.send(
                    sender,
                    "language.unknown",
                    Placeholder.unparsed("input", typed),
                    Placeholder.unparsed("languages", available()));
            return Cmd.OK;
        }
        Optional<Locale> forced = players.settings().forcedLocale();
        if (forced.isPresent()) {
            messages.send(sender, "language.forced", Placeholder.unparsed("language", LanguageNames.of(forced.get())));
            return Cmd.OK;
        }
        String written = LanguageNames.of(wanted.get());
        if (players.resolver().localeOf(player).equals(wanted.get())) {
            messages.send(sender, "language.already", Placeholder.unparsed("language", written));
            return Cmd.OK;
        }
        choices.choose(player.getUniqueId(), wanted.get());
        players.save();
        messages.send(sender, "language.changed", Placeholder.unparsed("language", written));
        return Cmd.OK;
    }

    private int reset(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "language.players_only");
            return Cmd.OK;
        }
        players.choices().reset(player.getUniqueId());
        players.save();
        messages.send(sender, "language.reset");
        return Cmd.OK;
    }

    private int setPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender operator = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        String typed = StringArgumentType.getString(ctx, "language");
        LanguageChoices choices = players.choices();
        Optional<Locale> wanted = choices.parse(typed);
        if (wanted.isEmpty()) {
            messages.send(
                    operator,
                    "language.unknown",
                    Placeholder.unparsed("input", typed),
                    Placeholder.unparsed("languages", available()));
            return Cmd.OK;
        }
        String written = LanguageNames.of(wanted.get());
        switch (choices.named(name)) {
            case LanguageChoices.Named.Unknown unknown ->
                messages.send(operator, "language.unknown_player", Placeholder.unparsed("input", name));
            case LanguageChoices.Named.Online online -> {
                choices.choose(online.player().getUniqueId(), wanted.get());
                messages.send(online.player(), "language.changed", Placeholder.unparsed("language", written));
                messages.send(
                        operator,
                        "language.set_player",
                        Placeholder.unparsed("player", online.player().getName()),
                        Placeholder.unparsed("language", written));
                sayWhenForced(operator);
            }
            case LanguageChoices.Named.Away away -> {
                choices.choose(away.id(), wanted.get());
                messages.send(
                        operator,
                        "language.set_player_away",
                        Placeholder.unparsed("player", away.name()),
                        Placeholder.unparsed("language", written));
                sayWhenForced(operator);
            }
        }
        players.save();
        return Cmd.OK;
    }

    private int resetPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender operator = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        LanguageChoices choices = players.choices();
        switch (choices.named(name)) {
            case LanguageChoices.Named.Unknown unknown ->
                messages.send(operator, "language.unknown_player", Placeholder.unparsed("input", name));
            case LanguageChoices.Named.Online online -> {
                choices.reset(online.player().getUniqueId());
                messages.send(online.player(), "language.reset");
                messages.send(
                        operator,
                        "language.reset_player",
                        Placeholder.unparsed("player", online.player().getName()));
            }
            case LanguageChoices.Named.Away away -> {
                choices.reset(away.id());
                messages.send(operator, "language.reset_player_away", Placeholder.unparsed("player", away.name()));
            }
        }
        players.save();
        return Cmd.OK;
    }

    /** An operator who set a choice the server overrides is told it is kept but not read while forced. */
    private void sayWhenForced(CommandSender operator) {
        players.settings()
                .forcedLocale()
                .ifPresent(locale -> messages.send(
                        operator,
                        "language.forced_anyway",
                        Placeholder.unparsed("language", LanguageNames.of(locale))));
    }

    private String available() {
        return players.choices().available().stream().map(LanguageNames::of).collect(Collectors.joining(", "));
    }
}
