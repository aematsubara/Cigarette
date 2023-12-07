package me.matsubara.cigarette.listener;

import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.cigarette.Cigarette;
import me.matsubara.cigarette.cigarette.CigaretteType;
import me.matsubara.cigarette.command.MainCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

public final class InventoryClick implements Listener {

    private final CigarettePlugin plugin;

    public InventoryClick(CigarettePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        ItemStack cursor = event.getCursor();
        if (cursor == null) return;
        if (cursor.getType() != Material.FLINT_AND_STEEL && cursor.getType() != Material.FIRE_CHARGE) return;

        ItemStack current = event.getCurrentItem();
        if (current == null) return;

        CigaretteType type = plugin.getTypeByItem(current);
        if (type == null) return;

        event.setCancelled(true);

        ItemStack newCurrent;
        if (current.getAmount() > 1) {
            newCurrent = current.clone();
            newCurrent.setAmount(newCurrent.getAmount() - 1);
            event.setCurrentItem(newCurrent);
        } else {
            event.setCurrentItem(null);
        }

        if (cursor.getType() == Material.FLINT_AND_STEEL) {
            Damageable damageable = (Damageable) cursor.getItemMeta();
            if (damageable != null) damageable.setDamage(damageable.getDamage() + 1);
            cursor.setItemMeta(damageable);
        } else {
            ItemStack newCursor;
            if (cursor.getAmount() > 1) {
                newCursor = cursor.clone();
                newCursor.setAmount(newCursor.getAmount() - 1);
                event.setCursor(newCursor);
            } else {
                event.setCursor(null);
            }
        }

        plugin.extinguishIfPossible(player);

        new Cigarette(plugin, player, type);
        player.sendMessage(plugin.getString(MainCommand.MSG_LIGHT));
    }
}