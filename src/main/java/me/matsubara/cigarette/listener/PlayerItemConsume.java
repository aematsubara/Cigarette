package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class PlayerItemConsume implements Listener {

    private final CigarettePlugin plugin;

    public PlayerItemConsume(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.MILK_BUCKET) return;

        if (!plugin.getConfig().getBoolean("remove-when-drinking-milk")) return;

        Player player = event.getPlayer();

        if (plugin.isSmoking(player)) {
            Cigarette ciggy = plugin.getCigarette(player);
            if (ciggy == null) return;

            ciggy.extinguish();

            player.sendMessage(plugin.getString("messages.extinguish"));
        }
    }
}