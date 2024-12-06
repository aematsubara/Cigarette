package me.matsubara.cigarette.cigarette;

import com.cryptomorin.xseries.XSound;
import lombok.Getter;
import me.matsubara.cigarette.data.Shape;
import me.matsubara.cigarette.data.Smoke;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Getter
public final class CigaretteType {

    private final String name;
    private final MaterialType materialType;
    private final int duration;
    private final ItemStack item;
    private final List<String> effects;
    private final Shape shape;
    private final Smoke smoke;
    private final XSound.Record lightSound;
    private final XSound.Record extinguishSound;
    private final XSound.Record smokeSound;
    private final boolean secondHandSmoke;
    private final boolean requiresLightning;

    private List<PotionEffect> cache;

    public CigaretteType(
            String name,
            MaterialType materialType,
            int duration,
            ItemStack item,
            List<String> effects,
            Shape shape,
            Smoke smoke,
            String lightSound,
            String extinguishSound,
            String smokeSound,
            boolean secondHandSmoke,
            boolean requiresLightning) {
        this.name = name;
        this.materialType = materialType;
        this.duration = duration;
        this.item = item;
        this.effects = effects;
        this.shape = shape;
        this.smoke = smoke;
        this.lightSound = XSound.parse(lightSound);
        this.extinguishSound = XSound.parse(extinguishSound);
        this.smokeSound = XSound.parse(smokeSound);
        this.secondHandSmoke = secondHandSmoke;
        this.requiresLightning = requiresLightning;
    }

    public List<PotionEffect> getEffects() {
        if (cache != null) return cache;

        List<PotionEffect> temp = new ArrayList<>();
        for (String effect : effects) {
            PotionEffect potionEffect = parsePotionEffectFromString(effect);
            if (potionEffect == null) continue;

            temp.add(potionEffect);
        }
        return cache = temp;
    }

    private PotionEffect parsePotionEffectFromString(@Nullable String potion) {
        if (potion == null || potion.isEmpty() || potion.equalsIgnoreCase("none")) return null;
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(potion), ',');
        if (split.length == 0) split = StringUtils.split(potion, ' ');

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