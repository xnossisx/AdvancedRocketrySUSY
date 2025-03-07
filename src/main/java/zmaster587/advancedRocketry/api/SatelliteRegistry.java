package zmaster587.advancedRocketry.api;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.item.ItemSatellite;
import zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip;

public class SatelliteRegistry {

    static HashMap<String, Class<? extends SatelliteBase>> registry = new HashMap<>();

    static HashMap<ItemStack, SatelliteProperties> itemPropertiesRegistry = new HashMap<>();

    /**
     * Registers an itemStack with a satellite property, this is used in the Satellite Builder to determine the effect
     * of a component
     *
     * @param stack      stack to register, stacksize insensitive
     * @param properties Satellite Properties to register the ItemStack with
     */
    public static void registerSatelliteProperty(@Nonnull ItemStack stack, SatelliteProperties properties) {
        if (stack.isEmpty()) {
            Logger.getLogger(Constants.modId).warning("Empty satellite property being registered!");
        } else if (!itemPropertiesRegistry.containsKey(stack))
            itemPropertiesRegistry.put(stack, properties);
        else
            Logger.getLogger(Constants.modId).warning("Duplicate satellite property being registered for " + stack);
    }

    /**
     * @param stack ItemStack to get the SatelliteProperties of, stacksize insensitive
     * @return the registered SatelliteProperties of the stack, or null if not registered
     */
    public static SatelliteProperties getSatelliteProperty(@Nonnull ItemStack stack) {
        for (ItemStack keyStack : itemPropertiesRegistry.keySet()) {
            if (keyStack.getItem() == stack.getItem() &&
                    (!keyStack.getHasSubtypes() || keyStack.getItemDamage() == stack.getItemDamage())) {
                return itemPropertiesRegistry.get(keyStack);
            }
        }

        return null;
    }

    /**
     * Registers a satellite class with a string ID, used for loading and saving satellites
     *
     * @param name  String id to register the satellite class to
     * @param clazz class to register
     */
    public static void registerSatellite(String name, Class<? extends SatelliteBase> clazz) {
        registry.put(name, clazz);
    }

    /**
     * @param clazz Satellite Class to get the String identifier for
     * @return String identifier for clazz
     */
    public static String getKey(Class<? extends SatelliteBase> clazz) {
        for (Entry<String, Class<? extends SatelliteBase>> entrySet : registry.entrySet()) {
            if (entrySet.getValue() == clazz)
                return entrySet.getKey();
        }
        // TODO: throw exception
        return "poo";
    }

    /**
     * Handles loading a satellite from nbt, does NOT add it to list of functioning satellites
     *
     * @param nbt NBT to create a satellite Object from
     * @return Satellite constructed from the passed NBT
     */
    public static SatelliteBase createFromNBT(NBTTagCompound nbt) {
        SatelliteBase satellite = getNewSatellite(nbt.getString("dataType"));

        satellite.readFromNBT(nbt);

        return satellite;
    }

    /**
     * @param name String identifier for a satellite
     * @return new satellite registered to the String identifier, SatelliteDefunct otherwise
     */
    public static SatelliteBase getNewSatellite(String name) {
        Class<? extends SatelliteBase> clazz = registry.get(name);

        if (clazz == null) {
            return null;
        } else
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                return null;
            }
    }

    /**
     * @param stack Satellite Chip or Satellite Chassis to get the ID of
     * @return ID of the satellite, or -1 if not found
     */
    public static long getSatelliteId(@Nonnull ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();

            if (nbt != null) {
                if (stack.getItem() instanceof ItemSatelliteIdentificationChip)
                    return nbt.getLong("satelliteId");
                else
                    return nbt.getLong("satId");
            }
        }
        return -1;
    }

    /**
     * @param stack Satellite Chip or Satellite Chassis to get the SatelliteBase of
     * @return SatelliteBase the satellite is, or null if not found
     */
    public static SatelliteBase getSatellite(@Nonnull ItemStack stack) {
        if (SatelliteRegistry.getSatelliteId(stack) != -1)
            return DimensionManager.getInstance().getSatellite(SatelliteRegistry.getSatelliteId(stack));
        return null;
    }

    public static SatelliteProperties getSatelliteProperties(@Nonnull ItemStack stack) {
        if (SatelliteRegistry.getSatelliteId(stack) != -1) {
            NBTTagCompound nbt = stack.getTagCompound();

            if (nbt != null) {
                if (stack.getItem() instanceof ItemSatellite) {
                    SatelliteProperties properties = new SatelliteProperties(nbt.getInteger("powerGeneration"),
                            nbt.getInteger("powerStorage"), nbt.getString("dataType"), nbt.getInteger("maxData"),
                            nbt.getFloat("weight"));
                    properties.setId(SatelliteRegistry.getSatelliteId(stack));
                    return properties;
                }
            }
        }
        return null;
    }
}
