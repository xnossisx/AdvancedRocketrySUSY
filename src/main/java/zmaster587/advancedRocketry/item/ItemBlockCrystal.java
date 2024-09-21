package zmaster587.advancedRocketry.item;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import zmaster587.libVulpes.block.INamedMetaBlock;
import zmaster587.libVulpes.items.ItemBlockMeta;

public class ItemBlockCrystal extends ItemBlockMeta {

    public ItemBlockCrystal(Block p_i45326_1_) {
        super(p_i45326_1_);
    }

    @Override
    public String getTranslationKey(@Nonnull ItemStack stack) {
        return ((INamedMetaBlock) Block.getBlockFromItem(stack.getItem())).getUnlocalizedName(stack.getItemDamage());
    }
}
