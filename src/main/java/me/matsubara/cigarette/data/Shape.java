package me.matsubara.cigarette.data;

import com.google.common.base.Strings;
import lombok.Getter;
import me.matsubara.cigarette.CigarettePlugin;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

@SuppressWarnings("unused")
public final class Shape {

    private final CigarettePlugin plugin;

    private final String name;
    private final boolean shaped;
    private final List<String> ingredients;
    private final List<String> shape;

    private @Getter Recipe recipe;

    public Shape(CigarettePlugin plugin, String name, boolean shaped, List<String> ingredients, List<String> shape, ItemStack result) {
        this.plugin = plugin;
        this.name = name;
        this.shaped = shaped;
        this.ingredients = ingredients;
        this.shape = shape;
        register(result);
    }

    public void register(ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "cigarette_" + name);
        recipe = shaped ? new ShapedRecipe(key, item) : new ShapelessRecipe(key, item);

        if (shaped) {
            ((ShapedRecipe) recipe).shape(shape.toArray(new String[0]));
        }

        for (String ingredient : ingredients) {
            if (Strings.isNullOrEmpty(ingredient) || ingredient.equalsIgnoreCase("none")) continue;
            String[] split = StringUtils.split(StringUtils.deleteWhitespace(ingredient), ',');
            if (split.length == 0) split = StringUtils.split(ingredient, ' ');

            Material type = Material.valueOf(split[0]);

            char ingredientKey = ' ';

            if (split.length > 1) {
                ingredientKey = split[1].charAt(0);
            }

            if (shaped) {
                // Empty space is used for AIR.
                if (ingredientKey == ' ') continue;
                ((ShapedRecipe) recipe).setIngredient(ingredientKey, type);
            } else {
                ((ShapelessRecipe) recipe).addIngredient(type);
            }
        }

        Bukkit.addRecipe(recipe);
    }
}