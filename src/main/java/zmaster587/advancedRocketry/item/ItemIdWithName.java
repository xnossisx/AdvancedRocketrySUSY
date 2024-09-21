package zmaster587.advancedRocketry.item;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.mojang.realmsclient.gui.ChatFormatting;

public class ItemIdWithName extends Item {

    public void setName(@Nonnull ItemStack stack, String name) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            nbt.setString("name", name);
            stack.setTagCompound(nbt);
        }
    }

    public String getName(@Nonnull ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            return nbt.getString("name");
        }

        return "";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, World player, List<String> list, ITooltipFlag bool) {
        if (stack.getItemDamage() == -1) {
            list.add(ChatFormatting.GRAY + "Unprogrammed");
        } else {
            list.add(getName(stack));
        }
    }
}
