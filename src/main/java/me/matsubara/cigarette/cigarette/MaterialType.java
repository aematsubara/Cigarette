package me.matsubara.cigarette.cigarette;

import lombok.Getter;
import me.matsubara.cigarette.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public enum MaterialType {
    NORMAL_CIGARETTE(0.15d, -0.35d, 0.8d, -0.1d, 0.68d, 0.3d, 0.0d, 0.0d, 0.0d, false),
    NORMAL_PIPE(0.1d, -0.25d, 0.85d, -0.1d, -0.4d, -0.73d, 355.0d, 0.0d, 180.0d, false),
    SMALL_PIPE(0.25d, -0.125d, 0.575d, -0.1d, 0.55d, 0.175d, 355.0d, 0.0d, 180.0d, true);

    /**
     * The stand offset from the player location. For the height use {@link #normalHeight} or {@link #sneakingHeight}.
     */
    private final @Getter Vector offset;

    /**
     * Effects offset from the eye location. For the height, use {@link #effectsHeight}.
     */
    private final Vector effectsOffset;

    /**
     * Effects height from the eye location. For the offset, use {@link #effectsOffset}.
     */
    private final Vector effectsHeight;

    /**
     * The height used when standing.
     */
    private final Vector normalHeight;

    /**
     * The height used when sneaking.
     */
    private final Vector sneakingHeight;

    /**
     * The angle of the right hand.
     */
    private final @Getter EulerAngle angle;

    /**
     * Whether the stand is small.
     */
    private final @Getter boolean isSmall;

    private static final Material[] PIPE_TYPES = {
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.GOLDEN_HOE,
            Material.IRON_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE};

    MaterialType(double frontOffset,
                 double sideOffset,
                 double effectsOffset,
                 double effectsHeight,
                 double normalHeight,
                 double sneakingHeight,
                 double angleXDegrees,
                 double angleYDegrees,
                 double angleZDegrees,
                 boolean isSmall) {
        this.offset = new Vector(frontOffset, 0.0d, sideOffset);
        this.effectsOffset = new Vector(effectsOffset, 0.0d, 0.0d);
        this.effectsHeight = new Vector(0.0d, effectsHeight, 0.0d);
        this.normalHeight = new Vector(0.0d, normalHeight, 0.0d);
        this.sneakingHeight = new Vector(0.0d, sneakingHeight, 0.0d);
        this.isSmall = isSmall;
        this.angle = new EulerAngle(Math.toRadians(angleXDegrees), Math.toRadians(angleYDegrees), Math.toRadians(angleZDegrees));
    }

    public @NotNull Location getEffectsLocationFromPlayer(@NotNull Player player) {
        Location location = player.getEyeLocation().clone();
        return location
                .add(PluginUtils.offsetVector(effectsOffset, location.getYaw(), location.getPitch()))
                .add(effectsHeight);
    }


    public Vector getHeight(boolean isSneaking) {
        return isSneaking ? sneakingHeight : normalHeight;
    }

    public static MaterialType getByMaterial(Material material, boolean isSmall) {
        for (MaterialType type : values()) {
            if (!type.name().endsWith("PIPE")) continue;
            for (Material pipeType : PIPE_TYPES) {
                if (pipeType != material || (type.isSmall() != isSmall)) continue;
                return type;
            }
        }
        return NORMAL_CIGARETTE;
    }
}