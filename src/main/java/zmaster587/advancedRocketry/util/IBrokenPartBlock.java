package zmaster587.advancedRocketry.util;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import zmaster587.advancedRocketry.tile.TileBrokenPart;

public interface IBrokenPartBlock {

    ItemStack getDropItem(final IBlockState state, final World world, final @Nullable TileBrokenPart te);
}
