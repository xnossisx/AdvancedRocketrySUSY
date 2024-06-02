package zmaster587.advancedRocketry.util;

import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Loader;
import zmaster587.advancedRocketry.api.IPlanetaryProvider;
import zmaster587.advancedRocketry.api.dimension.IDimensionProperties;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import static zmaster587.advancedRocketry.util.AstronomicalBodyHelper.getAverageTemperature;

public class BiomeHandler {


    //public static boolean is_watercontrol_loaded = Loader.isModLoaded("watercontrol");

    public static void changeBiome(World world, Biome biomeId, BlockPos pos) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);

        Biome biome = world.getBiome(pos);


        /*
        // spread and evaporate water - !!only possible if watercontrol is loaded!!
        if (is_watercontrol_loaded) {
            if (world.provider instanceof IPlanetaryProvider) {



                // for ocean/river biomes generate water
                if (biome == Biome.getBiome(0) ||biome == Biome.getBiome(7)) {
                    int sealvl = world.getSeaLevel();
                    BlockPos yy = world.getHeight(pos);


                    while ((!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                            && !(world.getBlockState(yy.down()).getBlock() == Blocks.WATER)) {
                        yy = yy.down();
                    }
                    if (yy.getY() <= sealvl) {
                        world.setBlockState(yy, Blocks.WATER.getDefaultState());
                    }
                }






                //spread water
                if (DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).water_can_exist) {

                    int sealvl = world.getSeaLevel();
                    BlockPos yy = new BlockPos(pos.getX(), sealvl, pos.getZ());


                    while ((!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                            && !(world.getBlockState(yy.down()).getBlock() == Blocks.WATER)
                    ) {
                        yy = yy.down();
                        if (
                                world.getBlockState(yy.east()).getBlock() == Blocks.WATER
                                        || world.getBlockState(yy.north()).getBlock() == Blocks.WATER
                                        || world.getBlockState(yy.west()).getBlock() == Blocks.WATER
                                        || world.getBlockState(yy.south()).getBlock() == Blocks.WATER
                        )
                            world.setBlockState(yy, Blocks.WATER.getDefaultState());

                    }

                }
                //evaporate water
                else {
                    // replace top water block with air
                    BlockPos top_block = world.getHeight(pos).down();
                    if (world.getBlockState(top_block).getBlock() == Blocks.WATER || world.getBlockState(top_block).getBlock() == Blocks.FLOWING_WATER) {
                        world.setBlockState(top_block, Blocks.AIR.getDefaultState());

                    }
                }
            }
        }
        */

        if (biome == biomeId)
            return;

        if (biome.topBlock != biomeId.topBlock) {
            BlockPos yy = world.getHeight(pos);

            while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                yy = yy.down();

            if (world.getBlockState(yy.down()) == biome.topBlock)
                world.setBlockState(yy.down(), biomeId.topBlock);
        }

        byte[] biomeArr = chunk.getBiomeArray();
        try {
            biomeArr[(pos.getX() & 15) + (pos.getZ() & 15) * 16] = (byte) Biome.getIdForBiome(biomeId);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }




        PacketHandler.sendToNearby(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 256);
    }
}
