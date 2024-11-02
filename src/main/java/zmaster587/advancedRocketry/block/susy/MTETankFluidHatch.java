package zmaster587.advancedRocketry.block.susy;

import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityFluidHatch;
import net.minecraft.util.ResourceLocation;

public class MTETankFluidHatch extends MetaTileEntityFluidHatch {
    public MTETankFluidHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier, false);
        this.fluidTank = new HatchFluidTank(this.getInventorySize(), this, isExportHatch);
        this.setWorkingEnabled(true);
        this.initializeInventory();
    }
}
