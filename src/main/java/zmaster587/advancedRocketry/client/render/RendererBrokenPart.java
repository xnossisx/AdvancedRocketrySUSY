package zmaster587.advancedRocketry.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.backwardCompat.WavefrontObject;
import zmaster587.advancedRocketry.client.ClientProxy;
import zmaster587.advancedRocketry.tile.TileBrokenPart;

public class RendererBrokenPart extends TileEntitySpecialRenderer<TileBrokenPart> {

    @Override
    public void render(TileBrokenPart tile, double x, double y, double z, float t, int damage, float a) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        ResourceLocation res = tile.getBlockType().getRegistryName();
        String name = tile.getBlockType().getUnlocalizedName().split("\\.")[1].toLowerCase();
        String pathToTexture = "textures/models/" + name + "_" + tile.getStage() / 3 + ".png";
        this.bindTexture(new ResourceLocation(res.getResourceDomain(), pathToTexture));

        WavefrontObject model = ClientProxy.getModel(new ResourceLocation(res.getResourceDomain(), "models/block/models/" + name + ".obj"));
        model.renderAll();

        GlStateManager.popMatrix();
    }
}
