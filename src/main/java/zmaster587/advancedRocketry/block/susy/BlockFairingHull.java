package zmaster587.advancedRocketry.block.susy;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import zmaster587.libVulpes.block.BlockFullyRotatable;

public class BlockFairingHull extends BlockFullyRotatable {
    public BlockFairingHull(Material mat) {
        super(mat);
    }

    Vec3i getNormal(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING).getDirectionVec();
    }
}
