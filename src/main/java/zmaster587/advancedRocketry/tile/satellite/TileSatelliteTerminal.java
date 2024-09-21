package zmaster587.advancedRocketry.tile.satellite;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.DataStorage;
import zmaster587.advancedRocketry.api.DataStorage.DataType;
import zmaster587.advancedRocketry.api.satellite.IDataHandler;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.inventory.modules.ModuleData;
import zmaster587.advancedRocketry.inventory.modules.ModuleSatellite;
import zmaster587.advancedRocketry.item.ItemData;
import zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip;
import zmaster587.advancedRocketry.satellite.SatelliteData;
import zmaster587.advancedRocketry.util.IDataInventory;
import zmaster587.advancedRocketry.util.PlanetaryTravelHelper;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumer;
import zmaster587.libVulpes.util.INetworkMachine;

public class TileSatelliteTerminal extends TileInventoriedRFConsumer implements INetworkMachine, IModularInventory,
                                   IButtonInventory, IDataInventory, IDataHandler {

    // private ModuleText satelliteText;
    private SatelliteBase satellite;
    private ModuleText moduleText;
    private DataStorage data;

    public TileSatelliteTerminal() {
        super(10000, 2);

        data = new DataStorage();
        data.setMaxData(1000);
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
        return world.getTotalWorldTime() % 16 == 0 && getSatelliteFromSlot(0) != null;
    }

    @Override
    public int getPowerPerOperation() {
        return 1;
    }

    @Override
    public void performFunction() {
        if (world.isRemote)
            updateInventoryInfo();
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte packetId) {
        if (packetId == (byte) 22) {
            satellite = getSatelliteFromSlot(0);
            if (satellite != null && satellite instanceof SatelliteData) {
                if (getUniversalEnergyStored() < getPowerPerOperation()) {
                    out.writeInt(1); // no power
                } else {
                    if (!PlanetaryTravelHelper.isTravelAnywhereInPlanetarySystem(satellite.getDimensionId(),
                            DimensionManager.getEffectiveDimId(world, pos).getId())) {
                        out.writeInt(2);// out of range
                    } else {
                        out.writeInt(3);
                        out.writeInt(((SatelliteData) satellite).getPowerPerTick());
                        out.writeInt(((SatelliteData) satellite).data.getData());
                        out.writeInt(((SatelliteData) satellite).data.getMaxData());
                    }
                }
            } else {
                out.writeInt(0); // no link
            }
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte packetId,
                                    NBTTagCompound nbt) {
        if (packetId == (byte) 22) {
            int status = in.readInt();
            if (status == 3) {
                nbt.setInteger("ppt", in.readInt());
                nbt.setInteger("data", in.readInt());
                nbt.setInteger("maxdata", in.readInt());
            }
            nbt.setInteger("status", status);
        }
    }

    @Override
    public void update() {
        super.update();

        if (!world.isRemote) {
            // update satellite for players nearby
            if ((world.getTotalWorldTime() % 20) == 0) {
                PacketHandler.sendToNearby(new PacketMachine(this, (byte) 22), world.provider.getDimension(), pos, 16);
            }
        }
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
        if (id == 0) {
            storeData(0);
        } else if (id == 100) {

            if (satellite != null && PlanetaryTravelHelper.isTravelAnywhereInPlanetarySystem(satellite.getDimensionId(),
                    DimensionManager.getEffectiveDimId(world, pos).getId())) {
                satellite.performAction(player, world, pos);
            }
        } else if (id == 101) {
            onInventoryButtonPressed(id - 100);
        }

        if (id == 22) {
            if (world.isRemote) { // 22 should never arrive at the server
                int status = nbt.getInteger("status");
                satellite = getSatelliteFromSlot(0);
                if (moduleText != null) {
                    if (status != 0 && satellite != null) {
                        if (status == 1)
                            moduleText.setText(LibVulpes.proxy.getLocalizedString("msg.notenoughpower"));

                        else if (status == 2) {
                            moduleText.setText(satellite.getName() + "\n\n" +
                                    LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.toofar"));
                        } else if (status == 3) {
                            moduleText.setText(satellite.getName() + "\n\n" +
                                    LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.info") + "\n" +
                                    "Power gen.: " + nbt.getInteger("ppt") + "\n" +
                                    "Data: " + nbt.getInteger("data") + "/" + nbt.getInteger("maxdata"));
                        }
                    } else
                        moduleText.setText(LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.nolink"));
                }
            }
        }
    }

    @Override
    public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        satellite = getSatelliteFromSlot(0);
        updateInventoryInfo();
    }

    public void updateInventoryInfo() {}

    public SatelliteBase getSatelliteFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemSatelliteIdentificationChip) {
            return ItemSatelliteIdentificationChip.getSatellite(stack);
        }

        return null;
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> modules = new LinkedList<>();
        modules.add(new ModulePower(18, 20, this.energy));
        modules.add(new ModuleButton(116, 70, 0, LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.connect"), this,
                zmaster587.libVulpes.inventory.TextureResources.buttonBuild));
        modules.add(new ModuleButton(173, 3, 1, "", this, TextureResources.buttonKill,
                LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.destroysat"), 24, 24));
        modules.add(new ModuleData(28, 20, 1, this, data));
        ModuleSatellite moduleSatellite = new ModuleSatellite(152, 10, this, 0);
        moduleSatellite.setSatellite(satellite);
        modules.add(moduleSatellite);

        // Try to assign a satellite ASAP
        // moduleSatellite.setSatellite(getSatelliteFromSlot(0));

        moduleText = new ModuleText(60, 20, LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.nolink"), 0x404040);
        modules.add(moduleText);

        updateInventoryInfo();
        return modules;
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        if (buttonId == 0) {
            PacketHandler.sendToServer(new PacketMachine(this, (byte) (100 + buttonId)));

        } else if (buttonId == 1) {
            ItemStack stack = getStackInSlot(0);

            if (!stack.isEmpty() && stack.getItem() instanceof ItemSatelliteIdentificationChip) {
                ItemSatelliteIdentificationChip idchip = (ItemSatelliteIdentificationChip) stack.getItem();

                SatelliteBase satellite = idchip.getSatellite(stack);

                // Somebody might want to erase the chip of an already existing satellite
                if (satellite != null)
                    DimensionManager.getInstance().getDimensionProperties(satellite.getDimensionId())
                            .removeSatellite(satellite.getId());

                idchip.erase(stack);
                setInventorySlotContents(0, stack);
                PacketHandler.sendToServer(new PacketMachine(this, (byte) (100 + buttonId)));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        NBTTagCompound data = new NBTTagCompound();

        this.data.writeToNBT(data);
        nbt.setTag("data", data);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        NBTTagCompound data = nbt.getCompoundTag("data");
        this.data.readFromNBT(data);
    }

    @Override
    public void loadData(int id) {}

    @Override
    public void storeData(int id) {
        if (!world.isRemote) {
            ItemStack stack = getStackInSlot(1);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemData && stack.getCount() == 1) {
                ItemData dataItem = (ItemData) stack.getItem();
                data.removeData(dataItem.addData(stack, data.getData(), data.getDataType()), true);
            }
        } else {
            PacketHandler.sendToServer(new PacketMachine(this, (byte) 0));
        }
    }

    @Override
    public int extractData(int maxAmount, DataType type, EnumFacing dir, boolean commit) {
        // TODO

        SatelliteBase satellite = getSatelliteFromSlot(0);
        if (satellite instanceof SatelliteData && PlanetaryTravelHelper.isTravelAnywhereInPlanetarySystem(
                satellite.getDimensionId(), DimensionManager.getEffectiveDimId(world, pos).getId())) {
            satellite.performAction(null, world, pos);
        }

        if (type == data.getDataType() || data.getDataType() == DataType.UNDEFINED) {
            return data.removeData(maxAmount, commit);
        }

        return 0;
    }

    @Override
    public int addData(int maxAmount, DataType type, EnumFacing dir, boolean commit) {
        return data.addData(maxAmount, type, commit);
    }

    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }
}
