package zmaster587.advancedRocketry.entity.fx;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import static java.lang.Math.min;

public class TrailFx extends InverseTrailFx {

    float max_speed_increase = 1.5f;
    float current_speed_increase = 1.0f;
    int max_engines_for_calculation = 64;

    //increase x-z motion
    public void register_additional_engines(int n){
        float enginepx = min(1,n/(float)max_engines_for_calculation);
        float d = max_speed_increase - current_speed_increase;
        current_speed_increase = current_speed_increase+d*enginepx;
    }

    public TrailFx(World world, double x,
                   double y, double z, double motx, double moty, double motz) {
        super(world, x, y, z, motx, moty, motz);

        this.prevPosX = this.posX = x;
        this.prevPosY = this.posY = y;
        this.prevPosZ = this.posZ = z;

        float chroma = this.rand.nextFloat() * 0.2f;
        this.particleRed = .8F + chroma;
        this.particleGreen = .8F + chroma;
        this.particleBlue = .8F + chroma;
        this.setSize(0.12F, 0.12F);
        this.particleScale = (this.rand.nextFloat() * 0.6F + 6F)*0.8f;
        this.motionX = motx;
        this.motionY = moty;
        this.motionZ = motz;
        this.particleMaxAge = (int) world.rand.nextInt(400) + 50;

        icon = new ResourceLocation("advancedrocketry:textures/particle/soft1.png");
    }

    @Override
    public int getFXLayer() {
        return 1;
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
        this.particleAlpha = min(1 - (this.particleAge / (float) this.particleMaxAge), particleAge/20f);
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
                this.posY = ch + 1;

                this.motionX = (world.rand.nextFloat() - 0.5) / 4;
                this.motionZ = (world.rand.nextFloat() - 0.5) / 4;


            }
            if (this.motionY < 0) {
                //fast slowdown when near ground AND moving lower
                if (this.posY - ch < 10) {
                    this.motionY *= 0.99;
                }
            }
            this.motionY *= 1-(0.02/(current_speed_increase));
            this.motionY += 0.0005;

        //this.motionX *= 1-(0.02/(current_speed_increase));
        //this.motionZ *= 1-(0.02/(current_speed_increase));

        this.setPosition(posX + this.motionX*current_speed_increase, posY + this.motionY, posZ + this.motionZ*current_speed_increase);
    }
}
