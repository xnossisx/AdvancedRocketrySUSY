package zmaster587.advancedRocketry.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeManager;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.advancedRocketry.world.ChunkProviderPlanet;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.RandomUtils.nextInt;

enum TerraformingType{
    ALLOWED, BORDER, PROTECTED
}

public class TerraformingHelper {
    public BiomeProvider chunkMgrTerraformed;
    public int dimId;
    public List<BiomeManager.BiomeEntry> biomeList;
    public World world;
    public ChunkProviderPlanet generator;
    private DimensionProperties props;
    public List<chunkdata> chunkdataList;

    // A block is placed in queue if (OR)
    // - Initial block placements - both only set blocks if the terrain at the BlockPos is not fully generated in the "real" world
    //      - The TerraformingHelper is created ( causes all loaded chunks to place their blocks here, from DimProps )
    //      - A new chunk is loaded ( called from PlanetEventHandler )
    // - After the BiomeHandler has made a change at a BlockPos
    //      - For type ALLOWED: if the terrain heigt does not match the target, re-add the block to the queue
    //      - For type BORDER: if BiomeHandler has made a change of the height at BlockPos re-add to queue
    //      - For type BORDER: if a type.ALLOWED has changed its height it will add blocks next to it (if type==BORDER) to the queue
    //      - For type PROTECTED: the block is never re-added to queue because it will change its biome only once
    // - When a protecting Block has been removed and chunk status is re-calculated
    //   add every chunk that was PROTECTED and no longer is PROTECTED to the queue for terraforming.
    //   doesn't matter if it is type ALLOWED or type BORDER
    public List<BlockPos> terraformingqueue;

    int safe_zone_radius = 3;
    int border_zone = 1;


    public TerraformingHelper(int dimension, List<BiomeManager.BiomeEntry> biomes, List<ChunkPos> generated_chunks){
        this.dimId = dimension;
        this.props = DimensionManager.getInstance().getDimensionProperties(dimension);
        this.biomeList = biomes;
        this.world = net.minecraftforge.common.DimensionManager.getWorld(dimId);
        this.chunkMgrTerraformed = new ChunkManagerPlanet(world, world.getWorldInfo().getGeneratorOptions(), biomeList);
        this.terraformingqueue = new LinkedList<>();
        chunkdataList = new LinkedList<>();
        generator = new ChunkProviderPlanet(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), world.getWorldInfo().getGeneratorOptions());

        for (ChunkPos i:generated_chunks){
            chunkdata data = new chunkdata(i.x,i.z,null, world, this);
            data.chunk_fully_generated = true;
            chunkdataList.add(data);
        }
        recalculate_chunk_status();
    }

    public boolean can_populate(int x, int z){
        chunkdata currentchunk = getChunkFromList(x,z);
        chunkdata currentchunkx1 = getChunkFromList(x+1,z);
        chunkdata currentchunkz1 = getChunkFromList(x,z+1);
        chunkdata currentchunkx1z1 = getChunkFromList(x+1,z+1);

        if (currentchunk != null && currentchunkz1 != null && currentchunkx1 != null && currentchunkx1z1 != null){
            if (currentchunkz1.terrain_fully_generated && currentchunkx1.terrain_fully_generated && currentchunkx1z1.terrain_fully_generated && currentchunk.terrain_fully_generated)
                return true;
        }
        return false;
    }
    public void check_can_decorate(int px, int pz){
        // for every chunk next to this one, check if it can decorate (except for if it is already decorated)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (getChunkFromList(px+x,pz+z) != null && !getChunkFromList(px+x,pz+z).chunk_fully_generated){
                    if (can_populate(px+x,pz+z)){
                        //re-add all position to queue for decoration
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                terraformingqueue.add(new BlockPos((px+x)*16+bx, 0, (pz+z)*16+bz));
                            }
                        }
                    }
                }
            }
        }

    }

    public void recalculate_chunk_status() {

        for (int j = 0; j < chunkdataList.size(); j++) {
            chunkdataList.get(j).type = TerraformingType.ALLOWED;
        }

        for (BlockPos i : props.terraformingProtectedBlocks) {
            Chunk chunk = world.getChunkFromBlockCoords(i);
            for (int x = -safe_zone_radius; x <= safe_zone_radius; x++) {
                for (int z = -safe_zone_radius; z <= safe_zone_radius; z++) {

                    if (getChunkFromList(chunk.x + x, chunk.z + z) != null) {
                        getChunkFromList(chunk.x + x, chunk.z + z).type = TerraformingType.PROTECTED;

                        for (int x2 = -border_zone; x2 <= border_zone; x2++) {
                            for (int z2 = -border_zone; z2 <= border_zone; z2++) {
                                if (getChunkFromList(chunk.x + x+x2, chunk.z + z+z2) != null) {
                                    if (getChunkFromList(chunk.x + x+x2, chunk.z + z+z2).type == TerraformingType.ALLOWED) {
                                        getChunkFromList(chunk.x + x + x2, chunk.z + z + z2).type = TerraformingType.BORDER;
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public chunkdata getChunkFromList(int x, int z){
        for (int i = 0; i < chunkdataList.size(); i++) {
            if (chunkdataList.get(i).x == x && chunkdataList.get(i).z == z){
                return chunkdataList.get(i);
            }
        }
        return null;
    }

    public void setPositionReady(int x, int z){
        Chunk chunk = world.getChunkFromBlockCoords(new BlockPos(x,0,z));
        getChunkFromList(chunk.x, chunk.z).set_position_fully_generated(x%16, z%16);
    }

    public void add_position_to_queue(BlockPos p){
            terraformingqueue.add(p);
    }

    public BlockPos get_next_position(boolean random){
        if (terraformingqueue.isEmpty())
            return null;
        int index = 0;
        if (random)
            index = nextInt(0,terraformingqueue.size());
        return terraformingqueue.remove(0);
    }

    public IBlockState[] getBlocksAt(int x, int z){
        Chunk chunk = world.getChunkFromBlockCoords(new BlockPos(x,0,z));
        chunkdata data = getChunkFromList(chunk.x,chunk.z);
        if (data == null){
            ChunkPrimer primer = generator.getChunkPrimer(chunk.x, chunk.z, chunkMgrTerraformed);

            IBlockState[][][] blockStates = new IBlockState[16][16][256];
            for (int px = 0; px < 16; px++) {
                for (int pz = 0; pz < 16; pz++) {
                    for (int py = 0; py < 256; py++) {
                        blockStates[px][pz][py] = primer.getBlockState(px,py,pz);
                    }
                }
            }

            data = new chunkdata(chunk.x,chunk.z, blockStates, world, this);
            chunkdataList.add(data);
            recalculate_chunk_status();
        }
        int chunkx = x%16;
        int chunkz = z%16;

        if (data.blockStates == null)
            return null;

        return data.blockStates[chunkx][chunkz];
    }


    public void setChunkFullyGenerated(int x, int z) {
        getChunkFromList(x,z).chunk_fully_generated = true;
        props.terraformingChunksDone.add(new ChunkPos(x,z));
    }
}
