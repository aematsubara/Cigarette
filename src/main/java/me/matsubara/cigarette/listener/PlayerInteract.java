package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.command.MainCommand;
import me.matsubara.cigarette.manager.CigaretteManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class PlayerInteract implements Listener {

    private final CigarettePlugin plugin;

    public PlayerInteract(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getItemMeta() == null) return;

        // Only with main-hand, to prevent dupes.
        if (event.getHand() != EquipmentSlot.HAND) return;

        CigaretteManager manager = plugin.getCigaretteManager();

        CigaretteType type = manager.getTypeByItem(item);
        if (type == null) return;

        event.setCancelled(true);

        if (type.isRequiresLightning() && !player.hasPermission("cigarette.bypass.requireslightning")) {
            player.sendMessage(plugin.getString(MainCommand.MSG_REQUIRES_LIGHTNING));
            return;
        }

        manager.create(player, type);
        item.setAmount(item.getAmount() - 1);
        event.setCancelled(true);
    }
}