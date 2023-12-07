package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerDeathOrQuit implements Listener {

    private final CigarettePlugin plugin;

    public PlayerDeathOrQuit(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        plugin.extinguishIfPossible(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        plugin.extinguishIfPossible(event.getPlayer());
    }
}