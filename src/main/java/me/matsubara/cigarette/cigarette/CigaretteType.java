package me.matsubara.cigarette.cigarette;

import com.cryptomorin.xseries.XPotion;
import me.matsubara.cigarette.data.Shape;
import me.matsubara.cigarette.data.Smoke;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class CigaretteType {

    private final String name;
    private final int duration;
    private final ItemStack item;
    private final List<String> effects;
    private final Shape shape;
    private final Smoke smoke;

    private List<PotionEffect> cache;

    public CigaretteType(String name, int duration, ItemStack item, List<String> effects, Shape shape, Smoke smoke) {
        this.name = name;
        this.duration = duration;
        this.item = item;
        this.effects = effects;
        this.shape = shape;
        this.smoke = smoke;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public ItemStack getItem() {
        return item;
    }

    public List<String> getEffects() {
        return effects;
    }

    public List<PotionEffect> getPotionEffects() {
        if (cache != null) return cache;

        List<PotionEffect> temp = new ArrayList<>();
        for (String effect : effects) {
            PotionEffect potionEffect = XPotion.parsePotionEffectFromString(effect);
            if (potionEffect == null) continue;

            temp.add(potionEffect);
        }
        return cache = temp;
    }

    public Shape getShape() {
        return shape;
    }

    public Smoke getSmoke() {
        return smoke;
    }
}