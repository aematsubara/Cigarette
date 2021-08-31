package me.matsubara.cigarette;

import com.cryptomorin.xseries.ReflectionUtils;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.file.CigaretteTypes;
import me.matsubara.cigarette.listener.PlayerInteract;
import me.matsubara.cigarette.listener.PlayerItemConsume;
import me.matsubara.cigarette.listener.PlayerMove;
import me.matsubara.cigarette.listener.PlayerQuit;
import me.matsubara.cigarette.util.PluginUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public final class CigarettePlugin extends JavaPlugin {

    private Set<Cigarette> cigarettes;

    private CigaretteTypes types;

    @Override
    public void onEnable() {
        if (ReflectionUtils.VER < 15) {
            getLogger().severe("This plugin only works from 1.15.2 and up, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        cigarettes = new HashSet<>();

        types = new CigaretteTypes(this);

        getServer().getPluginManager().registerEvents(new PlayerMove(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteract(this), this);
        getServer().getPluginManager().registerEvents(new PlayerItemConsume(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(this), this);

        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Extinguish cigarettes.
        cigarettes.forEach(Cigarette::extinguish);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cigarette")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (!player.hasPermission("cigarette.admin")) {
                    player.sendMessage(getString("messages.no-permission"));
                    return true;
                }

                if (args.length == 0 || args.length > 2) {
                    player.sendMessage(getString("messages.usage"));
                } else if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        // Extinguish cigarettes.
                        cigarettes.forEach(Cigarette::extinguish);

                        reloadConfig();
                        types.reloadConfig();
                        player.sendMessage(getString("messages.reload"));
                    } else {
                        player.sendMessage(getString("messages.usage"));
                    }
                } else {
                    if (args[0].equalsIgnoreCase("get")) {
                        CigaretteType type = types.getTypeByName(args[1]);
                        if (type == null) {
                            player.sendMessage(getString("messages.unknown"));
                            return true;
                        }

                        player.getInventory().addItem(type.getItem());
                    } else {
                        player.sendMessage(getString("messages.usage"));
                    }
                }
            }
        }
        return true;
    }

    public String getString(String path) {
        return PluginUtils.translate(getConfig().getString(path));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("cigarette")) {
            if (!sender.hasPermission("cigarette.admin")) return null;

            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], Arrays.asList("get", "reload"), new ArrayList<>());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
                return StringUtil.copyPartialMatches(args[1], getCiggiesNames(), new ArrayList<>());
            }
        }
        return null;
    }

    public List<String> getCiggiesNames() {
        return types.getTypes().stream().map(CigaretteType::getName).collect(Collectors.toList());
    }

    public boolean isSmoking(Player player) {
        for (Cigarette cigarette : cigarettes) {
            if (cigarette.getOwner().equals(player.getUniqueId())) return true;
        }
        return false;
    }

    public Cigarette getCigarette(Player player) {
        for (Cigarette ciggy : cigarettes) {
            if (ciggy.getOwner().equals(player.getUniqueId())) return ciggy;
        }
        return null;
    }

    public Set<Cigarette> getCigarettes() {
        return cigarettes;
    }

    public CigaretteTypes getCigaretteTypes() {
        return types;
    }
}