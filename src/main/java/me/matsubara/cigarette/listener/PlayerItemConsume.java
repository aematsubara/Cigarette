package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerItemConsume implements Listener {

    private final CigarettePlugin plugin;

    public PlayerItemConsume(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.MILK_BUCKET) return;

        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("remove-when-drinking-milk")) {
            plugin.getCigaretteManager().extinguishIfPossible(player);
        }
    }
}