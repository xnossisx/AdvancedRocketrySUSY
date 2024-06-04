package zmaster587.advancedRocketry.satellite;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;
import zmaster587.advancedRocketry.api.AdvancedRocketryBiomes;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.item.ItemBiomeChanger;
import zmaster587.advancedRocketry.item.ItemWeatherController;
import zmaster587.advancedRocketry.util.BiomeHandler;
import zmaster587.libVulpes.api.IUniversalEnergy;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.util.*;

public class SatelliteWeatherController extends SatelliteBase {

    public int mode_id;
    public SatelliteWeatherController() {
        super();
        mode_id = 0;
    }

    public int getMode_id(){
        return mode_id;
    }
    @Override
    public String getInfo(World world) {
        return "Ready";
    }

    @Override
    public String getName() {
        return "Weather Satellite";
    }

    @Override
    @Nonnull
    public ItemStack getControllerItemStack(@Nonnull ItemStack satIdChip,
                                            SatelliteProperties properties) {

        ItemWeatherController idChipItem = (ItemWeatherController) satIdChip.getItem();
        idChipItem.setSatellite(satIdChip, properties);
        return satIdChip;
    }

    @Override
    public boolean isAcceptableControllerItemStack(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemWeatherController;
    }


    @Override
    public void tickEntity() {
        super.tickEntity();
    }


    @Override
    public boolean performAction(EntityPlayer player, World world, BlockPos pos) {
        if(world.isRemote)return false;


        return false;
    }


    @Override
    public double failureChance() {
        return 0;
    }

    public IUniversalEnergy getBattery() {
        return battery;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("mode_id", mode_id);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        mode_id = nbt.getInteger("mode_id");
    }
}

