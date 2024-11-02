package zmaster587.advancedRocketry.block.susy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.libVulpes.block.BlockFullyRotatable;

import javax.annotation.Nullable;

public class BlockRocketHatch extends BlockFullyRotatable {
    public BlockRocketHatch(Material mat) {
        super(mat);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        EnumFacing orientation = (EnumFacing) worldIn.getBlockState(pos).getProperties().get(FACING);
        if (orientation.getOpposite().equals(facing) || orientation.equals(facing)) {
            BlockPos newPos = pos.add(facing.getOpposite().getDirectionVec());
            int yChange = 0;
            if (worldIn.isAirBlock(newPos.add(0,-1,0))) {
                yChange = -1;
            }
            playerIn.setPositionAndUpdate(newPos.getX()+0.5, newPos.getY() + yChange, newPos.getZ()+0.5);
            //wacky stuff happens here
        }
        return true;
    }
}
