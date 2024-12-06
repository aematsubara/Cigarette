package me.matsubara.cigarette.manager;

import com.cryptomorin.xseries.particles.XParticle;
import lombok.Getter;
import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.cigarette.MaterialType;
import me.matsubara.cigarette.command.MainCommand;
import me.matsubara.cigarette.data.Shape;
import me.matsubara.cigarette.data.Smoke;
import me.matsubara.cigarette.util.PluginUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@Getter
public final class CigaretteManager implements Listener {

    private final CigarettePlugin plugin;
    private final Set<CigaretteType> types = new HashSet<>();
    private final Set<Cigarette> cigarettes = new HashSet<>();

    private File file;
    private FileConfiguration configuration;

    public CigaretteManager(CigarettePlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
        load();
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        extinguishIfPossible(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        extinguishIfPossible(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        extinguishIfPossible(event.getPlayer());
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

        ConfigurationSection section = configuration.getConfigurationSection("cigarettes");
        if (section == null) return;

        for (String path : section.getKeys(false)) {
            boolean craft = configuration.getBoolean("cigarettes." + path + ".craft");

            String tempDisName = configuration.getString("cigarettes." + path + ".display-name");
            String displayName = tempDisName != null ? PluginUtils.translate(tempDisName) : null;

            List<String> lore = PluginUtils.translate(configuration.getStringList("cigarettes." + path + ".lore"));

            int duration = configuration.getInt("cigarettes." + path + ".duration");

            String materialString = configuration.getString("cigarettes." + path + ".material", "TORCH");
            Material material;
            try {
                material = Material.valueOf(materialString.toUpperCase());
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid material type! {" + materialString + "}");
                continue;
            }
            ItemStack item = new ItemStack(material);

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            if (displayName != null) meta.setDisplayName(displayName);
            meta.setLore(lore);

            int modelData = configuration.getInt("cigarettes." + path + ".model-data", Integer.MIN_VALUE);
            if (modelData != Integer.MIN_VALUE) meta.setCustomModelData(modelData);

            // Save the name of the cigarrete to identify the type.
            meta.getPersistentDataContainer().set(plugin.getIdentifier(), PersistentDataType.STRING, path);

            item.setItemMeta(meta);

            List<String> effects = configuration.getStringList("cigarettes." + path + ".effects");

            Shape shape;
            if (craft) {
                boolean shaped = configuration.getBoolean("cigarettes." + path + ".crafting.shaped");
                List<String> ingredients = configuration.getStringList("cigarettes." + path + ".crafting.ingredients");
                List<String> shapeList = configuration.getStringList("cigarettes." + path + ".crafting.shape");
                shape = new Shape(plugin, path, shaped, ingredients, shapeList, item);
            } else shape = null;

            Smoke smoke = getSmoke(path);

            String lightSound = configuration.getString("cigarettes." + path + ".sounds.light", plugin.getConfig().getString("sounds.light"));
            String extinguishSound = configuration.getString("cigarettes." + path + ".sounds.extinguish", plugin.getConfig().getString("sounds.extinguish"));
            String smokeSound = configuration.getString("cigarettes." + path + ".sounds.smoke", plugin.getConfig().getString("sounds.smoke"));

            boolean secondHandSmoke = configuration.getBoolean("cigarettes." + path + ".second-hand-smoke");
            boolean requiresLightning = configuration.getBoolean("cigarettes." + path + ".requires-lighting");
            boolean isSmall = configuration.getBoolean("cigarettes." + path + ".small");

            types.add(new CigaretteType(
                    path,
                    MaterialType.getByMaterial(material, isSmall),
                    duration,
                    item,
                    effects,
                    shape,
                    smoke,
                    lightSound,
                    extinguishSound,
                    smokeSound,
                    secondHandSmoke,
                    requiresLightning));
        }
    }

    private @Nullable Smoke getSmoke(String path) {
        String particleString = configuration.getString("cigarettes." + path + ".particles");

        if (particleString == null || particleString.isEmpty() || particleString.equalsIgnoreCase("none")) return null;
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(particleString), ',');
        if (split.length == 0) split = StringUtils.split(particleString, ' ');

        Particle particle = XParticle
                .of(split[0])
                .filter(XParticle::isSupported)
                .map(XParticle::get)
                .orElse(null);

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

    public void create(@NotNull Player player, @NotNull CigaretteType type) {
        extinguishIfPossible(player);
        cigarettes.add(new Cigarette(plugin, player, type));
        player.sendMessage(plugin.getString(MainCommand.MSG_LIGHT));
    }

    public @Nullable CigaretteType getTypeByName(String name) {
        for (CigaretteType type : types) {
            if (type.getName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    public @Nullable Cigarette getCigarette(Player player) {
        for (Cigarette cigarette : cigarettes) {
            if (cigarette.getOwner().equals(player)) return cigarette;
        }
        return null;
    }

    public void extinguishIfPossible(Player player) {
        Cigarette cigarette = getCigarette(player);
        if (cigarette == null) return;

        cigarette.extinguish();
        player.sendMessage(plugin.getString(MainCommand.MSG_EXTINGUISH));
    }

    public @Nullable CigaretteType getTypeByItem(@NotNull ItemStack item) {
        boolean invalidateOld = plugin.getConfig().getBoolean("invalidate-old-cigarettes");

        ItemMeta meta = item.getItemMeta();
        if (meta == null && invalidateOld) return null;

        NamespacedKey identifier = plugin.getIdentifier();

        String typeName = meta != null ? meta.getPersistentDataContainer().get(identifier, PersistentDataType.STRING) : null;
        if (typeName == null) {
            if (invalidateOld) return null;

            for (CigaretteType type : types) {
                ItemStack clone = type.getItem().clone();
                ItemMeta cloneMeta = clone.getItemMeta();
                if (cloneMeta != null) {
                    cloneMeta.getPersistentDataContainer().remove(identifier);
                    clone.setItemMeta(cloneMeta);
                }
                if (item.isSimilar(clone)) return type;
            }

            return null;
        }

        for (CigaretteType type : types) {
            if (type.getName().equals(typeName)) return type;
        }

        return null;
    }
}