package zmaster587.advancedRocketry.tile.satellite;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.DataStorage;
import zmaster587.advancedRocketry.api.DataStorage.DataType;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.inventory.modules.ModuleData;
import zmaster587.advancedRocketry.inventory.modules.ModuleSatellite;
import zmaster587.advancedRocketry.item.ItemBiomeChanger;
import zmaster587.advancedRocketry.item.ItemData;
import zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip;
import zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger;
import zmaster587.advancedRocketry.satellite.SatelliteData;
import zmaster587.advancedRocketry.util.IDataInventory;
import zmaster587.advancedRocketry.util.PlanetaryTravelHelper;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.inventory.TextureResources;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumer;
import zmaster587.libVulpes.util.INetworkMachine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;



public class TileTerraformingTerminal extends TileInventoriedRFConsumer implements INetworkMachine, IModularInventory, IButtonInventory {



    private ModuleText moduleText;

    public boolean was_enabled_last_tick;


    private ModuleButton buttonstopall;


    public TileTerraformingTerminal() {
        super(1, 1);

        buttonstopall = new ModuleButton(40, 60, 1, "stop all",this, TextureResources.buttonSquare,
                "- emergency stop all terminals -\n" +
                        "When resetting your satellites\n" +
                        "you need to turn all terminals off\n" +
                        "before you start turning them on\n" +
                        "again or they will interfere with\n" +
                        "each other\n\n" +
                        "recommended to use only\n" +
                        "in emergency situations");

        was_enabled_last_tick = false;

    }

    @Override
    @Nonnull
    public int[] getSlotsForFace(@Nullable EnumFacing side) {
        return new int[0];
    }

    @Override
    public String getModularInventoryName() {
        return AdvancedRocketryBlocks.blockSatelliteControlCenter.getLocalizedName();
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPerformFunction() {
        return true;
    }

    @Override
    public int getPowerPerOperation() {
        return 0;
    }

    @Override
    public void performFunction() {
        if (world.isRemote)
            updateInventoryInfo();
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte packetId) {
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte packetId,
                                    NBTTagCompound nbt) {

    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
            if (id == 1){
                //unregister many satellites
                for (int i = 0; i < 1000; i++) {
                    DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).unregister_terraforming_satellite();
                }
                this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 2);
                this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 1);
            }
    }

    @Override
    public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        updateInventoryInfo();
        this.openInventory(Minecraft.getMinecraft().player);
    }



    @Override
    public void update() {
        if (world.isRemote){
            if (world.getTotalWorldTime()%20 == 0)
                updateInventoryInfo();
        }
        super.update();
        boolean has_redstone = world.isBlockIndirectlyGettingPowered(getPos()) != 0;
        if (!world.isRemote) {
            if (hasValidBiomeChanger() && has_redstone) {
                if (!was_enabled_last_tick) {
                    was_enabled_last_tick = true;
                    DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getAverageTemp();
                    DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).setTerraformedBiomes(DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getViableBiomes());
                    ((WorldProviderPlanet) net.minecraftforge.common.DimensionManager.getProvider(world.provider.getDimension())).chunkMgrTerraformed = new ChunkManagerPlanet(world, world.getWorldInfo().getGeneratorOptions(), DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getTerraformedBiomes());
                    DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).register_terraforming_satellite();
                    this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 2);
                    this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 1);
                    this.markDirty();
                }
            }
            else {
                if (was_enabled_last_tick) {
                    was_enabled_last_tick = false;
                    DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).unregister_terraforming_satellite();
                    this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 2);
                    this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 1);
                    this.markDirty();
                }
            }
        }
    }
    public void updateInventoryInfo() {
        if (moduleText != null) {
            if (hasValidBiomeChanger() && world.isBlockIndirectlyGettingPowered(getPos()) != 0){
                moduleText.setText("terraforming planet...");
            }else if (hasValidBiomeChanger()){
                moduleText.setText("provide redstone signal\nto start the process");
            }
            else{
                moduleText.setText("place a biome remote here\nto make the satellite terraform\nthe entire planet");
            }
            int num_regs = DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getNum_terraforming_satellites_registered();
            moduleText.setText(moduleText.getText()+"\nTotal satellites working: "+num_regs);

        }
    }

    public boolean hasValidBiomeChanger() {
        ItemStack biomeChanger = getStackInSlot(0);
        SatelliteBase satellite;

        return !biomeChanger.isEmpty() &&
                (biomeChanger.getItem() instanceof ItemBiomeChanger) &&
                SatelliteRegistry.getSatellite(biomeChanger) != null &&
                (satellite = ((ItemSatelliteIdentificationChip) AdvancedRocketryItems.itemBiomeChanger).getSatellite(biomeChanger)).getDimensionId() == world.provider.getDimension() &&
                satellite instanceof SatelliteBiomeChanger;
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {


        List<ModuleBase> modules = new LinkedList<>();
        modules.add(buttonstopall);
        ModuleSatellite moduleSatellite = new ModuleSatellite(152, 10, this, 0);
        modules.add(moduleSatellite);

        moduleText = new ModuleText(5, 20, LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.nolink"), 0x404040);
        modules.add(moduleText);

        updateInventoryInfo();
        return modules;
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
            PacketHandler.sendToServer(new PacketMachine(this, (byte) (buttonId)));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("was_enabled_last_tick", was_enabled_last_tick);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        was_enabled_last_tick = nbt.getBoolean("was_enabled_last_tick");
    }



    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }
}
