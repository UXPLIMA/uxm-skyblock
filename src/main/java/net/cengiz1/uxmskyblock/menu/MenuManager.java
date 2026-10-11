package net.cengiz1.uxmskyblock.menu;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;
import net.cengiz1.uxmskyblock.config.ConfigMigrator;
import net.cengiz1.uxmskyblock.island.Island;
import net.cengiz1.uxmskyblock.island.IslandFlag;
import net.cengiz1.uxmskyblock.island.IslandManager;
import net.cengiz1.uxmskyblock.island.IslandPermission;
import net.cengiz1.uxmskyblock.island.IslandRole;
import net.cengiz1.uxmskyblock.upgrade.Upgrade;
import net.cengiz1.uxmskyblock.upgrade.UpgradeLevel;
import net.cengiz1.uxmskyblock.upgrade.UpgradeManager;
import net.cengiz1.uxmskyblock.upgrade.UpgradeType;
import net.cengiz1.uxmskyblock.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuManager {

    private static final String[] DEFAULT_MENUS = {
            "main.yml", "settings.yml", "upgrades.yml", "help.yml", "delete-confirm.yml", "warp.yml",
            "blocks.yml", "create.yml"
    };

    private static final Pattern FLAG_PATTERN = Pattern.compile("\\{flag_([a-zA-Z_]+)\\}");

    private final UxmSkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final Map<String, MenuDefinition> menus = new ConcurrentHashMap<>();

    public MenuManager(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.islandManager = plugin.getIslandManager();
        reload();
    }

    public void reload() {
        this.menus.clear();

        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists())
            folder.mkdirs();

        for (String menu : DEFAULT_MENUS)
            ConfigMigrator.sync(plugin, "menus/" + menu);

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(java.util.Locale.ROOT);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            this.menus.put(id, parse(config));
        }
        plugin.getLogger().info("Loaded " + this.menus.size() + " menus.");
    }

    private MenuDefinition parse(YamlConfiguration config) {
        String title = config.getString("title", "Menu");
        int rows = Math.max(1, Math.min(6, config.getInt("rows", 3)));
        String type = config.getString("type", "normal");
        MenuDefinition definition = new MenuDefinition(title, rows, type);

        definition.setUpgradeLore(config.getStringList("upgrade-lore"));
        definition.setUpgradeLoreMax(config.getStringList("upgrade-lore-max"));
        definition.setBlockValuesFormat(config.getString("block-values-format"));

        definition.setHeadSlots(config.getIntegerList("head-slots"));
        definition.setHeadName(config.getString("head-name"));
        definition.setHeadLore(config.getStringList("head-lore"));
        definition.setMemberFormat(config.getString("member-format"));

        definition.setBlockName(config.getString("block-name"));
        definition.setBlockLore(config.getStringList("block-lore"));

        definition.setSchematicName(config.getString("schematic-name"));
        definition.setSchematicLore(config.getStringList("schematic-lore"));

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection entry = items.getConfigurationSection(key);
                if (entry == null)
                    continue;
                int slot = entry.getInt("slot", 0);
                Material material = Material.matchMaterial(entry.getString("material", "STONE"));
                if (material == null)
                    material = Material.STONE;
                int amount = entry.getInt("amount", 1);
                int customModelData = entry.getInt("custom-model-data", -1);
                String name = entry.getString("name", "");
                List<String> lore = entry.getStringList("lore");
                String action = entry.getString("action", null);
                boolean blockValues = entry.getBoolean("block-values", false);
                definition.getEntries().add(
                        new MenuDefinition.Entry(slot, material, amount, customModelData, name, lore, action, blockValues));
            }
        }
        return definition;
    }

    public boolean has(String menuId) {
        return menuId != null && this.menus.containsKey(menuId.toLowerCase(java.util.Locale.ROOT));
    }

    public void open(Player player, String menuId, UUID islandId) {
        open(player, menuId, islandId, 0);
    }

    public void open(Player player, String menuId, UUID islandId, int page) {
        MenuDefinition definition = this.menus.get(menuId.toLowerCase(java.util.Locale.ROOT));
        if (definition == null)
            return;

        Island island = islandId != null
                ? islandManager.getById(islandId)
                : islandManager.getByMember(player.getUniqueId());

        MenuHolder holder = new MenuHolder(menuId.toLowerCase(java.util.Locale.ROOT),
                island != null ? island.getUniqueId() : null, Math.max(0, page));
        Inventory inventory = Bukkit.createInventory(holder, definition.getRows() * 9,
                color(apply(definition.getTitle(), player, island)));
        holder.setInventory(inventory);

        for (MenuDefinition.Entry entry : definition.getEntries()) {
            if (entry.slot < 0 || entry.slot >= inventory.getSize())
                continue;
            inventory.setItem(entry.slot, buildItem(definition, entry, player, island));
            if (entry.action != null && !entry.action.isEmpty())
                holder.getActions().put(entry.slot, entry.action);
        }

        if (definition.getType().equals("upgrades") && island != null)
            populateUpgrades(definition, inventory, holder, player, island);

        if (definition.getType().equals("warp"))
            populateWarp(definition, inventory, holder);

        if (definition.getType().equals("warps"))
            populateWarps(definition, inventory, holder);

        if (definition.getType().equals("blocks"))
            populateBlocks(definition, inventory, holder);

        if (definition.getType().equals("schematics"))
            populateSchematics(definition, inventory, holder);

        player.openInventory(inventory);
    }

    private void populateWarp(MenuDefinition definition, Inventory inventory, MenuHolder holder) {
        List<Integer> slots = definition.getHeadSlots();
        if (slots.isEmpty())
            return;

        List<Island> islands = collectOnlineIslands();
        int perPage = slots.size();
        int pageCount = Math.max(1, (int) Math.ceil(islands.size() / (double) perPage));
        int page = Math.min(holder.getPage(), pageCount - 1);
        holder.setPageCount(pageCount);

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize() || index >= islands.size())
                continue;
            Island island = islands.get(index);
            inventory.setItem(slot, buildHead(definition, island));
            holder.getActions().put(slot, "visit:" + island.getOwner());
        }
    }

    private void populateWarps(MenuDefinition definition, Inventory inventory, MenuHolder holder) {
        List<Integer> slots = definition.getHeadSlots();
        if (slots.isEmpty())
            return;

        List<Island> islands = plugin.getWarpService().getWarps();
        int perPage = slots.size();
        int pageCount = Math.max(1, (int) Math.ceil(islands.size() / (double) perPage));
        int page = Math.min(holder.getPage(), pageCount - 1);
        holder.setPageCount(pageCount);

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize() || index >= islands.size())
                continue;
            Island island = islands.get(index);
            inventory.setItem(slot, buildHead(definition, island));
            holder.getActions().put(slot, "warpto:" + island.getOwner());
        }
    }

    private void populateBlocks(MenuDefinition definition, Inventory inventory, MenuHolder holder) {
        List<Integer> slots = definition.getHeadSlots();
        if (slots.isEmpty())
            return;

        List<Map.Entry<Material, Double>> values =
                new ArrayList<>(plugin.getBlockValueManager().getPositiveValues().entrySet());
        int perPage = slots.size();
        int pageCount = Math.max(1, (int) Math.ceil(values.size() / (double) perPage));
        int page = Math.min(holder.getPage(), pageCount - 1);
        holder.setPageCount(pageCount);

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize() || index >= values.size())
                continue;
            Map.Entry<Material, Double> entry = values.get(index);
            inventory.setItem(slot, buildBlockItem(definition, entry.getKey(), entry.getValue()));
        }
    }

    private void populateSchematics(MenuDefinition definition, Inventory inventory, MenuHolder holder) {
        List<Integer> slots = definition.getHeadSlots();
        if (slots.isEmpty())
            return;

        List<net.cengiz1.uxmskyblock.schematic.SchematicDefinition> schematics =
                new ArrayList<>(islandManager.getSchematicService().getDefinitions());
        int perPage = slots.size();
        int pageCount = Math.max(1, (int) Math.ceil(schematics.size() / (double) perPage));
        int page = Math.min(holder.getPage(), pageCount - 1);
        holder.setPageCount(pageCount);

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize() || index >= schematics.size())
                continue;
            net.cengiz1.uxmskyblock.schematic.SchematicDefinition schematic = schematics.get(index);
            inventory.setItem(slot, buildSchematicItem(definition, schematic));
            holder.getActions().put(slot, "island:create " + schematic.getKey());
        }
    }

    private ItemStack buildSchematicItem(MenuDefinition definition,
                                         net.cengiz1.uxmskyblock.schematic.SchematicDefinition schematic) {
        Material icon = schematic.getIcon() != null && schematic.getIcon().isItem()
                ? schematic.getIcon() : Material.GRASS_BLOCK;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = schematic.getDisplayName();
            String key = schematic.getKey();
            meta.setDisplayName(color(definition.getSchematicName()
                    .replace("{name}", name).replace("{key}", key)));
            List<String> lore = new ArrayList<>();
            for (String line : definition.getSchematicLore())
                lore.add(color(line.replace("{name}", name).replace("{key}", key)));
            if (!lore.isEmpty())
                meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildBlockItem(MenuDefinition definition, Material material, double value) {
        Material icon = material.isItem() ? material : Material.PAPER;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String block = prettyName(material.name());
            String number = formatNumber(value);
            meta.setDisplayName(color(definition.getBlockName()
                    .replace("{block}", block).replace("{value}", number)));
            List<String> lore = new ArrayList<>();
            for (String line : definition.getBlockLore())
                lore.add(color(line.replace("{block}", block).replace("{value}", number)));
            if (!lore.isEmpty())
                meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<Island> collectOnlineIslands() {
        Map<UUID, Island> unique = new LinkedHashMap<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            Island island = islandManager.getByOwner(online.getUniqueId());
            if (island != null)
                unique.put(island.getUniqueId(), island);
        }
        List<Island> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> {
            int byLevel = Integer.compare(b.getLevel(), a.getLevel());
            return byLevel != 0 ? byLevel : Double.compare(b.getPoints(), a.getPoints());
        });
        return result;
    }

    private ItemStack buildHead(MenuDefinition definition, Island island) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) meta;
            skull.setOwningPlayer(owner);
            skull.setDisplayName(color(applyHead(definition.getHeadName(), owner, island)));

            List<String> lore = new ArrayList<>();
            for (String line : definition.getHeadLore()) {
                if (line.contains("{member_list}"))
                    appendMembers(lore, definition.getMemberFormat(), island);
                else
                    lore.add(color(applyHead(line, owner, island)));
            }
            if (!lore.isEmpty())
                skull.setLore(lore);
            head.setItemMeta(skull);
        }
        return head;
    }

    private void appendMembers(List<String> lore, String format, Island island) {
        lore.add(color(format
                .replace("{player}", nameOf(island.getOwner()))
                .replace("{role}", plugin.getRoleManager().owner().getDisplayName())));
        for (Map.Entry<UUID, net.cengiz1.uxmskyblock.island.RoleData> entry : island.getMembers().entrySet())
            lore.add(color(format
                    .replace("{player}", nameOf(entry.getKey()))
                    .replace("{role}", entry.getValue().getDisplayName())));
    }

    private void populateUpgrades(MenuDefinition definition, Inventory inventory, MenuHolder holder,
                                  Player player, Island island) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        for (Upgrade upgrade : upgrades.getUpgrades().values()) {
            int slot = upgrade.getSlot();
            if (slot < 0 || slot >= inventory.getSize())
                continue;

            int currentLevel = island.getUpgradeLevel(upgrade.getKey());
            UpgradeLevel current = upgrade.getLevel(currentLevel);
            UpgradeLevel next = upgrade.getNextLevel(currentLevel);
            boolean maxed = next == null;

            Material icon = upgrade.getIcon();
            if (current != null && current.getIcon() != null)
                icon = current.getIcon();

            ItemStack item = new ItemStack(maxed && icon == null ? Material.BARRIER : icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(upgrade.getDisplayName() + " &7[" + currentLevel + "]"));
                List<String> template = maxed && !definition.getUpgradeLoreMax().isEmpty()
                        ? definition.getUpgradeLoreMax()
                        : definition.getUpgradeLore();
                List<String> lore = new ArrayList<>();
                for (String line : template)
                    lore.add(color(applyUpgrade(line, upgrade, currentLevel, current, next)));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            if (!maxed)
                holder.getActions().put(slot, "upgrade:" + upgrade.getKey());
        }
    }

    public void handleAction(Player player, MenuHolder holder, String action) {
        String lower = action.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("close")) {
            player.closeInventory();
            return;
        }
        if (lower.startsWith("open:")) {
            open(player, action.substring(5).trim(), holder.getIslandId());
            return;
        }
        if (lower.equals("page:next")) {
            open(player, holder.getMenuId(), holder.getIslandId(), holder.getPage() + 1);
            return;
        }
        if (lower.equals("page:prev")) {
            open(player, holder.getMenuId(), holder.getIslandId(), Math.max(0, holder.getPage() - 1));
            return;
        }
        if (lower.startsWith("visit:")) {
            handleVisit(player, action.substring(6).trim());
            return;
        }
        if (lower.startsWith("warpto:")) {
            handleWarpTo(player, action.substring(7).trim());
            return;
        }
        if (lower.startsWith("command:")) {
            player.closeInventory();
            player.performCommand(action.substring(8).trim());
            return;
        }
        if (lower.startsWith("island:")) {
            player.closeInventory();
            player.performCommand(plugin.getSettings().getCommandName() + " " + action.substring(7).trim());
            return;
        }
        if (lower.startsWith("flag:")) {
            handleFlagToggle(player, holder, action.substring(5).trim());
            return;
        }
        if (lower.startsWith("upgrade:")) {
            handleUpgrade(player, holder, action.substring(8).trim());
            return;
        }
        if (lower.equals("toggle-lock")) {
            handleLockToggle(player, holder);
            return;
        }
        if (lower.equals("delete-island")) {
            handleDelete(player, holder);
            return;
        }
        if (lower.equals("time")) {
            handleTimeToggle(player, holder);
        }
    }

    private void handleVisit(Player player, String ownerRaw) {
        UUID ownerId;
        try {
            ownerId = UUID.fromString(ownerRaw);
        } catch (IllegalArgumentException error) {
            return;
        }
        player.closeInventory();
        plugin.getVisitService().visitOwner(player, ownerId);
    }

    private void handleWarpTo(Player player, String ownerRaw) {
        UUID ownerId;
        try {
            ownerId = UUID.fromString(ownerRaw);
        } catch (IllegalArgumentException error) {
            return;
        }
        player.closeInventory();
        plugin.getWarpService().warpToOwner(player, ownerId);
    }

    private void handleDelete(Player player, MenuHolder holder) {
        Island island = islandManager.getByOwner(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            player.closeInventory();
            return;
        }
        player.closeInventory();
        islandManager.deleteIslandConfirmed(player, island);
    }

    private void handleFlagToggle(Player player, MenuHolder holder, String flagName) {
        IslandFlag flag = IslandFlag.fromString(flagName);
        if (flag == null)
            return;
        Island island = resolveIsland(player, holder);
        if (island == null || !island.hasPermission(player.getUniqueId(), IslandPermission.TOGGLE_SETTINGS)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        island.setFlag(flag, !island.getFlag(flag));
        islandManager.saveAsync(island);
        open(player, holder.getMenuId(), island.getUniqueId(), holder.getPage());
    }

    private void handleLockToggle(Player player, MenuHolder holder) {
        Island island = resolveIsland(player, holder);
        if (island == null || !island.hasPermission(player.getUniqueId(), IslandPermission.TOGGLE_SETTINGS)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        island.setLocked(!island.isLocked());
        islandManager.saveAsync(island);
        open(player, holder.getMenuId(), island.getUniqueId(), holder.getPage());
    }

    private void handleTimeToggle(Player player, MenuHolder holder) {
        Island island = resolveIsland(player, holder);
        if (island == null || !island.hasPermission(player.getUniqueId(), IslandPermission.TOGGLE_SETTINGS)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        if (!player.hasPermission("skyblock.time")) {
            plugin.getMessages().send(player, "time-no-permission");
            return;
        }
        island.setTime(island.getTime().next());
        islandManager.saveAsync(island);
        open(player, holder.getMenuId(), island.getUniqueId(), holder.getPage());
    }

    private void handleUpgrade(Player player, MenuHolder holder, String key) {
        Island island = resolveIsland(player, holder);
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.UPGRADE)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        UpgradeManager.PurchaseResult result =
                plugin.getUpgradeManager().purchase(player, island, key, plugin.getEconomy());
        switch (result) {
            case SUCCESS: plugin.getMessages().send(player, "upgrade-success"); break;
            case MAX_LEVEL: plugin.getMessages().send(player, "upgrade-max"); break;
            case NEED_ISLAND_LEVEL: plugin.getMessages().send(player, "upgrade-need-level"); break;
            case NEED_MONEY: plugin.getMessages().send(player, "upgrade-need-money"); break;
            default: plugin.getMessages().send(player, "upgrade-failed"); break;
        }
        open(player, holder.getMenuId(), island.getUniqueId(), holder.getPage());
    }

    private Island resolveIsland(Player player, MenuHolder holder) {
        return holder.getIslandId() != null
                ? islandManager.getById(holder.getIslandId())
                : islandManager.getByMember(player.getUniqueId());
    }

    private ItemStack buildItem(MenuDefinition definition, MenuDefinition.Entry entry, Player player, Island island) {
        ItemStack item = new ItemStack(entry.material, Math.max(1, entry.amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (entry.name != null && !entry.name.isEmpty())
                meta.setDisplayName(color(apply(entry.name, player, island)));

            List<String> lore = new ArrayList<>();
            if (entry.lore != null)
                for (String line : entry.lore)
                    lore.add(color(apply(line, player, island)));

            if (entry.blockValues)
                for (Map.Entry<Material, Double> bv : plugin.getBlockValueManager().getPositiveValues().entrySet())
                    lore.add(color(definition.getBlockValuesFormat()
                            .replace("{block}", prettyName(bv.getKey().name()))
                            .replace("{value}", formatNumber(bv.getValue()))));

            if (!lore.isEmpty())
                meta.setLore(lore);
            applyModelData(meta, entry.customModelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    private void applyModelData(ItemMeta meta, int customModelData) {
        if (meta != null && customModelData >= 0)
            meta.setCustomModelData(customModelData);
    }

    private String applyUpgrade(String text, Upgrade upgrade, int currentLevel, UpgradeLevel current, UpgradeLevel next) {
        if (text == null)
            return "";
        boolean generator = upgrade.getType() == UpgradeType.GENERATOR;
        String currentValue = current != null ? (generator ? "" + currentLevel : formatNumber(current.getValue())) : "-";
        String nextValue = next != null ? (generator ? "" + next.getLevel() : formatNumber(next.getValue())) : "-";

        String result = text;
        result = result.replace("{upgrade}", upgrade.getDisplayName());
        result = result.replace("{level}", String.valueOf(currentLevel));
        result = result.replace("{next_level}", next != null ? String.valueOf(next.getLevel()) : "-");
        result = result.replace("{current}", currentValue);
        result = result.replace("{next}", nextValue);
        result = result.replace("{req_level}", next != null ? String.valueOf(next.getRequiredIslandLevel()) : "-");
        result = result.replace("{req_money}", next != null ? formatNumber(next.getRequiredMoney()) : "-");
        result = result.replace("{max}", String.valueOf(upgrade.getMaxLevel()));
        return result;
    }

    private String apply(String text, Player player, Island island) {
        if (text == null)
            return "";
        String result = text.replace("{player}", player.getName());
        result = result.replace("{owner}", island != null ? nameOf(island.getOwner()) : "-");
        result = result.replace("{level}", island != null ? String.valueOf(island.getLevel()) : "0");
        result = result.replace("{points}", island != null ? formatNumber(island.getPoints()) : "0");
        result = result.replace("{island_name}",
                island != null && island.getName() != null ? island.getName() : nameOf(island != null ? island.getOwner() : player.getUniqueId()));
        result = result.replace("{members}", island != null ? String.valueOf(island.getMemberCount()) : "0");
        result = result.replace("{team_limit}", island != null
                ? String.valueOf((int) plugin.getUpgradeManager().getValue(island, "team-limit", 4)) : "0");
        result = result.replace("{time}", island != null ? island.getTime().getDisplayName() : "");
        result = result.replace("{lock_state}", island != null && island.isLocked()
                ? plugin.getMessages().get("visit-closed")
                : plugin.getMessages().get("visit-open"));

        double next = island != null ? plugin.getLevelManager().pointsForNextLevel(island.getLevel()) : -1;
        result = result.replace("{next_points}", next < 0 ? "MAX" : formatNumber(next));

        result = replaceFlags(result, island);

        result = net.cengiz1.uxmskyblock.placeholder.UxmSkyblockPlaceholders.apply(plugin, player, result);
        return Placeholders.apply(player, result);
    }

    private String replaceFlags(String text, Island island) {
        if (text.indexOf('{') < 0)
            return text;
        String flagOn = plugin.getMessages().get("flag-on");
        String flagOff = plugin.getMessages().get("flag-off");
        Matcher matcher = FLAG_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            IslandFlag flag = IslandFlag.fromString(matcher.group(1));
            String replacement;
            if (flag != null) {
                boolean value = island != null ? island.getFlag(flag) : flag.getDefault();
                replacement = value ? flagOn : flagOff;
            } else {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String applyHead(String text, OfflinePlayer owner, Island island) {
        if (text == null)
            return "";
        String result = text.replace("{owner}", nameOf(island.getOwner()));
        result = result.replace("{level}", String.valueOf(island.getLevel()));
        result = result.replace("{points}", formatNumber(island.getPoints()));
        result = result.replace("{members}", String.valueOf(island.getMemberCount()));
        result = result.replace("{team_limit}",
                String.valueOf((int) plugin.getUpgradeManager().getValue(island, "team-limit", 4)));
        result = result.replace("{lock_state}", island.isLocked()
                ? plugin.getMessages().get("visit-closed")
                : plugin.getMessages().get("visit-open"));
        result = net.cengiz1.uxmskyblock.placeholder.UxmSkyblockPlaceholders.apply(plugin, owner, result);
        return Placeholders.apply(owner, result);
    }

    private String nameOf(UUID id) {
        String name = net.cengiz1.uxmskyblock.util.PlayerNames.nameOf(id);
        return name != null ? name : id.toString().substring(0, 8);
    }

    private String prettyName(String enumName) {
        String[] parts = enumName.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0)
                builder.append(' ');
            if (!part.isEmpty())
                builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
