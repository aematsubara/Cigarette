package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerDeathOrQuit implements Listener {

    private final CigarettePlugin plugin;

    public PlayerDeathOrQuit(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.extinguishIfNecessary(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.extinguishIfNecessary(event.getPlayer());
    }
}