package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class PlayerInteract implements Listener {

    private final CigarettePlugin plugin;

    public PlayerInteract(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getItemMeta() == null) return;

        // Only with main-hand, to prevent dupes.
        if (event.getHand() != EquipmentSlot.HAND) {
            event.setCancelled(true);
            return;
        }

        CigaretteType cigaretteType = plugin.getTypeByItem(item);
        if (cigaretteType == null) return;

        if (cigaretteType.isRequiresLightning() && !player.hasPermission("cigarette.bypass.requireslightning")) {
            player.sendMessage(plugin.getString(CigarettePlugin.MSG_REQUIRES_LIGHTNING));
            return;
        }

        plugin.extinguishIfNecessary(player);

        new Cigarette(plugin, player, cigaretteType);
        player.sendMessage(plugin.getString(CigarettePlugin.MSG_LIGHT));

        ItemStack toRemove = item.clone();
        toRemove.setAmount(1);

        player.getInventory().removeItem(toRemove);

        event.setCancelled(true);
    }
}