package zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage;

import java.util.ArrayList;
import java.util.HashSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import zmaster587.advancedRocketry.util.TerraformingHelper;

public class dimensionTerraformingInfo {

    public dimensionTerraformingInfo() {}

    public ArrayList<BlockPos> terraformingProtectedBlocks;
    public HashSet<ChunkPos> terraformingChunksDone;
    public HashSet<ChunkPos> biomeChangingChunksDone;
    public TerraformingHelper terraformingHelper;
}
