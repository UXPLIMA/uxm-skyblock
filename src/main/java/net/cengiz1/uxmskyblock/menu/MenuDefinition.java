package net.cengiz1.uxmskyblock.menu;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MenuDefinition {

    public static class Entry {
        public final int slot;
        public final Material material;
        public final int amount;
        public final int customModelData;
        public final String name;
        public final List<String> lore;
        public final String action;
        public final boolean blockValues;

        public Entry(int slot, Material material, int amount, int customModelData, String name,
                     List<String> lore, String action, boolean blockValues) {
            this.slot = slot;
            this.material = material;
            this.amount = amount;
            this.customModelData = customModelData;
            this.name = name;
            this.lore = lore;
            this.action = action;
            this.blockValues = blockValues;
        }
    }

    private final String title;
    private final int rows;
    private final String type;
    private final List<Entry> entries = new ArrayList<>();

    private List<String> upgradeLore = new ArrayList<>();
    private List<String> upgradeLoreMax = new ArrayList<>();
    private String blockValuesFormat = "&7• &f{block}: &e{value}";

    private List<Integer> headSlots = new ArrayList<>();
    private String headName = "&f{owner} &7[{level}]";
    private List<String> headLore = new ArrayList<>();
    private String memberFormat = "&8• &f{player} &7{role}";

    private String blockName = "&e{block}";
    private List<String> blockLore = new ArrayList<>();

    private String pointName = "&e{block} &7x{count}";
    private List<String> pointLore = new ArrayList<>();
    private String pointEmpty = "&7Henüz puan veren blok yok.";

    private String schematicName = "&a{name}";
    private List<String> schematicLore = new ArrayList<>();

    public MenuDefinition(String title, int rows, String type) {
        this.title = title;
        this.rows = rows;
        this.type = type == null ? "normal" : type.toLowerCase(java.util.Locale.ROOT);
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public String getType() {
        return type;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<String> getUpgradeLore() {
        return upgradeLore;
    }

    public void setUpgradeLore(List<String> upgradeLore) {
        this.upgradeLore = upgradeLore;
    }

    public List<String> getUpgradeLoreMax() {
        return upgradeLoreMax;
    }

    public void setUpgradeLoreMax(List<String> upgradeLoreMax) {
        this.upgradeLoreMax = upgradeLoreMax;
    }

    public String getBlockValuesFormat() {
        return blockValuesFormat;
    }

    public void setBlockValuesFormat(String blockValuesFormat) {
        if (blockValuesFormat != null)
            this.blockValuesFormat = blockValuesFormat;
    }

    public List<Integer> getHeadSlots() {
        return headSlots;
    }

    public void setHeadSlots(List<Integer> headSlots) {
        if (headSlots != null)
            this.headSlots = headSlots;
    }

    public String getHeadName() {
        return headName;
    }

    public void setHeadName(String headName) {
        if (headName != null)
            this.headName = headName;
    }

    public List<String> getHeadLore() {
        return headLore;
    }

    public void setHeadLore(List<String> headLore) {
        if (headLore != null)
            this.headLore = headLore;
    }

    public String getMemberFormat() {
        return memberFormat;
    }

    public void setMemberFormat(String memberFormat) {
        if (memberFormat != null)
            this.memberFormat = memberFormat;
    }

    public String getPointName() {
        return pointName;
    }

    public void setPointName(String pointName) {
        if (pointName != null && !pointName.isEmpty())
            this.pointName = pointName;
    }

    public List<String> getPointLore() {
        return pointLore;
    }

    public void setPointLore(List<String> pointLore) {
        if (pointLore != null && !pointLore.isEmpty())
            this.pointLore = pointLore;
    }

    public String getPointEmpty() {
        return pointEmpty;
    }

    public void setPointEmpty(String pointEmpty) {
        if (pointEmpty != null && !pointEmpty.isEmpty())
            this.pointEmpty = pointEmpty;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        if (blockName != null && !blockName.isEmpty())
            this.blockName = blockName;
    }

    public List<String> getBlockLore() {
        return blockLore;
    }

    public void setBlockLore(List<String> blockLore) {
        if (blockLore != null && !blockLore.isEmpty())
            this.blockLore = blockLore;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public void setSchematicName(String schematicName) {
        if (schematicName != null && !schematicName.isEmpty())
            this.schematicName = schematicName;
    }

    public List<String> getSchematicLore() {
        return schematicLore;
    }

    public void setSchematicLore(List<String> schematicLore) {
        if (schematicLore != null && !schematicLore.isEmpty())
            this.schematicLore = schematicLore;
    }
}
