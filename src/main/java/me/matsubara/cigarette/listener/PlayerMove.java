package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public final class PlayerMove implements Listener {

    private final CigarettePlugin plugin;

    public PlayerMove(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Cigarette cigarette = plugin.getCigarette(player);
        if (cigarette == null) return;

        Location location = player.getLocation().clone();
        location.add(PluginUtils.offsetVector(new Vector(0.0d, 0.0d, -0.35d), location.getYaw(), location.getPitch()));

        cigarette.getStand().teleport(location.add(0.0d, player.isSneaking() ? 0.3d : 0.68d, 0.0d));

        float pitch = player.getLocation().getPitch();
        cigarette.show((pitch > -14.0f) && (pitch < 20.0f));
    }
}