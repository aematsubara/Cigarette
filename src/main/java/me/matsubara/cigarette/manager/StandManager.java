package me.matsubara.cigarette.manager;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.util.stand.PacketStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class StandManager implements Listener, Runnable {

    private final CigarettePlugin plugin;

    private static final double BUKKIT_VIEW_DISTANCE = Math.pow(Bukkit.getViewDistance() << 4, 2);

    public StandManager(CigarettePlugin plugin) {
        this.plugin = plugin;
        Server server = this.plugin.getServer();
        server.getPluginManager().registerEvents(this, plugin);
        server.getScheduler().runTaskTimerAsynchronously(plugin, this, 20L, 20L);
    }

    @Override
    public void run() {
        // Here we will handle the visibility of the table to the players in the world.
        // This approach should be much better than doing it in PlayerMoveEvent.
        for (Cigarette cigarette : plugin.getCigaretteManager().getCigarettes()) {
            World world = cigarette.getStand().getWorld();
            if (world == null) continue;

            for (Player player : world.getPlayers()) {
                handleStandRender(cigarette, player, player.getLocation(), HandleCause.MOVE);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        for (Cigarette cigarette : plugin.getCigaretteManager().getCigarettes()) {
            cigarette.getOut().remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        handleStandRender(player, player.getLocation());
    }

    @EventHandler
    public void onPlayerChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        handleStandRender(player, player.getLocation());
    }

    private double getRenderDistance() {
        double distance = plugin.getConfig().getDouble("render-distance");
        return Math.min(distance * distance, BUKKIT_VIEW_DISTANCE);
    }

    private boolean isInRange(@NotNull Location location, @NotNull Location check) {
        return Objects.equals(location.getWorld(), check.getWorld())
                && location.distanceSquared(check) <= getRenderDistance();
    }

    private void handleStandRender(Player player, Location location) {
        for (Cigarette cigarette : plugin.getCigaretteManager().getCigarettes()) {
            handleStandRender(cigarette, player, location, HandleCause.SPAWN);
        }
    }

    public void handleStandRender(@NotNull Cigarette cigarette, @NotNull Player player, Location location, HandleCause cause) {
        Set<UUID> out = cigarette.getOut();
        UUID playerUUID = player.getUniqueId();

        PacketStand stand = cigarette.getStand();

        // The table is in another world, there is no need to send packets.
        if (!Objects.equals(player.getWorld(), stand.getWorld())) {
            out.add(playerUUID);
            return;
        }

        boolean range = isInRange(stand.getLocation(), location);
        boolean ignored = out.contains(playerUUID);
        boolean spawn = cause == HandleCause.SPAWN;

        boolean show = range && (ignored || spawn);
        boolean destroy = !range && !ignored;
        if (!show && !destroy) return;

        if (show) {
            out.remove(playerUUID);
        } else {
            out.add(playerUUID);
            if (spawn) return;
        }

        // Show/hide cigarette stands.
        if (show) {
            stand.spawn(player);
        } else {
            stand.destroy(player);
        }
    }

    public enum HandleCause {
        SPAWN,
        MOVE
    }
}