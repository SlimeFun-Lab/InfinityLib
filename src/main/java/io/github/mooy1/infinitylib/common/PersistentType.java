package io.github.mooy1.infinitylib.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

import lombok.RequiredArgsConstructor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class with some persistent data types
 *
 * @author Mooy1
 */
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public final class PersistentType<T, Z> implements PersistentDataType<T, Z> {
    private static final Logger log = LoggerFactory.getLogger(PersistentType.class);

    public static final PersistentDataType<byte[], ItemStack> ITEM_STACK = new PersistentType<>(
            byte[].class, ItemStack.class,
            (ItemStack itemStack) -> {
                try {
                    Map<String, Object> m = itemStack.serialize();
                    YamlConfiguration yml = new YamlConfiguration();
                    yml.set("i", m);
                    return yml.saveToString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.error("Failed to serialize ItemStack.", e);
                    return new byte[0];
                }
            },
            arr -> {
                if (arr.length == 0) return CustomItemStack.create(Material.STONE, "&cERROR");

                try {
                    String s = new String(arr, StandardCharsets.UTF_8);
                    YamlConfiguration yml = new YamlConfiguration();
                    yml.loadFromString(s);

                    Object raw = yml.get("i");
                    if (!(raw instanceof Map)) {
                        log.error("ItemStack YAML missing 'i' map or wrong type: {}", raw == null ? "null" : raw.getClass());
                        return CustomItemStack.create(Material.STONE, "&cERROR");
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) raw;

                    return ItemStack.deserialize(m);
                } catch (InvalidConfigurationException ex) {
                    log.error("Invalid YAML while deserializing ItemStack.", ex);
                    return CustomItemStack.create(Material.STONE, "&cERROR");
                } catch (Exception e) {
                    log.error("Failed to deserialize ItemStack.", e);
                    return CustomItemStack.create(Material.STONE, "&cERROR");
                }
            }
    );

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final PersistentDataType<byte[], List<ItemStack>> ITEM_STACK_LIST = new PersistentType<byte[], List<ItemStack>>(
            byte[].class, (Class) List.class,
            list -> {
                try {
                    YamlConfiguration yml = new YamlConfiguration();
                    List<Map<String, Object>> serialized = new ArrayList<>(list.size());
                    for (ItemStack item : list) {
                        serialized.add(item.serialize());
                    }
                    yml.set("items", serialized);
                    return yml.saveToString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.error("Failed to serialize ItemStack list.", e);
                    return new byte[0];
                }
            },
            arr -> {
                List<ItemStack> out = new ArrayList<>();
                if (arr.length == 0) return out;

                try {
                    String s = new String(arr, StandardCharsets.UTF_8);
                    YamlConfiguration yml = new YamlConfiguration();
                    yml.loadFromString(s);

                    Object raw = yml.get("items");
                    if (!(raw instanceof List<?> rawList)) {
                        log.error("ItemStack list YAML missing 'items' list or wrong type: {}", raw == null ? "null" : raw.getClass());
                        return out;
                    }

                    for (Object o : rawList) {
                        if (o instanceof Map<?, ?> map) {
                            Map<String, Object> cast = (Map<String, Object>) map;
                            out.add(ItemStack.deserialize(cast));
                        } else {
                            log.warn("Skipping non-map element in 'items': {}", o == null ? "null" : o.getClass());
                        }
                    }
                } catch (InvalidConfigurationException ex) {
                    log.error("Invalid YAML while deserializing ItemStack list.", ex);
                } catch (Exception e) {
                    log.error("Failed to deserialize ItemStack list.", e);
                }
                return out;
            }
    );

    public static final PersistentDataType<long[], Location> LOCATION = new PersistentDataType<long[], Location>() {
        @Override
        public @NotNull Class<long[]> getPrimitiveType() {
            return long[].class;
        }

        @Override
        public @NotNull Class<Location> getComplexType() {
            return Location.class;
        }

        @Override
        public long @NotNull [] toPrimitive(@NotNull Location loc, @NotNull PersistentDataAdapterContext ctx) {
            long x = Double.doubleToLongBits(loc.getX());
            long y = Double.doubleToLongBits(loc.getY());
            long z = Double.doubleToLongBits(loc.getZ());

            UUID uuid = (loc.getWorld() == null) ? null : loc.getWorld().getUID();
            long msb = (uuid == null) ? 0L : uuid.getMostSignificantBits();
            long lsb = (uuid == null) ? 0L : uuid.getLeastSignificantBits();

            return new long[]{x, y, z, msb, lsb};
        }

        @Override
        public @NotNull Location fromPrimitive(long @NotNull [] data, @NotNull PersistentDataAdapterContext ctx) {
            if (data.length < 5) {
                return new Location(null,0,0,0);
            }

            double x = Double.longBitsToDouble(data[0]);
            double y = Double.longBitsToDouble(data[1]);
            double z = Double.longBitsToDouble(data[2]);

            long msb = data[3];
            long lsb = data[4];

            World world = null;
            if (msb != 0L || lsb != 0L) {
                UUID uuid = new UUID(msb, lsb);
                world = Bukkit.getWorld(uuid);
            }

            return new Location(world, x, y, z);
        }
    };

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final PersistentDataType<byte[], List<String>> STRING_LIST = new PersistentType<byte[], List<String>>(
            byte[].class, (Class) List.class,
            list -> {
               try {
                   YamlConfiguration yml = new YamlConfiguration();
                   yml.set("l", list);
                   return yml.saveToString().getBytes(StandardCharsets.UTF_8);
               } catch (Exception e) {
                   log.error("Failed to serialize String list.", e);
                   return new byte[0];
               }
            },
            arr -> {
                if (arr.length == 0) return Collections.emptyList();

                try {
                    String s = new String(arr, StandardCharsets.UTF_8);
                    YamlConfiguration yml = new YamlConfiguration();
                    yml.loadFromString(s);

                    return yml.getStringList("l");
                }
                catch (InvalidConfigurationException ex) {
                    log.error("Invalid YAML while deserializing String list.", ex);
                    return Collections.emptyList();
                }
                catch (Exception e) {
                    log.error("Failed to deserialize String list.", e);
                    return Collections.emptyList();
                }
            }
    );

    private final Class<T> primitive;
    private final Class<Z> complex;
    private final Function<Z, T> toPrimitive;
    private final Function<T, Z> toComplex;

    @Nonnull
    @Override
    public Class<T> getPrimitiveType() {
        return primitive;
    }

    @Nonnull
    @Override
    public Class<Z> getComplexType() {
        return complex;
    }

    @Nonnull
    @Override
    public T toPrimitive(Z complex, PersistentDataAdapterContext context) {
        return toPrimitive.apply(complex);
    }

    @Nonnull
    @Override
    public Z fromPrimitive(T primitive, PersistentDataAdapterContext context) {
        return toComplex.apply(primitive);
    }

}
