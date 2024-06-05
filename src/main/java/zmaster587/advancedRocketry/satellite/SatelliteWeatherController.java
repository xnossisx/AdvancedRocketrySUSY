package zmaster587.advancedRocketry.satellite;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;
import zmaster587.advancedRocketry.AdvancedRocketry;
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

import static org.apache.commons.lang3.RandomUtils.nextInt;

public class SatelliteWeatherController extends SatelliteBase {

    public int mode_id;
    public int timer;
    public SatelliteWeatherController() {
        super();
        mode_id = 0;
        timer = 0;
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
        if (this.timer > 0){
            this.timer--;
            int listsize = viable_positions.size();
            if (listsize>0){
                BlockPos new_block = viable_positions.remove(nextInt(0,listsize));
                World world = net.minecraftforge.common.DimensionManager.getWorld(getDimensionId());
                world.setBlockState(new_block, Blocks.WATER.getDefaultState());
            }
        }
    }

    public int radius = 16;
    private List <BlockPos> viable_positions;
    private List <BlockPos> failed_positions;


    private boolean is_block_in_list(List<BlockPos> l, BlockPos p){
        for (BlockPos i:l){
            if (i.getX() == p.getX() && i.getZ() == p.getZ() && i.getY() == p.getY()){
                return true;
            }
        }
        return false;
    }
    private boolean can_be_made_water(BlockPos block, World world){
        return  world.getBlockState(block).getBlock() == Blocks.AIR ||
                world.getBlockState(block).getBlock() == Blocks.WATER ||
                world.getBlockState(block).getMaterial().isReplaceable()
        ;
    }
    private boolean is_at_edge(BlockPos init, BlockPos current){
        int dx = init.getX() - current.getX();
        int dz = init.getZ() - current.getZ();
        int r = (int) Math.sqrt( dx*dx+dz*dz );
        return r > radius;
    }

    private boolean check_one_block(BlockPos init, BlockPos block_x, World world, List<BlockPos> checked_blocks, int in){

if (in > 1000){
    ITextComponent chatMessage = new TextComponentString("too deep: " + block_x.getY()+":"+block_x.getY()+":"+block_x.getZ());
    AdvancedRocketry.logger.warn(chatMessage);
    return false;
}
        if (is_at_edge(init, block_x))
            return false;
        if (is_block_in_list(viable_positions, block_x))
            return true;
        if (is_block_in_list(failed_positions, block_x))
            return false;
        if (!is_block_in_list(checked_blocks, block_x) && can_be_made_water(block_x, world)) {
            checked_blocks.add(block_x);
            if (!find_connected_blocks_down(init, block_x, world, checked_blocks, in+1))
                return false;
        }
        return true;
    }

    private boolean find_connected_blocks_down(BlockPos init, BlockPos start, World world, List <BlockPos> checked_blocks, int in) {

        if (!check_one_block(init, start.north(), world, checked_blocks, in))
            return false;
        if (!check_one_block(init, start.west(), world, checked_blocks,in))
            return false;
        if (!check_one_block(init, start.south(), world, checked_blocks,in))
            return false;
        if (!check_one_block(init, start.east(), world, checked_blocks,in))
            return false;
        if (!check_one_block(init, start.down(), world, checked_blocks,in))
            return false;


        return true;
    }

    @Override
    public boolean performAction(EntityPlayer player, World world, BlockPos pos) {
        if(world.isRemote)return false;

        if (this.mode_id == 0) {

            long startTime = System.nanoTime();

            failed_positions = new ArrayList<>();
            viable_positions = new ArrayList<>();

            //reset timer so that tick loop can run
            this.timer = 20 * 120; // run for max 120 seconds


            for (int z = -radius; z < radius + 1; z++) {
                for (int x = -radius; x < radius + 1; x++) {

                    BlockPos top_block = world.getHeight(new BlockPos(x + pos.getX(), 0, z + pos.getZ()));

                    // go down until top_block has a solid block below it
                    while (!world.getBlockState(top_block.down()).isOpaqueCube() && top_block.getY() > 0)
                        top_block = top_block.down();

                    boolean was_valid_pos = false;
                    // rek flood check
                    List<BlockPos> checked_blocks = new ArrayList<>();
                    while (find_connected_blocks_down(pos, top_block, world, checked_blocks, 0)) {
                        viable_positions.addAll(checked_blocks);
                        checked_blocks = new ArrayList<>();
                        was_valid_pos = true;
                        // up one block and check again
                        top_block = top_block.up();
                    }
                    if (!was_valid_pos)
                        failed_positions.addAll(checked_blocks);
                }
            }

            long endTime = System.nanoTime();
            long durationInNanoseconds = endTime - startTime;
        } else if (this.mode_id == 1) {
            
        }
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

