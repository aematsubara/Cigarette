package me.matsubara.cigarette.cigarette;

import me.matsubara.cigarette.data.Shape;
import me.matsubara.cigarette.data.Smoke;
import me.matsubara.cigarette.util.Lang3Utils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
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
            PotionEffect potionEffect = parsePotionEffectFromString(effect);
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

    private PotionEffect parsePotionEffectFromString(@Nullable String potion) {
        if (potion == null || potion.isEmpty() || potion.equalsIgnoreCase("none")) return null;
        String[] split = Lang3Utils.split(Lang3Utils.deleteWhitespace(potion), ',');
        if (split.length == 0) split = Lang3Utils.split(potion, ' ');

        PotionEffectType type = PotionEffectType.getByName(split[0]);
        if (type == null) return null;

        int duration = 2400; // 20 ticks * 60 seconds * 2 minutes.
        int amplifier = 0;
        if (split.length > 1) {
            duration = Integer.parseInt(split[1]) * 20;
            if (split.length > 2) amplifier = Integer.parseInt(split[2]) - 1;
        }

        return new PotionEffect(type, duration, amplifier);
    }
}