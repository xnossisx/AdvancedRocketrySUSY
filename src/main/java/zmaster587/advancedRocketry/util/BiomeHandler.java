package zmaster587.advancedRocketry.util;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.Sys;
import zmaster587.advancedRocketry.api.IPlanetaryProvider;
import zmaster587.advancedRocketry.api.dimension.IDimensionProperties;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.advancedRocketry.world.ChunkProviderPlanet;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static zmaster587.advancedRocketry.util.AstronomicalBodyHelper.getAverageTemperature;
import static zmaster587.advancedRocketry.util.AstronomicalBodyHelper.getOrbitalPeriod;

public class BiomeHandler {



    //try to not change this method definition again because it will mess up the REID mixin
    public static void changeBiome(World world, Biome biomeId, BlockPos pos, boolean was_biome_remote, int dimId) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);

        Biome biome = world.getBiome(pos);

        if (biomeId == null)return;

        BlockPos yy = world.getHeight(pos);

        if (biome != biomeId) {
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
        }

        //for biome remote use, only change and do simple decoration
        if(was_biome_remote) {
            yy = world.getHeight(pos);
            while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                yy = yy.down();

            decorateBiome(world, yy, biomeId);
        }

        if (!was_biome_remote) { // heavy terraforming here...
            //if (world.provider instanceof WorldProviderPlanet) {
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dimId);

            IBlockState[] target_blocks = props.terraformingHelper.getBlocksAt(yy.getX(), yy.getZ()); // this causes a not generated chunk to generate

            if (!props.terraformingHelper.getChunkFromList(chunk.x, chunk.z).fully_generated[yy.getX() % 16][yy.getZ() % 16]) {
                //fast replacing
                for (int i = 5; i < 255; i++) {
                    world.setBlockState(new BlockPos(yy.getX(), i, yy.getZ()), target_blocks[i]);
                }

                // check if the terrain is fully generated to target height
                int current_height = world.getHeight(yy.getX(),yy.getZ()); // returns the y value above the highest block
                int target_height = 0;
                for (int i = 255; i > 5; i--) {
                    // go down until there is a non-air block, this is target height
                    if (target_blocks[i] != Blocks.AIR.getDefaultState()){
                        target_height = i+1;
                        break;
                    }
                }
                // as long as terrain does not match the target height, re-add position to queue
                if (current_height == target_height){
                    props.terraformingHelper.getChunkFromList(chunk.x, chunk.z).set_position_fully_generated(yy.getX() % 16, yy.getZ() % 16);
                }else{
                    props.terraformingHelper.add_position_to_queue(yy);
                }
            }


            if (props.terraformingHelper.can_populate(chunk.x, chunk.z)) {

                // we do some hacky tricks here to ensure that trees can fully generate
                // we shift the actual tree generation by 8 blocks so that it overlaps with the chunks next to it
                // can_populate() ensures that the chunks next to it are ready for decoration

                if (!props.terraformingHelper.getChunkFromList(chunk.x,chunk.z).fully_decorated[yy.getX()%16][yy.getZ()%16]) {

                    int treegen = biomeId.decorator.treesPerChunk;
                    if (world.rand.nextInt(16 * 16) < treegen)
                        biomeId.getRandomTreeFeature(world.rand).generate(world, world.rand, world.getHeight(yy.add(8, 0, 8)));

                    props.terraformingHelper.getChunkFromList(chunk.x, chunk.z).set_position_decorated(yy.getX() % 16, yy.getZ() % 16);
                }
            }

        }


        PacketHandler.sendToNearby(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 256);

    }

    private static void spawn_decoration_block(World world, BlockPos pos, IBlockState state, int p){
        if (p < 0){
            System.out.println("p should not be negative!!");
            return;
        }
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
            spawn_decoration_block(world, pos, mushroom_b, chunksize / decorator.mushroomsPerChunk / 2);
            spawn_decoration_block(world, pos, mushroom_r, chunksize / decorator.mushroomsPerChunk / 2);
        }
        if (decorator.cactiPerChunk > 0) {
            IBlockState cactus = Blocks.CACTUS.getDefaultState();
            spawn_decoration_block(world, pos, cactus, chunksize / decorator.cactiPerChunk);
        }
    }

}



