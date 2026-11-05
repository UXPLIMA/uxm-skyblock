package com.uxplima.uxmskyblock.bukkit.acid;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.BrewingRecipe;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.RecipeChoice;

import io.papermc.paper.potion.PotionMix;

/**
 * Brewing a bottle of acid water with coal or charcoal makes it clean.
 *
 * <p>Paper 26.3 brews through {@link BrewingRecipe}, a recipe like any other; Paper 26.2 has no such class and
 * brews through the potion brewer alone. Each way sits in its own nested class, so a server loads only the one
 * it runs.
 */
public final class AcidBrewing {

    private static final boolean RECIPES = hasBrewingRecipes();

    private AcidBrewing() {
        throw new UnsupportedOperationException("AcidBrewing only adds and removes one brewing recipe");
    }

    /** Adds the recipe to the server. */
    public static void add(Server server) {
        if (RECIPES) {
            Recipes.add(server);
        } else {
            Brewer.add(server);
        }
    }

    /** Takes the recipe away again. */
    public static void remove() {
        if (RECIPES) {
            Bukkit.removeRecipe(AcidWater.BREWING);
        } else {
            Brewer.remove();
        }
    }

    private static boolean hasBrewingRecipes() {
        try {
            Class.forName("org.bukkit.inventory.BrewingRecipe", false, AcidBrewing.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final class Recipes {
        static void add(Server server) {
            server.addRecipe(new BrewingRecipe(
                    AcidWater.BREWING,
                    AcidWater.cleanBottle(),
                    RecipeChoice.predicateChoice(AcidWater::isAcid, AcidWater.waterBottle()),
                    RecipeChoice.itemType(ItemType.COAL, ItemType.CHARCOAL)));
        }
    }

    // Paper 26.2 only: the brewer is the one way to brew there, and is marked for removal on later versions.
    @SuppressWarnings({"removal", "deprecation"})
    private static final class Brewer {
        static void add(Server server) {
            server.getPotionBrewer()
                    .addPotionMix(new PotionMix(
                            AcidWater.BREWING,
                            AcidWater.cleanBottle(),
                            PotionMix.createPredicateChoice(AcidWater::isAcid),
                            new RecipeChoice.MaterialChoice(Material.COAL, Material.CHARCOAL)));
        }

        static void remove() {
            Bukkit.getPotionBrewer().removePotionMix(AcidWater.BREWING);
        }
    }
}
