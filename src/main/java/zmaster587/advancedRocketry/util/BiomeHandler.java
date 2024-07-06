package zmaster587.advancedRocketry.util;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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

    public static void decorate_simple(World world, Biome biomeId, Biome old_biome, BlockPos pos) {

        BlockPos yy = world.getHeight(pos);
        while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
            yy = yy.down();

        if (old_biome.topBlock != biomeId.topBlock) {
            if (world.getBlockState(yy.down()) == old_biome.topBlock)
                world.setBlockState(yy.down(), biomeId.topBlock);
        }

        decorateBiome(world, yy, biomeId);
    }

    // Bro I am sorry for changing this again and I know it will mess up your mixin but terraforming had a big update so....
    public static void changeBiome(World world, Biome biomeId, Biome old_biome, BlockPos pos) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (old_biome != biomeId) {
            byte[] biomeArr = chunk.getBiomeArray();
            try {
                biomeArr[(pos.getX() & 15) + (pos.getZ() & 15) * 16] = (byte) Biome.getIdForBiome(biomeId);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void do_heavy_terraforming(World world, Biome biomeId, Biome old_biome, BlockPos pos, int dimId){

        int inchunkx = ((pos.getX() % 16) + 16) % 16;
     int inchunkz = ((pos.getZ() % 16) + 16) % 16;

        long startTime;
        startTime = System.currentTimeMillis();
        DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dimId);

        ChunkPos cpos = props.terraformingHelper.getChunkPosFromBlockPos(pos);


        IBlockState[] target_blocks = props.terraformingHelper.getBlocksAt(pos.getX(), pos.getZ()); // 4-6ms
        chunkdata data = props.terraformingHelper.getChunkFromList(cpos.x, cpos.z); // 4-6ms

        if (data.type == TerraformingType.PROTECTED){
            //System.out.println("working protected");
            decorate_simple(world, biomeId,old_biome, pos);
            props.terraformingHelper.getChunkFromList(cpos.x, cpos.z).set_position_fully_generated(inchunkx,inchunkz);
            props.terraformingHelper.register_height_change(pos); // it does not really changetheheight but it will notify the border to update
        }
        else if (data.type == TerraformingType.ALLOWED) {
            //System.out.println("working full");

            if (!data.fully_generated[inchunkx][inchunkz]) {
                //fast replacing
                for (int i = 5; i < 255; i++) {
                    world.setBlockState(new BlockPos(pos.getX(), i, pos.getZ()), target_blocks[i], 2);
                }

                // check if the terrain is fully generated to target height
                int current_height = world.getHeight(pos.getX(), pos.getZ()); // returns the y value above the highest block
                int target_height = 0;
                for (int i = 255; i > 5; i--) {
                    // go down until there is a non-air block, this is target height
                    if (target_blocks[i] != Blocks.AIR.getDefaultState()) {
                        target_height = i + 1;
                        break;
                    }
                }
                // as long as terrain does not match the target height, re-add position to queue
                if (current_height == target_height) {
                    props.terraformingHelper.getChunkFromList(cpos.x, cpos.z).set_position_fully_generated(inchunkx, inchunkz);
                } else {
                    props.terraformingHelper.add_position_to_queue(pos);
                    props.terraformingHelper.register_height_change(pos);
                }

            }
        }

        else if (data.type == TerraformingType.BORDER){
            //System.out.println("working border");

            world.getChunkFromBlockCoords(pos.add(-1,0,0)); // Ensure the chunk of the target positions are generated
            world.getChunkFromBlockCoords(pos.add(1,0,0)); // Ensure the chunk of the target positions are generated
            world.getChunkFromBlockCoords(pos.add(0,0,-1)); // Ensure the chunk of the target positions are generated
            world.getChunkFromBlockCoords(pos.add(0,0,1)); // Ensure the chunk of the target positions are generated

            int next_heightsx0 = world.getHeight(pos.add(-1,0,0)).getY();
            int next_heightsx1 = world.getHeight(pos.add(1,0,0)).getY();
            int next_heightsz0 = world.getHeight(pos.add(0,0,-1)).getY();
            int next_heightsz1 = world.getHeight(pos.add(0,0,1)).getY();

            int avg_height = Math.round ((float) (next_heightsz1 + next_heightsx1 + next_heightsx0 + next_heightsz0) / 4);

            int prev_height = world.getHeight(pos).getY();
            if (avg_height == prev_height){ // nothing to do
                return;
            }

            //fast replacing
            for (int i = 5; i < 256; i++) {
                IBlockState target= target_blocks[i];
                if (i < avg_height)
                    if (target == Blocks.AIR.getDefaultState())
                        target = biomeId.topBlock;
                if (i == avg_height)
                    target = biomeId.topBlock;
                if (i > avg_height)
                    target = Blocks.AIR.getDefaultState();
                world.setBlockState(new BlockPos(pos.getX(), i, pos.getZ()), target, 2);
            }

            int new_height = world.getHeight(pos).getY();

            if (prev_height != new_height){
                props.terraformingHelper.register_height_change(pos);
                props.terraformingHelper.add_position_to_queue(pos);
            }else{
                props.terraformingHelper.check_next_border_chunk_fully_generated(cpos.x,cpos.z); // maybe this was the last border block in queue? if yes, its terrain is done!
            }
        }
        //System.out.println("d1: " +(System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

        int can_populate = props.terraformingHelper.can_populate(cpos.x, cpos.z);
        if (can_populate == -1){
            //because it can never be populated, it is considered "done with population"
            props.terraformingHelper.getChunkFromList(cpos.x, cpos.z).set_position_decorated(inchunkx, inchunkz);
        }
        if (can_populate == 1) {

            // we do some hacky tricks here to ensure that trees can fully generate
            // we shift the actual tree generation by 8 blocks so that it overlaps with the chunks next to it
            // can_populate() ensures that the chunks next to it are ready for decoration

            if (!props.terraformingHelper.getChunkFromList(cpos.x,cpos.z).fully_decorated[inchunkx][inchunkz]) {
                //System.out.println("decorate block");
                int treegen = biomeId.decorator.treesPerChunk;
                if (world.rand.nextInt(16 * 16) < treegen)
                    biomeId.getRandomTreeFeature(world.rand).generate(world, world.rand, world.getHeight(pos.add(8, 0, 8)));

                props.terraformingHelper.getChunkFromList(cpos.x, cpos.z).set_position_decorated(inchunkx, inchunkz);
            }
        }
    }

    public static void terraform(World world, Biome biomeId, BlockPos pos, boolean was_biome_remote, int dimId) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (biomeId == null)return;
        Biome old_biome = world.getBiome(pos);

        changeBiome(world,biomeId, old_biome, pos);

        //for biome remote use, only change top block and do simple decoration
        if(was_biome_remote) {
            decorate_simple(world, biomeId, old_biome, pos);
        }

        if (!was_biome_remote) { // heavy terraforming here...
            do_heavy_terraforming(world, biomeId,old_biome,pos,dimId);
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



