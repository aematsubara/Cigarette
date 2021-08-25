package me.matsubara.cigarette.cigarette;

import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.UUID;

@SuppressWarnings("unused")
public final class Cigarette {

    private final CigarettePlugin plugin;

    private final UUID owner;
    private final ItemStack item;
    private final CigaretteType type;
    private final ArmorStand stand;
    private final UUID standId;
    private final int taskId;

    public Cigarette(CigarettePlugin plugin, Player owner, ItemStack item, CigaretteType type) {
        // Copy location from player and add an offset.
        Location location = owner.getLocation().clone();
        location.add(PluginUtils.offsetVector(new Vector(0.0d, 0.0d, -0.35d), location.getYaw(), location.getPitch()));

        this.plugin = plugin;
        this.owner = owner.getUniqueId();
        this.item = item;
        this.type = type;
        this.stand = owner.getWorld().spawn(location.add(0.0d, 0.68d, 0.0d), ArmorStand.class, this::init);
        this.standId = stand.getUniqueId();
        this.taskId = startTask(owner);
        plugin.getCigarettes().add(this);
    }

    private int startTask(Player owner) {
        // Play lit sound.
        XSound.play(stand.getLocation(), plugin.getString("sounds.light"));
        XPotion.addPotionEffectsFromString(owner, type.getEffects());
        show(true);

        return new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count == type.getDuration()) {
                    owner.sendMessage(plugin.getString("messages.extinguish"));
                    extinguish();
                    cancel();
                }

                int delayInSeconds = 2;
                if (count % delayInSeconds == 0 && isVisible()) {
                    XSound.play(stand.getLocation(), plugin.getString("sounds.smoke"));

                    Location location = owner.getEyeLocation().clone();
                    location.add(PluginUtils.offsetVector(new Vector(0.3d, -0.1d, 0.0d), location.getYaw(), location.getPitch()));
                    if (type.getSmoke() != null) {
                        type.getSmoke().playAt(location);
                    }
                }

                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    public void cancelTask() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    public void extinguish() {
        XSound.play(stand.getLocation(), plugin.getString("sounds.extinguish"));
        stand.remove();
        plugin.getCigarettes().remove(this);

        // Remove previous effects.
        Player owner = getOwnerAsPlayer();
        if (owner != null) {
            for (PotionEffect effect : type.getPotionEffects()) {
                owner.removePotionEffect(effect.getType());
            }
        }

        cancelTask();
    }

    public boolean isVisible() {
        // If somehow the equipment is null, then the stand is invisible.
        if (stand.getEquipment() == null) return false;
        return stand.getEquipment().getItemInMainHand().getType() != Material.AIR;
    }

    public void show(boolean show) {
        if (stand.getEquipment() != null) {
            stand.getEquipment().setItemInMainHand(show ? type.getItem() : null);
        }
    }

    private void init(ArmorStand stand) {
        stand.setAI(false);
        stand.setArms(false);
        stand.setBasePlate(false);
        stand.setCollidable(false);
        stand.setGravity(false);

        // Prevent stand respawning on restart.
        stand.setPersistent(false);

        stand.setVisible(false);
        stand.setMarker(true);
        stand.setSilent(true);
        stand.setFireTicks(Integer.MAX_VALUE);
        stand.setRightArmPose(new EulerAngle(0.0d, 0.0d, 0.0d));
    }

    public UUID getOwner() {
        return owner;
    }

    public ItemStack getItem() {
        return item;
    }

    public Player getOwnerAsPlayer() {
        return Bukkit.getPlayer(owner);
    }

    public CigaretteType getType() {
        return type;
    }

    public ArmorStand getStand() {
        return stand;
    }

    public UUID getStandId() {
        return standId;
    }

    public int getTaskId() {
        return taskId;
    }
}