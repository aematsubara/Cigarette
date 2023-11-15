package me.matsubara.cigarette;

import com.google.common.collect.ImmutableList;
import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.file.CigaretteTypes;
import me.matsubara.cigarette.listener.InventoryClick;
import me.matsubara.cigarette.listener.PlayerDeathOrQuit;
import me.matsubara.cigarette.listener.PlayerInteract;
import me.matsubara.cigarette.listener.PlayerItemConsume;
import me.matsubara.cigarette.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems")
@Getter
public final class CigarettePlugin extends JavaPlugin {

    private Set<Cigarette> cigarettes;
    private CigaretteTypes types;

    private static final List<String> SPECIAL_SECTIONS = Collections.emptyList();

    public static final String MSG_NOT_FROM_CONSOLE = "messages.not-from-console";
    public static final String MSG_NO_PERMISSION = "messages.no-permission";
    public static final String MSG_PLAYER_NOT_FOUND = "messages.player-not-found";
    public static final String MSG_REQUIRES_LIGHTNING = "messages.requires-lightning";
    public static final String MSG_USAGE = "messages.usage";
    public static final String MSG_RELOADING = "messages.reloading";
    public static final String MSG_RELOAD = "messages.reload";
    public static final String MSG_UNKNOWN = "messages.unknown";
    public static final String MSG_EXTINGUISH = "messages.extinguish";
    public static final String MSG_LIGHT = "messages.light";

    @Override
    public void onEnable() {
        if (PluginUtils.MINOR_VERSION < 15) {
            getLogger().severe("This plugin only works from 1.15.2 and up, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        cigarettes = new HashSet<>();

        types = new CigaretteTypes(this);

        getServer().getPluginManager().registerEvents(new InventoryClick(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteract(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathOrQuit(this), this);
        getServer().getPluginManager().registerEvents(new PlayerItemConsume(this), this);

        saveDefaultConfig();
        updateMainConfig();
    }

    private void updateMainConfig() {
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
        // Extinguish cigarettes.
        if (cigarettes != null) cigarettes.forEach(Cigarette::extinguish);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("cigarette")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(getString(MSG_NOT_FROM_CONSOLE));
            return true;
        }

        if (!player.hasPermission("cigarette.admin")) {
            player.sendMessage(getString(MSG_NO_PERMISSION));
            return true;
        }

        if (args.length == 0 || args.length > 3) {
            player.sendMessage(getString(MSG_USAGE));
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                player.sendMessage(getString(MSG_RELOADING));
                CompletableFuture.runAsync(this::updateMainConfig).thenRun(() -> getServer().getScheduler().runTask(this, () -> {
                    // Extinguish cigarettes.
                    cigarettes.forEach(Cigarette::extinguish);

                    // Reload cigarettes from config.
                    types.reloadConfig();

                    player.sendMessage(getString(MSG_RELOAD));
                }));
            } else {
                player.sendMessage(getString(MSG_USAGE));
            }
        } else {
            if (args[0].equalsIgnoreCase("give")) {
                CigaretteType type = types.getTypeByName(args[1]);
                if (type == null) {
                    player.sendMessage(getString(MSG_UNKNOWN));
                    return true;
                }

                Player target = args.length == 3 ? Bukkit.getPlayer(args[2]) : player;
                if (target != null && target.isOnline()) {
                    target.getInventory().addItem(type.getItem());
                } else {
                    player.sendMessage(getString(MSG_PLAYER_NOT_FOUND));
                }
            } else {
                player.sendMessage(getString(MSG_USAGE));
            }
        }
        return true;
    }

    public String getString(String path) {
        return PluginUtils.translate(getConfig().getString(path));
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, @NotNull Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("cigarette")) return null;
        if (!sender.hasPermission("cigarette.admin")) return null;

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "reload"), new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return StringUtil.copyPartialMatches(args[1], getCiggiesNames(), new ArrayList<>());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && getCiggiesNames().contains(args[1].toLowerCase())) {
            return StringUtil.copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), new ArrayList<>());
        }

        return null;
    }

    public List<String> getCiggiesNames() {
        return types.getTypes().stream().map(CigaretteType::getName).collect(Collectors.toList());
    }

    public boolean isSmoking(Player player) {
        for (Cigarette cigarette : cigarettes) {
            if (cigarette.getOwner().getUniqueId().equals(player.getUniqueId())) return true;
        }
        return false;
    }

    public @Nullable Cigarette getCigarette(Player player) {
        for (Cigarette ciggy : cigarettes) {
            if (ciggy.getOwner().getUniqueId().equals(player.getUniqueId())) return ciggy;
        }
        return null;
    }

    public @Nullable CigaretteType getTypeByItem(ItemStack item) {
        for (CigaretteType type : types.getTypes()) {
            if (item.isSimilar(type.getItem())) {
                return type;
            }
        }
        return null;
    }

    public void extinguishIfNecessary(Player player) {
        if (!isSmoking(player)) return;

        Cigarette ciggy = getCigarette(player);
        if (ciggy == null) return;

        ciggy.extinguish();
        player.sendMessage(getString(CigarettePlugin.MSG_EXTINGUISH));
    }
}