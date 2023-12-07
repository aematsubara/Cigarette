package me.matsubara.cigarette.command;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.util.PluginUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final CigarettePlugin plugin;

    public static final String MSG_NO_PERMISSION = "messages.no-permission";
    public static final String MSG_PLAYER_NOT_FOUND = "messages.player-not-found";
    public static final String MSG_REQUIRES_LIGHTNING = "messages.requires-lightning";
    public static final String MSG_RELOADING = "messages.reloading";
    public static final String MSG_RELOAD = "messages.reload";
    public static final String MSG_UNKNOWN = "messages.unknown";
    public static final String MSG_EXTINGUISH = "messages.extinguish";
    public static final String MSG_LIGHT = "messages.light";
    public static final String MSG_INVALID_AMOUNT = "messages.invalid-amount";

    private static final List<String> ARGS = List.of("give", "reload");
    private static final List<String> AMOUNT = List.of("[amount]");
    private static final List<String> VALID_RANGE = IntStream.rangeClosed(1, 64).mapToObj(String::valueOf).toList();
    private static final List<String> HELP = Stream.of(
            "&8----------------------------------------",
            "&6&lCigarette &f&oCommands &c<required> | [optional]",
            "&e/cigarette reload &f- &7Reload configuration files.",
            "&e/cigarette give <name> [amount] [player] &f- &7Gives you (or any player) a cigarette by name and amount.",
            "&8----------------------------------------").map(PluginUtils::translate).toList();

    public MainCommand(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("cigarette.admin")) {
            sender.sendMessage(plugin.getString(MSG_NO_PERMISSION));
            return true;
        }

        if (args.length == 0 || args.length > 4) {
            HELP.forEach(sender::sendMessage);
            return true;
        }

        if (args.length == 1) {
            if (!args[0].equalsIgnoreCase("reload")) {
                HELP.forEach(sender::sendMessage);
                return true;
            }

            sender.sendMessage(plugin.getString(MSG_RELOADING));
            CompletableFuture.runAsync(plugin::updateMainConfig).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Extinguish cigarettes.
                plugin.getCigaretteManager().getCigarettes().forEach(Cigarette::extinguish);

                // Reload cigarettes from config.
                plugin.getCigaretteManager().reloadConfig();

                sender.sendMessage(plugin.getString(MSG_RELOAD));
            }));
            return true;
        }

        if (!args[0].equalsIgnoreCase("give")) {
            HELP.forEach(sender::sendMessage);
            return true;
        }

        CigaretteType type = plugin.getCigaretteManager().getTypeByName(args[1]);
        if (type == null) {
            sender.sendMessage(plugin.getString(MSG_UNKNOWN));
            return true;
        }

        int amount = args.length >= 3 && NumberUtils.isCreatable(args[2]) ? Integer.parseInt(args[2]) : 1;
        if (amount < 1 || amount > 64) {
            sender.sendMessage(plugin.getString(MSG_INVALID_AMOUNT));
            return true;
        }

        ItemStack item = type.getItem().clone();
        item.setAmount(amount);

        Player target = args.length == 4 ? Bukkit.getPlayer(args[3]) : sender instanceof Player player ? player : null;
        if (target != null && target.isOnline()) {
            target.getInventory().addItem(item);
        } else {
            sender.sendMessage(plugin.getString(MSG_PLAYER_NOT_FOUND));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("cigarette.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], ARGS, new ArrayList<>());
        }

        boolean isGive = args[0].equalsIgnoreCase("give");

        if (args.length == 2 && isGive) {
            return StringUtil.copyPartialMatches(args[1], getCiggiesNames(), new ArrayList<>());
        }

        boolean validCigarette = getCiggiesNames().contains(args[1].toLowerCase());

        if (args.length == 3 && isGive && validCigarette) {
            return StringUtil.copyPartialMatches(args[2], AMOUNT, new ArrayList<>());
        }

        if (args.length == 4 && isGive && validCigarette && VALID_RANGE.contains(args[2])) {
            return null;
        }

        return Collections.emptyList();
    }

    public List<String> getCiggiesNames() {
        return plugin.getCigaretteManager().getTypes().stream().map(CigaretteType::getName).collect(Collectors.toList());
    }
}