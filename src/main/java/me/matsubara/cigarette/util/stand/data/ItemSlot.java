package me.matsubara.cigarette.util.stand.data;

import lombok.Getter;
import me.matsubara.cigarette.util.Reflection;
import me.matsubara.cigarette.util.stand.PacketStand;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;

@Getter
public enum ItemSlot {
    MAINHAND(EquipmentSlot.HAND, "main-hand"),
    OFFHAND(EquipmentSlot.OFF_HAND, "off-hand"),
    FEET(EquipmentSlot.FEET, "boots"),
    LEGS(EquipmentSlot.LEGS, "leggings"),
    CHEST(EquipmentSlot.CHEST, "chestplate"),
    HEAD(EquipmentSlot.HEAD, "helmet");

    private final EquipmentSlot slot;
    private final String path;
    private final Object nmsObject;

    ItemSlot(EquipmentSlot slot, String path) {
        this.slot = slot;
        this.path = path;
        this.nmsObject = initNMSObject();
    }

    private @Nullable Object initNMSObject() {
        try {
            MethodHandle field = Reflection.getField(
                    PacketStand.ENUM_ITEM_SLOT,
                    PacketStand.ENUM_ITEM_SLOT,
                    String.valueOf(PacketStand.ALPHABET[ordinal()]),
                    true,
                    name());

            if (field != null) {
                return field.invoke();
            }
        } catch (Throwable exception) {
            exception.printStackTrace();
        }
        return null;
    }
}