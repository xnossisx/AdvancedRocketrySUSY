package zmaster587.advancedRocketry.item;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemThermite extends Item {

    @Override
    public int getItemBurnTime(@Nonnull ItemStack itemStack) {
        return 6000;
    }
}
