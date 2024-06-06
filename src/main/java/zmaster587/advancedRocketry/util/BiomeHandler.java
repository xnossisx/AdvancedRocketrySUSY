package zmaster587.advancedRocketry.util;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Loader;
import zmaster587.advancedRocketry.api.IPlanetaryProvider;
import zmaster587.advancedRocketry.api.dimension.IDimensionProperties;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static zmaster587.advancedRocketry.util.AstronomicalBodyHelper.getAverageTemperature;

public class BiomeHandler {



    public static void changeBiome(World world, Biome biomeId, BlockPos pos, boolean regen_vegetation) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);

        Biome biome = world.getBiome(pos);

        if (biome != biomeId) {
            BlockPos yy = world.getHeight(pos);
            if (biome.topBlock != biomeId.topBlock) {

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
            decorateBiome(world, yy, biomeId);

            PacketHandler.sendToNearby(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 256);
        }
        //for biome remote use
        else if(regen_vegetation) {
            BlockPos yy = world.getHeight(pos);

            while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                yy = yy.down();

            if (world.getBlockState(yy.down()) == biome.topBlock)
                world.setBlockState(yy.down(), biomeId.topBlock);


            decorateBiome(world, yy, biomeId);

            PacketHandler.sendToNearby(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 256);

        }


    }

    private static void spawn_decoration_block(World world, BlockPos pos, IBlockState state, int p){
        if (world.rand.nextInt(p) == 0) {
            if (world.isAirBlock(pos) && state.getBlock().canPlaceBlockAt(world, pos)) {
                world.setBlockState(pos, state);
            }
        }
    }


    private static void decorateBiome(World world, BlockPos pos, Biome biome) {
        Random rand = world.rand;
        BiomeDecorator decorator = biome.decorator;
        int chunksize = 16 * 16;

        // if block above topblock == grass/flower..., remove it
        Block block_above_biome_topblock = world.getBlockState(pos).getBlock();
        if (block_above_biome_topblock instanceof BlockTallGrass ||
                block_above_biome_topblock instanceof BlockFlower ||
                block_above_biome_topblock instanceof BlockCactus ||
                block_above_biome_topblock instanceof BlockMushroom
        )
            world.setBlockState(pos, Blocks.AIR.getDefaultState());


        IBlockState deadbush = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.DEAD_BUSH);
        IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
        IBlockState fern = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.FERN);


        if (decorator.flowersPerChunk > 0)
            spawn_decoration_block(world, pos, biome.pickRandomFlower(rand, pos).getBlockType().getBlock().getDefaultState(), chunksize / decorator.flowersPerChunk);

        if (decorator.deadBushPerChunk > 0)
            spawn_decoration_block(world, pos, deadbush, chunksize / decorator.deadBushPerChunk);

        if (decorator.grassPerChunk > 0) {
            int grass_rate = chunksize / decorator.grassPerChunk;
            // increase fern chance in cold / forest
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST)) {
                spawn_decoration_block(world, pos, fern, grass_rate / 2);
                spawn_decoration_block(world, pos, grass, grass_rate / 2);
            } else {
                spawn_decoration_block(world, pos, fern, (int) (grass_rate / 0.2));
                spawn_decoration_block(world, pos, grass, (int) (grass_rate / 0.8));
            }
        }

        if (decorator.mushroomsPerChunk > 0) {
            IBlockState mushroom_b = Blocks.BROWN_MUSHROOM.getDefaultState();
            IBlockState mushroom_r = Blocks.RED_MUSHROOM.getDefaultState();
            spawn_decoration_block(world, pos, mushroom_b, decorator.mushroomsPerChunk / 2);
            spawn_decoration_block(world, pos, mushroom_r, decorator.mushroomsPerChunk / 2);
        }
        if (decorator.cactiPerChunk > 0) {
            IBlockState cactus = Blocks.CACTUS.getDefaultState();
            spawn_decoration_block(world, pos, cactus, decorator.cactiPerChunk);
        }
    }

}



