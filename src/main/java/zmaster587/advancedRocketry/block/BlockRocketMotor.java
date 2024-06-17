package zmaster587.advancedRocketry.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.IRocketEngine;
import zmaster587.libVulpes.block.BlockFullyRotatable;

import javax.annotation.Nonnull;

public class BlockRocketMotor extends BlockFullyRotatable implements IRocketEngine {

    public BlockRocketMotor(Material mat) {
        super(mat);
        this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.DOWN));
    }
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
                                      BlockPos pos) {

        if (world.getBlockState(pos.add(0, 1, 0)).getBlock() instanceof BlockFuelTank){
            return state.withProperty(FACING, EnumFacing.DOWN);
        }
        if (world.getBlockState(pos.add(0, -1, 0)).getBlock() instanceof BlockFuelTank){
            return state.withProperty(FACING, EnumFacing.UP);
        }
        if (world.getBlockState(pos.add(1, 0, 0)).getBlock() instanceof BlockFuelTank){
            return state.withProperty(FACING, EnumFacing.EAST);
        }
        if (world.getBlockState(pos.add(-1, 0, 0)).getBlock() instanceof BlockFuelTank){
            return state.withProperty(FACING, EnumFacing.WEST);
        }
        if (world.getBlockState(pos.add(0, 0, 1)).getBlock() instanceof BlockFuelTank){
            return state.withProperty(FACING, EnumFacing.SOUTH);
        }
        if (world.getBlockState(pos.add(0, 0, -1)).getBlock() instanceof BlockFuelTank){
            return state.withProperty(FACING, EnumFacing.NORTH);
        }
        return state;
    }
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public int getThrust(World world, BlockPos pos) {
        return 10;
    }

    @Override
    public int getFuelConsumptionRate(World world, int x, int y, int z) {
        return 1;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, @Nonnull ItemStack stack) {

        world.setBlockState(pos, state.withProperty(FACING, EnumFacing.DOWN));
    }
}
