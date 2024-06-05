package zmaster587.advancedRocketry.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.advancedRocketry.satellite.SatelliteWeatherController;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.TextureResources;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.INetworkItem;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketItemModifcation;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class ItemWeatherController extends ItemSatelliteIdentificationChip implements IModularInventory, IButtonInventory, INetworkItem {

    @Override
    public List<ModuleBase> getModules(int id, EntityPlayer player) {
        List<ModuleBase> list = new LinkedList<>();

        SatelliteWeatherController sat = (SatelliteWeatherController) getSatellite(player.getHeldItem(EnumHand.MAIN_HAND));
        if (player.world.isRemote) {
            list.add(new ModuleImage(24, 14, zmaster587.advancedRocketry.inventory.TextureResources.earthCandyIcon));
        }

        list.add(new ModuleButton(32, 16 + 24 * (1), 1, "dry", this, TextureResources.buttonBuild));
        list.add(new ModuleButton(32, 16 + 24 * (2), 0, "rain", this, TextureResources.buttonBuild));

        list.add(new ModulePower(16, 48, sat.getBattery()));

        return list;
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, World player, List<String> list, ITooltipFlag arg5) {

        SatelliteBase sat = SatelliteRegistry.getSatellite(stack);

        SatelliteWeatherController mapping = null;
        if (sat instanceof SatelliteWeatherController)
            mapping = (SatelliteWeatherController) sat;

        if (!stack.hasTagCompound())
            list.add(LibVulpes.proxy.getLocalizedString("msg.unprogrammed"));
        else if (mapping == null)
            list.add(LibVulpes.proxy.getLocalizedString("msg.biomechanger.nosat"));
        else if (mapping.getDimensionId() == player.provider.getDimension()) {
            list.add(LibVulpes.proxy.getLocalizedString("msg.connected"));
            if (mapping.mode_id == 0)
                list.add("mode: rain");
            if (mapping.mode_id == 1)
                list.add("mode: dry");
        } else
            list.add(LibVulpes.proxy.getLocalizedString("msg.notconnected"));

        super.addInformation(stack, player, list, arg5);
    }


    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            SatelliteBase sat = SatelliteRegistry.getSatellite(stack);
            if (sat != null) {
                if (player.isSneaking()) {
                    if (getSatellite(stack) != null) {
                        player.openGui(LibVulpes.instance, GuiHandler.guiId.MODULARNOINV.ordinal(), world, -1, -1, 0);
                    }
                } else {
                    //Attempt to change weather only if player is in the same dimension
                    if (sat.getDimensionId() == world.provider.getDimension()) {
                        sat.performAction(player, world, player.getPosition());
                    }
                }
            }
        }
        return super.onItemRightClick(world, player, hand);
    }


    @Override
    public String getModularInventoryName() {
        return "item.weatherController.name";
    }

    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onInventoryButtonPressed(int buttonId) {

        ItemStack stack = Minecraft.getMinecraft().player.getHeldItem(EnumHand.MAIN_HAND);
        SatelliteBase sat = getSatellite(stack);
        if (sat instanceof SatelliteWeatherController) {
            ((SatelliteWeatherController) sat).mode_id = buttonId;
            PacketHandler.sendToServer(new PacketItemModifcation(this, Minecraft.getMinecraft().player, (byte) (buttonId == -1 ? -1 : 0)));
            Minecraft.getMinecraft().player.closeScreen();
        }
    }

    @Override
    public void writeDataToNetwork(ByteBuf byteBuf, byte b, @Nonnull ItemStack itemStack) {

    }

    @Override
    public void readDataFromNetwork(ByteBuf byteBuf, byte b, NBTTagCompound nbtTagCompound, @Nonnull ItemStack itemStack) {

    }

    @Override
    public void useNetworkData(EntityPlayer entityPlayer, Side side, byte b, NBTTagCompound nbtTagCompound, @Nonnull ItemStack itemStack) {

    }
}
