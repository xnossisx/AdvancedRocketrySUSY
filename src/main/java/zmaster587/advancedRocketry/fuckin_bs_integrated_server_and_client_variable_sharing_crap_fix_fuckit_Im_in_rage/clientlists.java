package zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;
import zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage.dimensionTerraformingInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class clientlists implements Afuckinginterface {
    Map<Integer, dimensionTerraformingInfo> terraforminginfolists;

    public clientlists(){
        this.terraforminginfolists = new HashMap<>();
    }

    public void setListforDIM(int dim, List<BlockPos> protectingblocklist, List<ChunkPos> chunksDoneList){
        if (terraforminginfolists.get(dim) != null){
            terraforminginfolists.get(dim).terraformingChunksDone = chunksDoneList;
            terraforminginfolists.get(dim).terraformingProtectedBlocks = protectingblocklist;
        }else{
            dimensionTerraformingInfo info = new dimensionTerraformingInfo();
            info.terraformingChunksDone = chunksDoneList;
            info.terraformingProtectedBlocks = protectingblocklist;
            terraforminginfolists.put(dim,info);
        }
    }
    public List<BlockPos> getProtectingBlocksForDimension(int dim){
        return terraforminginfolists.get(dim).terraformingProtectedBlocks;
    }
    public void setProtectingBlocksForDimension(int dim, List<BlockPos> blocks){
        terraforminginfolists.get(dim).terraformingProtectedBlocks = blocks;
    }

    public void setChunksFullyTerraformed(int dim, List<ChunkPos> lpos){
        terraforminginfolists.get(dim).terraformingChunksDone = lpos;
    }
    public List<ChunkPos> getChunksFullyTerraformed(int dim){
        return terraforminginfolists.get(dim).terraformingChunksDone;
    }

    public void sethelper(int dim, TerraformingHelper helper){
        terraforminginfolists.get(dim).terraformingHelper= helper;
    }
    public TerraformingHelper gethelper(int dim){
        if (terraforminginfolists.get(dim) == null)return null;
        return terraforminginfolists.get(dim).terraformingHelper;
    }
}
