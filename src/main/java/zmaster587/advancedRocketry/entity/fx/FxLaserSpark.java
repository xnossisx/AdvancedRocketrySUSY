package zmaster587.advancedRocketry.entity.fx;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

public class FxLaserSpark extends Particle {

    double length;

    public FxLaserSpark(World world, double x,
                        double y, double z, double velX, double velY, double velZ, double length) {
        super(world, x, y, z, 0, 0, 0);

        this.prevPosX = this.posX = x;
        this.prevPosY = this.posY = y;
        this.prevPosZ = this.posZ = z;
        this.particleMaxAge = (int) (10.0D);
        this.motionX = velX;
        this.motionY = velY;
        this.motionZ = velZ;
        this.length = length;
    }

    @Override
    public void renderParticle(BufferBuilder worldRendererIn, Entity entityIn,
                               float partialTicks, float rotationX, float rotationZ,
                               float rotationYZ, float rotationXY, float rotationXZ) {
        // worldRendererIn.finishDrawing();

        float x = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - interpPosX);
        float y = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - interpPosY);
        float z = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - interpPosZ);

        int i = this.getBrightnessForRender(0);
        int j = i >> 16 & 65535;
        int k = i & 65535;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        GL11.glLineWidth(1);
        GlStateManager.color(0.8f, 0.2f, 0.2f, particleAlpha);

        x += motionX * particleAge;
        y += motionY * particleAge;
        z += motionZ * particleAge;

        buffer.pos(x, y, z).endVertex();
        buffer.pos(x + motionX * length, y + motionY * length, z + motionZ * length).endVertex();

        Tessellator.getInstance().draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 0, 0);
        GlStateManager.color(1, 1, 1, 1);
        GL11.glLineWidth(1);
    }

    @Override
    public int getFXLayer() {
        return 3;
    }

    @Override
    public void onUpdate() {
        this.particleAlpha = 1 - (particleAge / (float) particleMaxAge);

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }
    }
}
