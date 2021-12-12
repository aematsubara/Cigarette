package me.matsubara.cigarette.file;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Enums;
import com.google.common.base.Strings;
import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.data.Shape;
import me.matsubara.cigarette.data.Smoke;
import me.matsubara.cigarette.util.PluginUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public final class CigaretteTypes {

    private final CigarettePlugin plugin;
    private final Set<CigaretteType> types;

    private File file;
    private FileConfiguration configuration;

    public CigaretteTypes(CigarettePlugin plugin) {
        this.plugin = plugin;
        this.types = new HashSet<>();
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "cigarettes.yml");
        if (!file.exists()) {
            plugin.saveResource("cigarettes.yml", false);
        }
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    private void update() {
        // Remove old recipes.
        for (CigaretteType type : types) {
            Bukkit.removeRecipe(new NamespacedKey(plugin, "cigarette_" + type.getName()));
        }

        types.clear();

        ConfigurationSection section = getConfig().getConfigurationSection("cigarettes");
        if (section == null) return;

        for (String path : section.getKeys(false)) {
            boolean craft = configuration.getBoolean("cigarettes." + path + ".craft");
            String displayName = PluginUtils.translate(configuration.getString("cigarettes." + path + ".display-name"));
            List<String> lore = PluginUtils.translate(configuration.getStringList("cigarettes." + path + ".lore"));

            int duration = configuration.getInt("cigarettes." + path + ".duration");

            String material = configuration.getString("cigarettes." + path + ".material");
            ItemStack item = XMaterial.matchXMaterial(material).get().parseItem();
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            int modelData = configuration.getInt("cigarettes." + path + ".model-data", Integer.MIN_VALUE);
            if (modelData != Integer.MIN_VALUE) meta.setCustomModelData(modelData);

            item.setItemMeta(meta);

            List<String> effects = configuration.getStringList("cigarettes." + path + ".effects");

            Shape shape = null;
            if (craft) {
                boolean shaped = configuration.getBoolean("cigarettes." + path + ".crafting.shaped");
                List<String> ingredients = configuration.getStringList("cigarettes." + path + ".crafting.ingredients");
                List<String> shapeList = configuration.getStringList("cigarettes." + path + ".crafting.shape");

                shape = new Shape(plugin, path, shaped, ingredients, shapeList, item);
            }

            Smoke smoke = getSmoke(path);

            CigaretteType type = new CigaretteType(path, duration, item, effects, shape, smoke);
            types.add(type);
        }
    }

    private Smoke getSmoke(String path) {
        String particleString = configuration.getString("cigarettes." + path + ".particles");

        if (Strings.isNullOrEmpty(particleString) || particleString.equalsIgnoreCase("none")) return null;
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(particleString), ',');
        if (split.length == 0) split = StringUtils.split(particleString, ' ');


        Particle particle = Enums.getIfPresent(Particle.class, split[0]).orNull();
        if (particle == null) return null;

        int amount = 1;
        double randomX = 0.0d, randomY = 0.0d, randomZ = 0.0d, speed = 0.001d;
        if (split.length > 1) {
            amount = Integer.parseInt(split[1]);
            if (split.length > 2) randomX = Double.parseDouble(split[2]);
            if (split.length > 3) randomY = Double.parseDouble(split[3]);
            if (split.length > 4) randomZ = Double.parseDouble(split[4]);
            if (split.length > 5) speed = Double.parseDouble(split[5]);
        }

        return new Smoke(particle, amount, randomX, randomY, randomZ, speed);
    }

    public void reloadConfig() {
        try {
            configuration = new YamlConfiguration();
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    public CigaretteType getTypeByName(String name) {
        for (CigaretteType type : types) {
            if (type.getName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    public Set<CigaretteType> getTypes() {
        return types;
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}