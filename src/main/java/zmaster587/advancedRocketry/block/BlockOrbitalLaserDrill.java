package zmaster587.advancedRocketry.block;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import zmaster587.advancedRocketry.advancements.ARAdvancements;
import zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill.TileOrbitalLaserDrill;
import zmaster587.libVulpes.block.multiblock.BlockMultiblockMachine;
import zmaster587.libVulpes.inventory.GuiHandler;

public class BlockOrbitalLaserDrill extends BlockMultiblockMachine {

    public BlockOrbitalLaserDrill() {
        super(TileOrbitalLaserDrill.class, GuiHandler.guiId.MODULAR.ordinal());
        setTickRandomly(true).setTranslationKey("spaceLaser");
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileOrbitalLaserDrill();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    // can happen when lever is flipped... Update the state of the tile
    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        if (!(world.getTileEntity(neighbor) instanceof TileOrbitalLaserDrill))
            ((TileOrbitalLaserDrill) world.getTileEntity(pos)).checkCanRun();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        boolean r = super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
        if (!worldIn.isRemote) {
            if (worldIn.getTileEntity(pos) instanceof TileOrbitalLaserDrill) {
                if (((TileOrbitalLaserDrill) worldIn.getTileEntity(pos)).isComplete()) {
                    ARAdvancements.DEATH_STAR.trigger((EntityPlayerMP) playerIn);
                }
            }
        }
        return r;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (worldIn.getTileEntity(pos) instanceof TileOrbitalLaserDrill)
            ((TileOrbitalLaserDrill) worldIn.getTileEntity(pos)).onDestroy();
    }

    @Override
    public void onBlockExploded(World worldIn, BlockPos pos, Explosion explosionIn) {
        super.onBlockExploded(worldIn, pos, explosionIn);
        if (worldIn.getTileEntity(pos) instanceof TileOrbitalLaserDrill)
            ((TileOrbitalLaserDrill) worldIn.getTileEntity(pos)).onDestroy();
    }

    // To check if the laser is jammed
    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state,
                           Random rand) {
        super.updateTick(worldIn, pos, state, rand);
    }
}
