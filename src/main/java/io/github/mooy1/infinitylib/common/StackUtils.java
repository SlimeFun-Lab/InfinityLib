package io.github.mooy1.infinitylib.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import net.kyori.adventure.text.Component;

import java.awt.*;
import java.util.Objects;

@UtilityClass
@ParametersAreNonnullByDefault
public final class StackUtils {

    private static final NamespacedKey ID_KEY = Slimefun.getItemDataService().getKey();

    @Nullable
    public static String getId(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        SlimefunItem sf = SlimefunItem.getByItem(item);
        return (sf != null) ? sf.getId() : null;
    }

    @Nullable
    public static String getId(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(ID_KEY, PersistentDataType.STRING);
    }

    @Nonnull
    public static String getIdOrType(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return "AIR";

        SlimefunItem sf = SlimefunItem.getByItem(item);
        return (sf != null) ? sf.getId() : item.getType().name();
    }

    @Nullable
    public static ItemStack itemById(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        return item == null ? null : item.getItem().clone();
    }

    @Nonnull
    public static ItemStack itemByIdOrType(String idOrType) {
        SlimefunItem item = SlimefunItem.getById(idOrType);
        return item == null ? new ItemStack(Material.valueOf(idOrType)) : item.getItem().clone();
    }

    /**
     * Returns true when both items:
     *  - Are null or air
     *  - Have the same slimefun id
     *  - Have the same type and display name or lack thereof
     */
    public static boolean isSimilar(@Nullable ItemStack first, @Nullable ItemStack second) {
        if (first == null || first.getType().isAir()) {
            return second == null || second.getType().isAir();
        } else if (second == null || second.getType().isAir()) {
            return false;
        } else if (first.hasItemMeta()) {
            if (second.hasItemMeta()) {
                ItemMeta firstMeta = first.getItemMeta();
                ItemMeta secondMeta = second.getItemMeta();
                String firstId = getId(firstMeta);
                if (firstId == null) {
                    if (getId(secondMeta) == null) {
                        if (first.getType() == second.getType()) {
                            boolean aHas = firstMeta.hasDisplayName();
                            boolean bHas = secondMeta.hasDisplayName();

                            if (aHas != bHas) return false;
                            if (!aHas) return true;

                            Component a = firstMeta.displayName();
                            Component b = secondMeta.displayName();
                            return Objects.equals(a, b);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return firstId.equals(getId(secondMeta));
                }
            } else {
                return false;
            }
        } else if (second.hasItemMeta()) {
            return false;
        } else {
            return first.getType() == second.getType();
        }
    }

}
