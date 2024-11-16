package zmaster587.advancedRocketry.block.susy;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityFluidHatch;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class MTETankFluidHatch extends MetaTileEntityFluidHatch {
    @Override
    public ICubeRenderer getBaseTexture() {
        return Textures.PLASCRETE;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MTETankFluidHatch(this.metaTileEntityId);
    }

    // measured in buckets, flood fill again!
    public int getCapacity() {
        BlockPos beg = getPos().add(getFrontFacing().getOpposite().getDirectionVec());
        //flood fill when I get to it
        return 0;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay()) {
            SimpleOverlayRenderer renderer = Textures.PIPE_IN_OVERLAY;
            renderer.renderSided(getFrontFacing(), renderState, translation, pipeline);
            SimpleOverlayRenderer overlay = Textures.FLUID_HATCH_INPUT_OVERLAY;
            overlay.renderSided(getFrontFacing(), renderState, translation, pipeline);
        }
    }
    public MTETankFluidHatch(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, 0, false);
        //this.fluidTank = new HatchFluidTank(this.getInventorySize(), this, isExportHatch);
        //this.setWorkingEnabled(true);
        //this.initializeInventory();
    }
}
