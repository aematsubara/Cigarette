package me.matsubara.cigarette.util;

import com.cryptomorin.xseries.ReflectionUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginUtils {

    private final static Pattern PATTERN = Pattern.compile("(&)?&#([0-9a-fA-F]{6})");

    public static Vector offsetVector(Vector vector, float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(-1.0d * (yawDegrees + 90.0f));
        double pitch = Math.toRadians(-pitchDegrees);

        double cosYaw = Math.cos(yaw);
        double cosPitch = Math.cos(pitch);

        double sinYaw = Math.sin(yaw);
        double sinPitch = Math.sin(pitch);

        double initialX, initialY, initialZ;
        double x, y, z;

        initialX = vector.getX();
        initialY = vector.getY();
        x = initialX * cosPitch - initialY * sinPitch;
        y = initialX * sinPitch + initialY * cosPitch;

        initialZ = vector.getZ();
        initialX = x;
        z = initialZ * cosYaw - initialX * sinYaw;
        x = initialZ * sinYaw + initialX * cosYaw;

        return new Vector(x, y, z);
    }

    public static String translate(String message) {
        if (ReflectionUtils.VER < 16) return oldTranslate(message);

        Matcher matcher = PATTERN.matcher(oldTranslate(message));
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
        }

        return matcher.appendTail(buffer).toString();
    }

    public static List<String> translate(List<String> messages) {
        messages.replaceAll(PluginUtils::translate);
        return messages;
    }

    private static String oldTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}