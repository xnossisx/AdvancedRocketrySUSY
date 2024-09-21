package zmaster587.advancedRocketry.api;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;

public interface ISatelliteIdItem {

    void setSatellite(@Nonnull ItemStack stack, SatelliteProperties properties);
}
