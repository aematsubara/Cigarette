package me.matsubara.cigarette;

import com.google.common.collect.ImmutableList;
import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.command.MainCommand;
import me.matsubara.cigarette.listener.InventoryClick;
import me.matsubara.cigarette.listener.PlayerDeathOrQuit;
import me.matsubara.cigarette.listener.PlayerInteract;
import me.matsubara.cigarette.listener.PlayerItemConsume;
import me.matsubara.cigarette.manager.CigaretteManager;
import me.matsubara.cigarette.util.PluginUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public final class CigarettePlugin extends JavaPlugin {

    private CigaretteManager cigaretteManager;
    private final NamespacedKey identifier = new NamespacedKey(this, "cigarette-type");

    private static final List<String> SPECIAL_SECTIONS = Collections.emptyList();

    @Override
    public void onEnable() {
        PluginManager manager = getServer().getPluginManager();
        if (PluginUtils.MINOR_VERSION < 15) {
            getLogger().severe("This plugin only works from 1.15.2 and up, disabling...");
            manager.disablePlugin(this);
            return;
        }

        cigaretteManager = new CigaretteManager(this);

        manager.registerEvents(new InventoryClick(this), this);
        manager.registerEvents(new PlayerDeathOrQuit(this), this);
        manager.registerEvents(new PlayerInteract(this), this);
        manager.registerEvents(new PlayerItemConsume(this), this);

        saveDefaultConfig();
        updateMainConfig();

        PluginCommand mainCommand = getCommand("cigarette");
        if (mainCommand == null) return;

        MainCommand main = new MainCommand(this);
        mainCommand.setExecutor(main);
        mainCommand.setTabCompleter(main);
    }

    public void updateMainConfig() {
        updateConfig(
                getDataFolder().getPath(),
                "config.yml",
                file -> reloadConfig(),
                file -> saveDefaultConfig(),
                config -> SPECIAL_SECTIONS.stream().filter(config::contains).toList(),
                Collections.emptyList());
    }

    public void updateConfig(String folderName,
                             String fileName,
                             Consumer<File> reloadAfterUpdating,
                             Consumer<File> resetConfiguration,
                             Function<FileConfiguration, List<String>> ignoreSection,
                             List<ConfigChanges> changes) {
        File file = new File(folderName, fileName);

        FileConfiguration config = PluginUtils.reloadConfig(this, file, resetConfiguration);
        if (config == null) {
            getLogger().severe("Can't find {" + file.getName() + "}!");
            return;
        }

        for (ConfigChanges change : changes) {
            handleConfigChanges(file, config, change.predicate(), change.consumer(), change.newVersion());
        }

        try {
            ConfigUpdater.update(
                    this,
                    fileName,
                    file,
                    ignoreSection.apply(config));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        reloadAfterUpdating.accept(file);
    }

    private void handleConfigChanges(@NotNull File file, FileConfiguration config, @NotNull Predicate<FileConfiguration> predicate, Consumer<FileConfiguration> consumer, int newVersion) {
        if (!predicate.test(config)) return;

        int previousVersion = config.getInt("config-version", 0);
        getLogger().info("Updated {%s} config to v{%s} (from v{%s})".formatted(file.getName(), newVersion, previousVersion));

        consumer.accept(config);
        config.set("config-version", newVersion);

        try {
            config.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public record ConfigChanges(Predicate<FileConfiguration> predicate,
                                Consumer<FileConfiguration> consumer,
                                int newVersion) {

        public static @NotNull Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private final List<ConfigChanges> changes = new ArrayList<>();

            public Builder addChange(Predicate<FileConfiguration> predicate,
                                     Consumer<FileConfiguration> consumer,
                                     int newVersion) {
                changes.add(new ConfigChanges(predicate, consumer, newVersion));
                return this;
            }

            public List<ConfigChanges> build() {
                return ImmutableList.copyOf(changes);
            }
        }
    }

    @Override
    public void onDisable() {
        if (cigaretteManager == null) return;
        Set<Cigarette> cigarettes = cigaretteManager.getCigarettes();
        if (cigarettes != null) cigarettes.forEach(Cigarette::extinguish);
    }

    public @Nullable Cigarette getCigarette(Player player) {
        for (Cigarette cigarette : cigaretteManager.getCigarettes()) {
            if (cigarette.getOwner().equals(player)) return cigarette;
        }
        return null;
    }

    public @Nullable CigaretteType getTypeByItem(@NotNull ItemStack item) {
        boolean invalidateOld = getConfig().getBoolean("invalidate-old-cigarettes");

        ItemMeta meta = item.getItemMeta();
        if (meta == null && invalidateOld) return null;

        String typeName = meta != null ? meta.getPersistentDataContainer().get(identifier, PersistentDataType.STRING) : null;
        if (typeName == null) {
            if (invalidateOld) return null;

            for (CigaretteType type : cigaretteManager.getTypes()) {
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

        for (CigaretteType type : cigaretteManager.getTypes()) {
            if (type.getName().equals(typeName)) return type;
        }

        return null;
    }

    public void extinguishIfPossible(Player player) {
        Cigarette cigarette = getCigarette(player);
        if (cigarette == null) return;

        cigarette.extinguish();
        player.sendMessage(getString(MainCommand.MSG_EXTINGUISH));
    }

    public String getString(String path) {
        return PluginUtils.translate(getConfig().getString(path));
    }
}