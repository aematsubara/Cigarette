package me.matsubara.cigarette.cigarette;

import com.cryptomorin.xseries.XSound;
import lombok.Getter;
import me.matsubara.cigarette.CigarettePlugin;
import me.matsubara.cigarette.command.MainCommand;
import me.matsubara.cigarette.manager.StandManager;
import me.matsubara.cigarette.util.PluginUtils;
import me.matsubara.cigarette.util.stand.PacketStand;
import me.matsubara.cigarette.util.stand.StandSettings;
import me.matsubara.cigarette.util.stand.data.ItemSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
public final class Cigarette extends BukkitRunnable {

    private final CigarettePlugin plugin;

    private final Player owner;
    private final CigaretteType type;
    private final PacketStand stand;
    private final int taskId;
    private final Set<UUID> out = new HashSet<>();
    private int count = 0;

    public static final ItemStack EMPTY_ITEM = new ItemStack(Material.AIR);
    private static final Map<MaterialType, StandSettings> SETTINGS_CACHE = new HashMap<>();

    public Cigarette(@NotNull CigarettePlugin plugin, @NotNull Player owner, @NotNull CigaretteType type) {
        this.plugin = plugin;
        this.owner = owner;
        this.type = type;

        StandSettings settings = SETTINGS_CACHE.computeIfAbsent(type.getMaterialType(), this::createSetttings).clone();
        this.stand = new PacketStand(plugin, createLocation(), settings, true);

        // Play lit sound.
        playSound(stand.getLocation(), type.getLightSound());
        if (!type.getEffects().isEmpty()) owner.addPotionEffects(type.getEffects());
        show(true);

        this.taskId = runTaskTimer(plugin, 1L, 1L).getTaskId();

        StandManager manager = plugin.getStandManager();
        for (Player player : owner.getWorld().getPlayers()) {
            manager.handleStandRender(this, player, player.getLocation(), StandManager.HandleCause.SPAWN);
        }
    }

    @Override
    public void run() {
        // Extinguish if time is over.
        if (count / 20 == type.getDuration()) {
            owner.sendMessage(plugin.getString(MainCommand.MSG_EXTINGUISH));
            extinguish();
            cancel();
            return;
        }

        // Spawn particles & play ambient sound.
        if (count % 40 == 0 && isVisible()) {
            Location effectsLocation = type.getMaterialType().getEffectsLocationFromPlayer(owner);
            playSound(effectsLocation, type.getSmokeSound());
            if (type.getSmoke() != null) type.getSmoke().playAt(effectsLocation);
        }

        if (type.isSecondHandSmoke() && count % 5 == 0) {
            for (Entity near : owner.getNearbyEntities(2.5, 2.5d, 2.5d)) {
                if (!(near instanceof Player player)
                        || player.equals(owner)
                        || player.hasPermission("cigarette.bypass.secondhandsmoke")) continue;

                // Half of resting time.
                int time = Math.max(1, (type.getDuration() - count / 20) / 2);
                for (PotionEffect effect : type.getEffects()) {
                    PotionEffectType type = effect.getType();
                    if (player.hasPotionEffect(type)) continue;

                    player.addPotionEffect(new PotionEffect(
                            type,
                            time * 20,
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.hasParticles(),
                            effect.hasIcon()));
                }
            }
        }

        float pitch = owner.getLocation().getPitch();
        boolean positionChanged = !stand.invalidTeleport(createLocation()),
                shouldShow = (pitch > -14.0f) && (pitch < 20.0f),
                visibilityChanged = show(shouldShow);

        if (positionChanged || visibilityChanged) {
            List<Player> players = owner.getWorld().getPlayers();
            players.removeIf(player -> out.contains(player.getUniqueId()));

            if (!players.isEmpty()) {
                stand.sendLocation(players);
                stand.sendEquipment(players);
            }
        }

        count++;
    }

    public void extinguish() {
        playSound(stand.getLocation(), type.getExtinguishSound());
        stand.destroy();
        plugin.getCigaretteManager().getCigarettes().remove(this);

        if (taskId != -1) plugin.getServer().getScheduler().cancelTask(taskId);

        if (owner == null || !owner.isOnline()) return;
        for (PotionEffect effect : type.getEffects()) {
            owner.removePotionEffect(effect.getType());
        }
    }

    private void playSound(@NotNull Location location, XSound.@NotNull Record record) {
        record.soundPlayer()
                .atLocation(location)
                .play();
    }

    public boolean isVisible() {
        ItemStack item = stand.getSettings().getEquipment().get(ItemSlot.MAINHAND);
        return item != null && item.getType() == type.getItem().getType();
    }

    public boolean show(boolean show) {
        if (show == isVisible()) return false;
        stand.getSettings().getEquipment().put(ItemSlot.MAINHAND, show ? type.getItem() : EMPTY_ITEM);
        return true;
    }

    private @NotNull Location createLocation() {
        MaterialType type = this.type.getMaterialType();

        // Copy location from the player and add an offset.
        Location location = owner.getLocation().clone();

        float pitch = location.getPitch();

        location.add(PluginUtils.offsetVector(type.getOffset(), location.getYaw(), pitch));

        Vector height = type.getHeight(owner.isSneaking());
        return location.add(height);
    }

    private @NotNull StandSettings createSetttings(@NotNull MaterialType material) {
        return new StandSettings()
                .setArms(false)
                .setBasePlate(false)
                .setInvisible(true)
                .setRightArmPose(material.getAngle())
                .setSmall(material.isSmall());
    }
}