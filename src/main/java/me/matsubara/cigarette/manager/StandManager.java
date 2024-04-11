package me.matsubara.cigarette.manager;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.util.stand.PacketStand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class StandManager implements Listener {

    private final CigarettePlugin plugin;

    public StandManager(CigarettePlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        handleStandRender(player, player.getLocation(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;

        Location from = event.getFrom();
        if (to.getBlockX() == from.getBlockX()
                && to.getBlockY() == from.getBlockY()
                && to.getBlockZ() == from.getBlockZ()) return;

        Player player = event.getPlayer();
        handleStandRender(player, to, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        handleStandRender(player, player.getLocation(), true);
    }

    public void handleStandRender(@NotNull Player player, Location location, boolean isSpawn) {
        UUID playerUUID = player.getUniqueId();

        for (Cigarette cigarette : plugin.getCigaretteManager().getCigarettes()) {
            if (playerUUID.equals(cigarette.getOwner().getUniqueId())) continue;

            PacketStand stand = cigarette.getStand();
            boolean shouldShow = stand.isInRange(location);

            // Show/hide stands.
            handleStandRender(player, stand, shouldShow, isSpawn);
        }
    }

    private void handleStandRender(Player player, PacketStand stand, boolean shouldShow, boolean isSpawn) {
        if (shouldShow) {
            if (stand.isIgnored(player) || isSpawn) stand.spawn(player);
        } else {
            if (!stand.isIgnored(player)) stand.destroy(player);
        }
    }
}