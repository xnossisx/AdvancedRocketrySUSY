package zmaster587.advancedRocketry.api.satellite;

import net.minecraft.nbt.NBTTagCompound;

import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.util.IWeighted;

public class SatelliteProperties implements IWeighted {

    private int powerGeneration, powerStorage, maxData;
    private long id;
    private String satType;
    private float weight;

    public SatelliteProperties() {
        satType = null;
        id = -1;
    }

    public SatelliteProperties(int powerGeneration, int powerStorage, String satType, int maxData, float weight) {
        this();
        this.powerGeneration = powerGeneration;
        this.powerStorage = powerStorage;
        this.satType = satType;
        this.maxData = maxData;
        this.weight = weight;
    }

    /**
     * @return a flag containing the abilities of the item
     */
    public int getPropertyFlag() {
        int flag = 0;
        if (satType != null)
            flag |= Property.MAIN.getFlag();
        if (this.powerGeneration != 0)
            flag |= Property.POWER_GEN.getFlag();
        if (this.powerStorage != 0)
            flag |= Property.BATTERY.getFlag();
        if (this.maxData != 0)
            flag |= Property.DATA.getFlag();

        return flag;
    }

    /**
     * @return the unique ID of the satellite
     */
    public long getId() {
        return id;
    }

    /**
     * Assigns the ID parameter as the new ID of this satellite only if it does not already have one
     *
     * @param id the new Unique Id of the satellite
     * @return true if a new id is assigned, false otherwise
     */
    public boolean setId(long id) {
        if (this.id == -1) {
            this.id = id;
            return true;
        }
        return false;
    }

    /**
     * @return Amount of power per tick this satellite can generate
     */
    public int getPowerGeneration() {
        return powerGeneration;
    }

    /**
     * @param powerGeneration amount of power this satellite can generate
     * @return this
     */
    public SatelliteProperties setPowerGeneration(int powerGeneration) {
        this.powerGeneration = powerGeneration;
        return this;
    }

    public float getWeight() {
        return this.weight;
    }

    /**
     * @return the current size of the power buffer
     */
    public int getPowerStorage() {
        return powerStorage;
    }

    /**
     * @param powerStorage The new size of the power buffer
     * @return this
     */
    public SatelliteProperties setPowerStorage(int powerStorage) {
        this.powerStorage = powerStorage;
        return this;
    }

    /**
     * @param maxData Maximum amount of Data this satellite can store
     * @return this
     */
    public SatelliteProperties setMaxData(int maxData) {
        this.maxData = maxData;
        return this;
    }

    /**
     * @return Maximum size of the data buffer
     */
    public int getMaxDataStorage() {
        return maxData;
    }

    /**
     * @return the string identifying the satellite type as stored in {@link SatelliteRegistry}
     */
    public String getSatelliteType() {
        return satType;
    }

    /**
     * @param type the string identifying satellite Type of this satellite as stored in {@link SatelliteRegistry}
     * @return this
     */
    public SatelliteProperties setSatelliteType(String type) {
        this.satType = type;
        return this;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("powerGeneration", powerGeneration);
        nbt.setInteger("powerStorage", powerStorage);
        nbt.setString("dataType", satType);
        nbt.setLong("satId", id);
        nbt.setInteger("maxData", maxData);
        nbt.setFloat("weight", weight);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        powerGeneration = nbt.getInteger("powerGeneration");
        powerStorage = nbt.getInteger("powerStorage");
        satType = nbt.getString("dataType");
        id = nbt.getLong("satId");
        maxData = nbt.getInteger("maxData");
        weight = nbt.getFloat("weight");
    }

    public enum Property {

        MAIN,
        DATA,
        POWER_GEN,
        BATTERY;

        public int getFlag() {
            return 1 << ordinal();
        }

        public boolean isOfType(int flag) {
            return (flag & getFlag()) != 0;
        }
    }
}
