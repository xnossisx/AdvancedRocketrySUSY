package zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage;


import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;

import java.util.List;

public interface Afuckinginterface {

    public void setListforDIM(int dim, List<BlockPos> protectingblocklist, List<ChunkPos> chunksDoneList);

    public List<BlockPos> getProtectingBlocksForDimension(int dim);

    public void setProtectingBlocksForDimension(int dim, List<BlockPos> blocks);

    public void setChunksFullyTerraformed(int dim, List<ChunkPos> lpos);

    public List<ChunkPos> getChunksFullyTerraformed(int dim);

    public void sethelper(int dim, TerraformingHelper helper);

    public TerraformingHelper gethelper(int dim);

}
