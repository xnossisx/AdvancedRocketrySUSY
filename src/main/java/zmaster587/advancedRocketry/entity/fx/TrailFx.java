package zmaster587.advancedRocketry.entity.fx;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TrailFx extends InverseTrailFx {



    public TrailFx(World world, double x,
                   double y, double z, double motx, double moty, double motz) {
        super(world, x, y, z, motx, moty, motz);

        this.prevPosX = this.posX = x;
        this.prevPosY = this.posY = y;
        this.prevPosZ = this.posZ = z;

        float chroma = this.rand.nextFloat() * 0.2f;
        this.particleRed = .4F + chroma;
        this.particleGreen = .4F + chroma;
        this.particleBlue = .4F + chroma;
        this.setSize(0.12F, 0.12F);
        this.particleScale = (this.rand.nextFloat() * 0.6F + 6F)*0.8f;
        this.motionX = motx;
        this.motionY = moty;
        this.motionZ = motz;
        this.particleMaxAge = (int) world.rand.nextInt(300) + 50;

        icon = new ResourceLocation("advancedrocketry:textures/particle/soft1.png");
    }

    @Override
    public int getFXLayer() {
        return 0;
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        //Change color and alpha over lifespan
        this.particleAlpha = 1 - (this.particleAge / (float) this.particleMaxAge);
        double initial_expansion = 1.006;
        double final_expansion = 1.004;
        double current_expansion = initial_expansion - (initial_expansion - final_expansion) * (this.particleAge / (float) this.particleMaxAge);
        this.particleScale *= (float) current_expansion;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }
        int ch = world.getHeight((int) this.posX, (int) this.posZ);
        if (this.posY < ch + 1) {
            this.motionY = 0;
            this.posY = ch +1 ;
            //double particlespeed = 0.25* Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ);

            this.motionX = (world.rand.nextFloat() - 0.5) / 4;
            this.motionZ = (world.rand.nextFloat() - 0.5) / 4;
            //this.motionY = (world.rand.nextFloat()) / 40;

            //double new_speed = Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ);
            //if (new_speed < particlespeed) {
            //    motionX *= particlespeed / new_speed;
            //    motionY *= particlespeed / new_speed;
            //    motionZ *= particlespeed / new_speed;
            //}

        }
        if (this.motionY < 0) {
            //fast slowdown when near ground AND moving lower
            if (this.posY - ch < 10) {
                this.motionY *= 0.99;
            }
        }
        this.motionY *= 0.98;
        this.motionY += 0.0005;

        this.setPosition(posX + this.motionX, posY + this.motionY, posZ + this.motionZ);
    }
}
