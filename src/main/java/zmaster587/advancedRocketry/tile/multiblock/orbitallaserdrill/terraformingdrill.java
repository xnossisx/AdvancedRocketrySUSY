package zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.oredict.OreDictionary;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityLaserNode;
import zmaster587.advancedRocketry.util.BiomeHandler;
import zmaster587.advancedRocketry.util.TerraformingHelper;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * This drill is used if the laserDrillPlanet config option is disabled. It simply conjures ores from nowhere
 */
class terraformingdrill extends AbstractDrill{


    private ForgeChunkManager.Ticket ticketLaser;
    private ChunkPos last_worked_chunk = null;
    private EntityLaserNode laser;

    terraformingdrill(){

    }

    private TerraformingHelper get_my_helper(){
        if (!DimensionProperties.proxylists.isinitialized(laser.world.provider.getDimension())){
            DimensionProperties.proxylists.initdim(laser.world.provider.getDimension());
        }

        TerraformingHelper t = DimensionProperties.proxylists.gethelper(laser.world.provider.getDimension());

        if (t == null) {
            DimensionManager.getInstance().getDimensionProperties(laser.world.provider.getDimension()).load_terraforming_helper(false);
            t = DimensionProperties.proxylists.gethelper(laser.world.provider.getDimension());
        }
        return t;
    }

    /**
     * Performs a single drilling operation
     *
     * @return The ItemStacks produced by this tick of drilling
     */
    ItemStack[] performOperation() {
        try {


            TerraformingHelper t = get_my_helper();
            BiomeProvider chunkmgr = t.chunkMgrTerraformed;
            BlockPos next_block_pos = t.get_next_position(false);

            if(next_block_pos == null){
                System.out.println("Queue empty - returning");
                return null;
            }

            // blocks are usually added per chunk so it may be efficient to keep it loaded
            // one chunk has to be loaded anyway to prevent dim unloading

            ChunkPos currentChunk = t.getChunkPosFromBlockPos(next_block_pos);
            if (last_worked_chunk != null){
                if (!currentChunk.equals(last_worked_chunk)){
                    releaseticket();
                    //just load chunk 0 0 to keep the dimension loaded
                    System.out.println("request ticket at chunk "+currentChunk.x+":"+currentChunk.z);
                    ticketLaser = ForgeChunkManager.requestTicket(AdvancedRocketry.instance, laser.world, ForgeChunkManager.Type.NORMAL);
                    if (ticketLaser != null) {
                        ForgeChunkManager.forceChunk(ticketLaser, new ChunkPos(0, 0));
                    }
                }
            }
            last_worked_chunk = currentChunk;


            System.out.println("terraform heightmap at "+next_block_pos.getX()+":"+next_block_pos.getZ());
            laser.setPosition(next_block_pos.getX(),next_block_pos.getY(), next_block_pos.getZ());

            BiomeHandler.terraform(laser.world, ((ChunkManagerPlanet) chunkmgr).getBiomeGenAt(next_block_pos.getX(), next_block_pos.getZ()), next_block_pos, false, laser.world.provider.getDimension());


            //} catch (NullPointerException e) {
            //    e.printStackTrace();
        } catch (NoClassDefFoundError e){
            e.printStackTrace(); //WTF
        }
        return null;
    }

    boolean activate(World world, int x, int z) {




            laser = new EntityLaserNode(world, x, 0, z);
            laser.markValid();
            laser.forceSpawn = true;
            world.spawnEntity(laser);
            return true;

    }

    void releaseticket(){
        if (ticketLaser != null)
            ForgeChunkManager.releaseTicket(ticketLaser);
    }

    void deactivate() {
        if (laser != null) {
            laser.setDead();
            laser = null;
        }

        releaseticket();



    }

    boolean isFinished() {
        return laser==null || !get_my_helper().has_blocks_in_tf_queue();
    }

    boolean needsRestart() {
        return this.laser == null;
    }
}
