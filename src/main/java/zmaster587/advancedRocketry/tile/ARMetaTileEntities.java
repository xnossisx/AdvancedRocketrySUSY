package zmaster587.advancedRocketry.tile;
import zmaster587.advancedRocketry.api.Constants;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.block.susy.MTETankFluidHatch;

import static gregtech.common.metatileentities.MetaTileEntities.registerMetaTileEntity;

public class ARMetaTileEntities {
    public static MTETankFluidHatch TANK_FLUID_HATCH;


    public static void init() {
        TANK_FLUID_HATCH = registerMetaTileEntity(11000,new MTETankFluidHatch(new ResourceLocation(Constants.modId, "tankFluidHatch")));
    }

}
