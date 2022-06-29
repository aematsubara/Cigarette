package me.matsubara.cigarette.data;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class Smoke {

    private final Particle particle;

    private final int amount;
    private final double randomX, randomY, randomZ, speed;

    public Smoke(Particle particle, int amount, double randomX, double randomY, double randomZ, double speed) {
        this.particle = particle;
        this.amount = amount;
        this.randomX = randomX;
        this.randomY = randomY;
        this.randomZ = randomZ;
        this.speed = speed;
    }

    public void playAt(Location location) {
        if (particle == null) return;
        Preconditions.checkArgument(location.getWorld() != null, "World can't be null.");

        location.getWorld().spawnParticle(particle, location, amount, randomX, randomY, randomZ, speed);
    }
}