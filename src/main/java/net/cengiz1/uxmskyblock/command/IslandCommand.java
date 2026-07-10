package net.cengiz1.uxmskyblock.command;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandPermission;
import net.cengiz1.uxmskyblock.island.RoleData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IslandCommand extends Command {

    private final UxmSkyblockPlugin plugin;
    private final Map<String, String> resolver;

    private final GeneralCommands general;
    private final MemberCommands members;
    private final SettingsCommands settings;
    private final AdminCommands admin;
    private final WarpCommands warps;
    private final BankCommands bank;
    private final RoleCommands roles;
    private final TopCommands top;

    public IslandCommand(UxmSkyblockPlugin plugin, String name, List<String> aliases, Map<String, String> resolver) {
        super(name);
        this.plugin = plugin;
        this.resolver = resolver;
        this.general = new GeneralCommands(plugin);
        this.members = new MemberCommands(plugin);
        this.settings = new SettingsCommands(plugin);
        this.admin = new AdminCommands(plugin);
        this.warps = new WarpCommands(plugin);
        this.bank = new BankCommands(plugin);
        this.roles = new RoleCommands(plugin, this.members);
        this.top = new TopCommands(plugin);
        setAliases(aliases);
        setDescription("Island commands");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            general.openMenu(player, "main");
            return true;
        }

        String canonical = this.resolver.getOrDefault(args[0].toLowerCase(java.util.Locale.ROOT), null);
        if (canonical == null) {
            plugin.getMessages().send(player, "unknown-subcommand");
            return true;
        }

        String arg1 = args.length >= 2 ? args[1] : null;
        String arg2 = args.length >= 3 ? args[2] : null;

        switch (canonical) {
            case "menu":      general.openMenu(player, "main"); break;
            case "git":       general.home(player); break;
            case "ziyaret":   general.visit(player, arg1); break;
            case "olustur":   general.create(player, arg1); break;
            case "sil":       general.delete(player); break;
            case "level":     general.level(player); break;
            case "bilgi":     general.info(player); break;
            case "uyeler":    general.members(player); break;
            case "yardim":    general.openMenu(player, "help"); break;

            case "davet":     members.invite(player, arg1); break;
            case "kabul":     members.accept(player); break;
            case "reddet":    members.deny(player); break;
            case "ayril":     members.leave(player); break;
            case "at":        members.kick(player, arg1); break;
            case "devret":    members.transfer(player, arg1); break;
            case "ban":       members.ban(player, arg1); break;
            case "unban":     members.unban(player, arg1); break;
            case "trust":     members.trust(player, arg1); break;
            case "untrust":   members.untrust(player, arg1); break;
            case "coop":      members.coop(player, arg1); break;
            case "uncoop":    members.uncoop(player, arg1); break;
            case "rol":
                if (RoleCommands.isManagement(arg1))
                    roles.handle(player, args);
                else
                    members.setRole(player, arg1, arg2);
                break;

            case "ayarlar":   settings.openIslandMenu(player, "settings"); break;
            case "yukseltme": settings.openIslandMenu(player, "upgrades"); break;
            case "setspawn":  settings.setSpawn(player); break;
            case "ucus":      settings.toggleFly(player); break;
            case "kilit":     settings.toggleLock(player); break;
            case "border":    settings.setBorderColor(player, arg1); break;
            case "biome":     settings.setBiome(player, arg1); break;
            case "block":     general.openMenu(player, "blocks"); break;
            case "bank":      bank.handle(player, arg1, arg2); break;
            case "top":       top.handle(player, arg1); break;
            case "puanla":
                if (arg1 != null && (arg1.equalsIgnoreCase("top") || arg1.equalsIgnoreCase("sıralama")
                        || arg1.equalsIgnoreCase("siralama") || arg1.equalsIgnoreCase("lider")))
                    general.ratingTop(player);
                else
                    general.rate(player, arg1);
                break;

            case "warp":      warps.warp(player, arg1, arg2); break;
            case "setwarp":   warps.setWarp(player, arg1); break;
            case "delwarp":   warps.delWarp(player, arg1); break;

            case "admin":     admin.admin(player, args); break;
            case "proxy":     admin.proxyStatus(player); break;
            case "anaspawn":  admin.setGlobalSpawn(player); break;

            default:          plugin.getMessages().send(player, "unknown-subcommand"); break;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 0)
            return result;

        boolean adminAccess = sender instanceof Player && ((Player) sender).hasPermission(AdminCommands.PERMISSION);

        if (args.length == 1) {
            for (String key : this.resolver.keySet()) {
                if (!adminAccess && isAdminOnly(this.resolver.get(key)))
                    continue;
                addIfMatch(result, args[0], key);
            }
            return result;
        }

        String canonical = this.resolver.get(args[0].toLowerCase(java.util.Locale.ROOT));
        if (canonical == null)
            return result;
        Player player = sender instanceof Player ? (Player) sender : null;

        switch (canonical) {
            case "border":
                if (args.length == 2)
                    addAll(result, args[1], "BLUE", "GREEN", "RED");
                break;
            case "bank":
                if (args.length == 2)
                    addAll(result, args[1], "balance", "deposit", "withdraw", "log");
                break;
            case "top":
                if (args.length == 2)
                    addAll(result, args[1], "holo");
                break;
            case "puanla":
                if (args.length == 2)
                    addAll(result, args[1], "1", "2", "3", "4", "5", "top");
                break;
            case "biome":
                if (args.length == 2)
                    for (String biome : plugin.getBiomeService().getAllowed())
                        addIfMatch(result, args[1], biome);
                break;
            case "setwarp":
            case "delwarp":
                if (args.length == 2 && player != null)
                    addWarpNames(result, ownIsland(player), args[1]);
                break;
            case "warp":
                if (args.length == 2)
                    addOnlinePlayers(result, args[1]);
                else if (args.length == 3)
                    addWarpNames(result, islandOfOwnerName(args[1]), args[2]);
                break;
            case "rol":
                completeRole(result, player, args);
                break;
            case "admin":
                completeAdmin(result, player, args);
                break;
            default:
                if (args.length == 2 && isPlayerArg(canonical))
                    addOnlinePlayers(result, args[1]);
        }
        return result;
    }

    private boolean isAdminOnly(String canonical) {
        return "admin".equals(canonical) || "proxy".equals(canonical) || "anaspawn".equals(canonical);
    }

    private void completeAdmin(List<String> result, Player player, String[] args) {
        if (player == null || !admin.hasAccess(player))
            return;
        if (args.length == 2) {
            for (String sub : AdminCommands.SUBCOMMANDS)
                addIfMatch(result, args[1], sub);
            return;
        }
        String sub = args[1].toLowerCase(java.util.Locale.ROOT);
        if (args.length == 3 && !sub.equals("reload") && !sub.equals("help")) {
            addOnlinePlayers(result, args[2]);
        } else if (args.length == 4) {
            if (sub.equals("setowner") || sub.equals("role"))
                addOnlinePlayers(result, args[3]);
            else if (sub.equals("bank"))
                addAll(result, args[3], "set", "add", "remove");
            else if (sub.equals("points"))
                addAll(result, args[3], "set", "add");
        } else if (args.length == 5 && sub.equals("role")) {
            addAssignableRoles(result, islandOfOwnerName(args[2]), args[4]);
        }
    }

    private void completeRole(List<String> result, Player player, String[] args) {
        if (args.length == 2) {
            addAll(result, args[1], "create", "delete", "perm", "list");
            addOnlinePlayers(result, args[1]);
            return;
        }
        String sub = args[1].toLowerCase(java.util.Locale.ROOT);
        if (!RoleCommands.isManagement(sub)) {
            if (args.length == 3)
                addAssignableRoles(result, ownIsland(player), args[2]);
            return;
        }
        if (sub.equals("perm") || sub.equals("izin")) {
            if (args.length == 3)
                addCustomRoles(result, ownIsland(player), args[2]);
            else if (args.length == 4)
                addPermissions(result, args[3]);
            else if (args.length == 5)
                addAll(result, args[4], "on", "off");
        } else if (args.length == 3 && (sub.equals("delete") || sub.equals("sil")
                || sub.equals("kaldir") || sub.equals("kaldır"))) {
            addCustomRoles(result, ownIsland(player), args[2]);
        }
    }

    private Island ownIsland(Player player) {
        return player == null ? null : plugin.getIslandManager().getByMember(player.getUniqueId());
    }

    private Island islandOfOwnerName(String name) {
        OfflinePlayer target = Bukkit.getPlayerExact(name);
        if (target == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            target = offline.hasPlayedBefore() ? offline : null;
        }
        return target == null ? null : plugin.getIslandManager().getByOwner(target.getUniqueId());
    }

    private void addAll(List<String> result, String prefix, String... values) {
        for (String value : values)
            addIfMatch(result, prefix, value);
    }

    private void addOnlinePlayers(List<String> result, String prefix) {
        for (Player online : Bukkit.getOnlinePlayers())
            addIfMatch(result, prefix, online.getName());
    }

    private void addWarpNames(List<String> result, Island island, String prefix) {
        if (island != null)
            for (String name : island.getWarpNames())
                addIfMatch(result, prefix, name);
    }

    private void addAssignableRoles(List<String> result, Island island, String prefix) {
        for (RoleData role : plugin.getRoleManager().assignableBuiltins())
            addIfMatch(result, prefix, role.getId());
        if (island != null)
            for (RoleData role : island.getCustomRoles())
                addIfMatch(result, prefix, role.getId());
    }

    private void addCustomRoles(List<String> result, Island island, String prefix) {
        if (island != null)
            for (RoleData role : island.getCustomRoles())
                addIfMatch(result, prefix, role.getId());
    }

    private void addPermissions(List<String> result, String prefix) {
        for (IslandPermission permission : IslandPermission.values())
            addIfMatch(result, prefix, permission.name());
    }

    private void addIfMatch(List<String> result, String prefix, String value) {
        if (value.toLowerCase(java.util.Locale.ROOT).startsWith(prefix.toLowerCase(java.util.Locale.ROOT)))
            result.add(value);
    }

    private boolean isPlayerArg(String canonical) {
        switch (canonical) {
            case "davet": case "at": case "devret": case "ban":
            case "unban": case "trust": case "untrust": case "rol":
            case "coop": case "uncoop":
            case "ziyaret": case "warp":
                return true;
            default:
                return false;
        }
    }
}
