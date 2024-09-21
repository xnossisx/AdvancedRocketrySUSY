package zmaster587.advancedRocketry.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import io.netty.buffer.ByteBuf;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.libVulpes.network.BasePacket;

public class PacketSatellitesUpdate extends BasePacket {

    private int dimNumber;
    private DimensionProperties dimProperties;

    public PacketSatellitesUpdate() {}

    public PacketSatellitesUpdate(int dimNumber, DimensionProperties dimProperties) {
        this.dimProperties = dimProperties;
        this.dimNumber = dimNumber;
    }

    @Override
    public void write(final ByteBuf byteBuf) {
        byteBuf.writeInt(this.dimNumber);

        NBTTagCompound compound = new NBTTagCompound();
        for (SatelliteBase satellite : this.dimProperties.getTickingSatellites()) {
            NBTTagCompound satTag = new NBTTagCompound();
            satellite.writeToNBT(satTag);
            compound.setTag(String.valueOf(satellite.getId()), satTag);
        }
        ByteBufUtils.writeTag(byteBuf, compound);
    }

    @Override
    public void readClient(final ByteBuf byteBuf) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            System.out.println("readclient was called on server side (this should never happen) - returning");
            return;
        }

        // System.out.println("readclient was called on client");
        int dimNumber = byteBuf.readInt();

        NBTTagCompound compound = ByteBufUtils.readTag(byteBuf);

        DimensionProperties prop = DimensionManager.getInstance().getDimensionProperties(dimNumber);

        for (String key : compound.getKeySet()) {
            prop.getSatellite(Long.parseLong(key)).readFromNBT(compound.getCompoundTag(key));
        }
    }

    @Override
    public void read(final ByteBuf byteBuf) {}

    @Override
    public void executeClient(final EntityPlayer entityPlayer) {}

    @Override
    public void executeServer(final EntityPlayerMP entityPlayerMP) {}
}
