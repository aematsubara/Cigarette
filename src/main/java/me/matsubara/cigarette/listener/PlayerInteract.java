package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
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

        CigaretteType cigaretteType = null;

        for (CigaretteType type : plugin.getCigaretteTypes().getTypes()) {
            if (item.isSimilar(type.getItem())) {
                cigaretteType = type;
                break;
            }
        }

        if (cigaretteType == null) return;

        if (plugin.isSmoking(player)) {
            Cigarette ciggy = plugin.getCigarette(player);
            if (ciggy == null) return;

            ciggy.extinguish();

            player.sendMessage(plugin.getString("messages.extinguish"));
        }

        new Cigarette(plugin, player, item, cigaretteType);
        player.sendMessage(plugin.getString("messages.light"));

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        event.setCancelled(true);
    }
}