package zmaster587.advancedRocketry.tile;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import gregtech.api.pattern.BlockWorldState;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import ibxm.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.api.RocketEvent.RocketLandedEvent;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;
import zmaster587.advancedRocketry.block.*;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.advancedRocketry.item.ItemPackedStructure;
import zmaster587.advancedRocketry.network.PacketInvalidLocationNotify;
import zmaster587.advancedRocketry.tile.hatch.TileSatelliteHatch;
import zmaster587.advancedRocketry.util.StorageChunk;
import zmaster587.advancedRocketry.util.WeightEngine;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.client.util.ProgressBarImage;
import zmaster587.libVulpes.interfaces.ILinkableTile;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.items.ItemLinker;
import zmaster587.libVulpes.network.PacketEntity;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.IMultiblock;
import zmaster587.libVulpes.tile.TileEntityRFConsumer;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.IconResource;
import zmaster587.libVulpes.util.ZUtils;

/**
 * Purpose: validate the rocket structure as well as give feedback to the player as to what needs to be
 * changed to complete the rocket structure
 * Also will be used to "build" the rocket components from the placed frames, control fuel flow etc
 **/
public class TileRocketAssemblingMachine extends TileEntityRFConsumer implements IButtonInventory, INetworkMachine,
                                         IDataSync, IModularInventory, IProgressBar, ILinkableTile {

    protected static final ResourceLocation backdrop = new ResourceLocation("advancedrocketry",
            "textures/gui/rocketBuilder.png");
    protected static final ProgressBarImage verticalProgressBar = new ProgressBarImage(76, 93, 8, 52, 176, 15, 2, 38, 3,
            2, EnumFacing.UP, backdrop);
    private final static int MAXSCANDELAY = 10;
    private final static int ENERGYFOROP = 100;
    private final static int MAX_SIZE = 16;
    private final static int MAX_SIZE_Y = 64;
    private final static int MIN_SIZE = 3;
    private final static int MIN_SIZE_Y = 4;
    private static final ProgressBarImage horizontalProgressBar = new ProgressBarImage(89, 9, 81, 17, 176, 0, 80, 15, 0,
            2, EnumFacing.EAST, backdrop);
    private static final Block[] viableBlocks = { AdvancedRocketryBlocks.blockLaunchpad,
            AdvancedRocketryBlocks.blockLandingPad };
    protected ModuleText errorText;
    protected StatsRocket stats;
    protected AxisAlignedBB bbCache;
    protected ErrorCodes status;
    private ModuleText thrustText, weightText, fuelText, accelerationText;
    private int totalProgress;
    private int progress; // How long until scan is finished from 0 -> num blocks
    private int prevProgress; // Used for client/server sync
    private boolean building; // True is rocket is being built, false if only scanning or otherwise
    private int lastRocketID;
    private List<HashedBlockPosition> blockPos;

    public TileRocketAssemblingMachine() {
        super(100000);

        blockPos = new LinkedList<>();

        status = ErrorCodes.UNSCANNED;
        stats = new StatsRocket();
        building = false;
        prevProgress = 0;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        MinecraftForge.EVENT_BUS.unregister(this);
        for (HashedBlockPosition pos : blockPos) {
            TileEntity tile = world.getTileEntity(pos.getBlockPos());

            if (tile instanceof IMultiblock)
                ((IMultiblock) tile).setIncomplete();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    public ErrorCodes getStatus() {
        return status;
    }

    public void setStatus(int value) {
        status = ErrorCodes.values()[value];
    }

    public StatsRocket getRocketStats() {
        return stats;
    }

    public AxisAlignedBB getBBCache() {
        return bbCache;
    }

    public int getTotalProgress() {
        return totalProgress;
    }

    public void setTotalProgress(int scanTotalBlocks) {
        this.totalProgress = scanTotalBlocks;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int scanTime) {
        this.progress = scanTime;
    }

    public double getNormallizedProgress() {
        return progress / (double) (totalProgress * MAXSCANDELAY);
    }

    public float getAcceleration(float gravitationalMultiplier) {
        return stats.getAcceleration(gravitationalMultiplier);
    }

    public float getWeight() {
        return stats.getWeight();
    }

    public int getThrust() {
        return stats.getThrust();
    }

    public float getNeededThrust() {
        return getWeight();
    }

    public boolean hasEnoughFuel(@Nonnull FuelType fuelType) {
        // return getAcceleration(getGravityMultiplier()) > 0 ? 2 * stats.getBaseFuelRate(fuelType) * MathHelper.sqrt((2
        // * (ARConfiguration.getCurrentConfig().orbit - this.getPos().getY())) /
        // getAcceleration(getGravityMultiplier())) : 0;
        float a = getAcceleration(getGravityMultiplier());
        float fueltime = (float) stats.getFuelCapacity(fuelType) / stats.getBaseFuelRate(fuelType);
        float s_can = a / 2f * fueltime * fueltime;
        float target_s = 1 * ARConfiguration.getCurrentConfig().orbit - this.getPos().getY(); // for way back *2
        return s_can > target_s;
    }

    public float getGravityMultiplier() {
        return DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension())
                .getGravitationalMultiplier();
    }

    public int getFuel(@Nullable FuelType fuelType) {
        return (int) (stats.getFuelCapacity(fuelType) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
    }

    public boolean isBuilding() {
        return building;
    }

    public void setBuilding(boolean building) {
        this.building = building;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Override
    public int getPowerPerOperation() {
        return ENERGYFOROP;
    }

    @Override
    public void performFunction() {
        if (progress >= (totalProgress * MAXSCANDELAY)) {
            if (!world.isRemote) {
                if (building)
                    assembleRocket();
                else
                    scanRocket(world, pos, bbCache);
            }
            totalProgress = -1;
            progress = 0;
            prevProgress = 0;
            building = false; // Done building

            // TODO call function instead
            if (thrustText != null)
                updateText();

        }

        progress++;

        if (!this.world.isRemote && this.energy.getUniversalEnergyStored() < getPowerPerOperation() &&
                progress - prevProgress > 0) {
            prevProgress = progress;
            PacketHandler.sendToNearby(new PacketMachine(this, (byte) 2), this.world.provider.getDimension(),
                    this.getPos(), 32);
        }
    }

    @Override
    public boolean canPerformFunction() {
        return isScanning();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (isScanning() && bbCache != null) {
            return bbCache;
        }
        return super.getRenderBoundingBox();
    }

    public boolean isScanning() {
        return totalProgress > 0;
    }

    public AxisAlignedBB scanRocket(World world, BlockPos pos2, AxisAlignedBB bb) {
        // if already a rocket exists, output their stats

        if (getBBCache() == null) {
            bbCache = getRocketPadBounds(world, pos);
        }

        if (getBBCache() != null) {
            double buffer = 0.0001;
            AxisAlignedBB bufferedBB = bbCache.grow(buffer, buffer, buffer);
            List<EntityRocket> rockets = world.getEntitiesWithinAABB(EntityRocket.class, bufferedBB);
            if (rockets.size() == 1) { // only if axactly one rocket is here
                rockets.get(0).recalculateStats();
                this.stats = rockets.get(0).stats;
                status = ErrorCodes.ALREADY_ASSEMBLED; // to prevent assembly
                return null;
            }
        }

        int thrustMonopropellant = 0;
        int thrustBipropellant = 0;
        int thrustNuclearNozzleLimit = 0;
        int thrustNuclearReactorLimit = 0;
        int thrustNuclearTotalLimit = 0;
        int monopropellantfuelUse = 0;
        int bipropellantfuelUse = 0;
        int nuclearWorkingFluidUseMax = 0;
        int fuelCapacityMonopropellant = 0;
        int fuelCapacityBipropellant = 0;
        int fuelCapacityOxidizer = 0;
        int fuelCapacityNuclearWorkingFluid = 0;

        float drillPower = 0f;
        stats.reset();

        int actualMinX = (int) bb.maxX,
                actualMinY = (int) bb.maxY,
                actualMinZ = (int) bb.maxZ,
                actualMaxX = (int) bb.minX,
                actualMaxY = (int) bb.minY,
                actualMaxZ = (int) bb.minZ;

        boolean hasGuidance = false;
        BlockPos corePos = null;
        boolean invalidBlock = false;
        int blockCount = 0;
        for (int xCurr = (int) bb.minX; xCurr <= bb.maxX; xCurr++) {
            for (int zCurr = (int) bb.minZ; zCurr <= bb.maxZ; zCurr++) {
                for (int yCurr = (int) bb.minY; yCurr <= bb.maxY; yCurr++) {

                    BlockPos currBlockPos = new BlockPos(xCurr, yCurr, zCurr);

                    if (!world.isAirBlock(currBlockPos)) {
                        if (xCurr < actualMinX) {
                            actualMinX = xCurr;
                        }
                        if (xCurr > actualMaxX) {
                            actualMaxX = xCurr;
                        }
                        if (yCurr < actualMinY) {
                            actualMinY = yCurr;
                        }
                        if (yCurr > actualMaxY) {
                            actualMaxY = yCurr;
                        }
                        if (zCurr < actualMinZ) {
                            actualMinZ = zCurr;
                        }
                        if (zCurr > actualMaxZ) {
                            actualMaxZ = zCurr;
                        }
                        blockCount++;
                        TileEntity tile = world.getTileEntity(currBlockPos);
                        if (tile instanceof TileGuidanceComputer) {
                            if (!hasGuidance) {
                                hasGuidance = true;
                                corePos = currBlockPos;
                            } else {
                                invalidBlock = true;
                            }
                        }
                    }
                }
            }
        }
        /* may or may not require this
        if (!hasGuidance) {
            status = ErrorCodes.NOGUIDANCE;
            return null;
        }*/
        AxisAlignedBB realAABB = new AxisAlignedBB(actualMinX, actualMinY, actualMinZ, actualMaxX, actualMaxY, actualMaxZ);
        Set<BlockPos> actualBlocks = getBlockSet(world, realAABB, corePos);
        if (actualBlocks.size() < blockCount) {
            status = ErrorCodes.DISCONNECTED_STRUCT;
            return null;
        }

        Set<BlockPos> hull  = checkHull(world, realAABB, actualBlocks);

        if (hull == null) {
            return null;
        }

        boolean hasSatellite = false;
        float weight = 0;

        if (verifyScan(bb, world)) {
            for (BlockPos currBlockPos : actualBlocks) {
                int xCurr = currBlockPos.getX();
                int yCurr = currBlockPos.getY();
                int zCurr = currBlockPos.getZ();
                BlockPos abovePos = new BlockPos(xCurr, yCurr + 1, zCurr);
                BlockPos belowPos = new BlockPos(xCurr, yCurr - 1, zCurr);

                IBlockState state = world.getBlockState(currBlockPos);
                Block block = state.getBlock();

                if (ARConfiguration.getCurrentConfig().blackListRocketBlocks.contains(block)) {
                    if (!block.isReplaceable(world, currBlockPos)) {
                        invalidBlock = true;
                        if (!world.isRemote)
                            PacketHandler.sendToNearby(
                                    new PacketInvalidLocationNotify(
                                            new HashedBlockPosition(xCurr, yCurr, zCurr)),
                                    world.provider.getDimension(), getPos(), 64);
                    }
                    continue;
                }

                if (ARConfiguration.getCurrentConfig().advancedWeightSystem) {
                    weight += WeightEngine.INSTANCE.getWeight(world, currBlockPos);
                } else {
                    weight += 1;
                }

                // If rocketEngine increaseThrust
                final float x = xCurr - actualMinX - ((actualMaxX - actualMinX) / 2f);
                final float z = zCurr - actualMinZ - ((actualMaxZ - actualMinZ) / 2f);
                if (block instanceof IRocketEngine && (world.isAirBlock(belowPos) ||
                        world.getBlockState(belowPos).getBlock() instanceof BlockLandingPad ||
                        world.getBlockState(belowPos).getBlock() ==
                                AdvancedRocketryBlocks.blockLaunchpad)) {
                    if (block instanceof BlockNuclearRocketMotor) {
                        nuclearWorkingFluidUseMax += ((IRocketEngine) block).getFuelConsumptionRate(world,
                                xCurr, yCurr, zCurr);
                        thrustNuclearNozzleLimit += ((IRocketEngine) block).getThrust(world, currBlockPos);
                    } else if (block instanceof BlockBipropellantRocketMotor) {
                        bipropellantfuelUse += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr,
                                yCurr, zCurr);
                        thrustBipropellant += ((IRocketEngine) block).getThrust(world, currBlockPos);
                    } else if (block instanceof BlockRocketMotor) {
                        monopropellantfuelUse += ((IRocketEngine) block).getFuelConsumptionRate(world,
                                xCurr, yCurr, zCurr);
                        thrustMonopropellant += ((IRocketEngine) block).getThrust(world, currBlockPos);
                    }
                    stats.addEngineLocation(x + 0.5f, yCurr - actualMinY + 0.5f, z + 0.5f);
                }

                if (block instanceof IFuelTank) {
                    if (block instanceof BlockBipropellantFuelTank) {
                        fuelCapacityBipropellant += (((IFuelTank) block).getMaxFill(world, currBlockPos,
                                state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                    } else if (block instanceof BlockOxidizerFuelTank) {
                        fuelCapacityOxidizer += (((IFuelTank) block).getMaxFill(world, currBlockPos,
                                state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                    } else if (block instanceof BlockNuclearFuelTank) {
                        fuelCapacityNuclearWorkingFluid += (((IFuelTank) block).getMaxFill(world,
                                currBlockPos, state) *
                                ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                    } else if (block instanceof BlockFuelTank) {
                        fuelCapacityMonopropellant += (((IFuelTank) block).getMaxFill(world, currBlockPos,
                                state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                    }
                }

                if (block instanceof IRocketNuclearCore &&
                        ((world.getBlockState(belowPos).getBlock() instanceof IRocketNuclearCore) ||
                                (world.getBlockState(belowPos).getBlock() instanceof IRocketEngine))) {
                    thrustNuclearReactorLimit += ((IRocketNuclearCore) block).getMaxThrust(world,
                            currBlockPos);
                }

                if (block instanceof BlockSeat &&
                        world.getBlockState(abovePos).getBlock().isPassable(world, abovePos)) {
                    stats.addPassengerSeat((int) Math.floor(x), yCurr - actualMinY, (int) Math.floor(z));
                }

                if (block instanceof IMiningDrill) {
                    drillPower += ((IMiningDrill) block).getMiningSpeed(world, currBlockPos);
                }

                TileEntity tile = world.getTileEntity(currBlockPos);
                if (tile instanceof TileSatelliteHatch) {
                    hasSatellite = true;
                    if (ARConfiguration.getCurrentConfig().advancedWeightSystem) {
                        TileSatelliteHatch hatch = (TileSatelliteHatch) tile;
                        if (hatch.getSatellite() != null) {
                            weight += hatch.getSatellite().getProperties().getWeight();
                        } else if (hatch.getStackInSlot(0).getItem() instanceof ItemPackedStructure) {
                            ItemPackedStructure struct = (ItemPackedStructure) hatch.getStackInSlot(0)
                                    .getItem();
                            weight += struct.getStructure(hatch.getStackInSlot(0)).getWeight();
                        }
                    }
                    }
            }

            int nuclearWorkingFluidUse = 0;
            if (thrustNuclearNozzleLimit > 0) {
                // Only run the number of engines our cores can support - we can't throttle these effectively because
                // they're small, so they shut off if they don't get full power
                thrustNuclearTotalLimit = Math.min(thrustNuclearNozzleLimit, thrustNuclearReactorLimit);
                nuclearWorkingFluidUse = (int) (nuclearWorkingFluidUseMax *
                        (thrustNuclearTotalLimit / (float) thrustNuclearNozzleLimit));
                thrustNuclearTotalLimit = (nuclearWorkingFluidUse * thrustNuclearNozzleLimit) /
                        nuclearWorkingFluidUseMax;
            }

            // Set fuel stats
            // Thrust depending on rocket type
            stats.setBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT, monopropellantfuelUse);
            stats.setBaseFuelRate(FuelType.LIQUID_BIPROPELLANT, bipropellantfuelUse);
            stats.setBaseFuelRate(FuelType.LIQUID_OXIDIZER, bipropellantfuelUse);
            stats.setBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID, nuclearWorkingFluidUse);
            // Fuel storage depending on rocket type
            stats.setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT, fuelCapacityMonopropellant);
            stats.setFuelCapacity(FuelType.LIQUID_BIPROPELLANT, fuelCapacityBipropellant);
            stats.setFuelCapacity(FuelType.LIQUID_OXIDIZER, fuelCapacityOxidizer);
            stats.setFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID, fuelCapacityNuclearWorkingFluid);

            // Non-fuel stats
            stats.setWeight(weight);
            stats.setThrust(Math.max(Math.max(thrustMonopropellant, thrustBipropellant), thrustNuclearTotalLimit));
            stats.setDrillingPower(drillPower);

            // Total stats, used to check if the user has tried to apply two or more types of thrust/fuel
            int totalFuel = fuelCapacityBipropellant + fuelCapacityNuclearWorkingFluid + fuelCapacityMonopropellant;
            int totalFuelUse = bipropellantfuelUse + nuclearWorkingFluidUse + monopropellantfuelUse;
            // System.out.println("rocket fuel use:"+totalFuelUse);

            // Set status
            if (invalidBlock)
                status = ErrorCodes.INVALIDBLOCK;
            else if (((fuelCapacityBipropellant > 0 && totalFuel > fuelCapacityBipropellant) ||
                    (fuelCapacityMonopropellant > 0 && totalFuel > fuelCapacityMonopropellant) ||
                    (fuelCapacityNuclearWorkingFluid > 0 && totalFuel > fuelCapacityNuclearWorkingFluid)) ||
                    ((thrustBipropellant > 0 && totalFuelUse > bipropellantfuelUse) ||
                            (thrustMonopropellant > 0 && totalFuelUse > monopropellantfuelUse) ||
                            (thrustNuclearTotalLimit > 0 && totalFuelUse > nuclearWorkingFluidUse)))
                status = ErrorCodes.COMBINEDTHRUST;
            else if (!hasGuidance && !hasSatellite)
                status = ErrorCodes.NOGUIDANCE;
            else if (getThrust() <= getNeededThrust())
                status = ErrorCodes.NOENGINES;
            else if (((thrustBipropellant > 0) && !hasEnoughFuel(FuelType.LIQUID_BIPROPELLANT)) ||
                    ((thrustMonopropellant > 0) && !hasEnoughFuel(FuelType.LIQUID_MONOPROPELLANT)) ||
                    ((thrustNuclearTotalLimit > 0) && !hasEnoughFuel(FuelType.NUCLEAR_WORKING_FLUID)))
                status = ErrorCodes.NOFUEL;
            else
                status = ErrorCodes.SUCCESS;
        }

        return new AxisAlignedBB(actualMinX, actualMinY, actualMinZ, actualMaxX, actualMaxY, actualMaxZ);
    }

    private Set<BlockPos> checkHull(World world, AxisAlignedBB aaBB, Set<BlockPos> actualBlocks) {
        AxisAlignedBB floodBB = aaBB.grow(1);// initializes flood fill box
        BlockPos bottom = new BlockPos(floodBB.minX, floodBB.minY, floodBB.minZ); // initializes flood fill start
        Queue<BlockPos> uncheckedBlocks = new ArrayDeque<>();
        Set<BlockPos> airBlocks = new HashSet<>();
        Set<BlockPos> hullBlocks = new HashSet<>();
        PatternMatchContext pmc = new PatternMatchContext();
        uncheckedBlocks.add(bottom);
        for (BlockPos pos; !uncheckedBlocks.isEmpty();) {
            pos = uncheckedBlocks.remove();
            if (actualBlocks.contains(pos)) {
                BlockWorldState bws = new BlockWorldState(); // this is awful but I guess it works?
                bws.update(world, pos,pmc,null,null,ARConfiguration.getCurrentConfig().rocketHullBlocks);
                if (!ARConfiguration.getCurrentConfig().rocketHullBlocks.test(bws)) {
                    status = ErrorCodes.HULL_IMPROPER;
                    return null;
                }
                hullBlocks.add(pos);
            } else {
                airBlocks.add(pos);
                uncheckedBlocks.addAll(getBlockDirNeighbors(world, floodBB, pos).stream().filter(
                                p -> actualContains(floodBB, p) && !(airBlocks.contains(p) || uncheckedBlocks.contains(p)))
                        .collect(Collectors.toSet()));
            }
        }
        long volume = Math.round((floodBB.maxX - floodBB.minX + 1)) * Math.round((floodBB.maxY - floodBB.minY + 1)) * Math.round((floodBB.maxZ - floodBB.minZ + 1));
        long remainingAir = volume - airBlocks.size() - actualBlocks.size(); // the .grow() is factored in with airBlocks.size()
        if (remainingAir < 2) { // considering you need a seat and an air block above it
            status = ErrorCodes.HULL_FULL;
            return null;
        }
        return hullBlocks;
    }

    private Set<BlockPos> getBlockSet(World world, AxisAlignedBB aaBB, BlockPos beg) {
        if (!aaBB.contains(new Vec3d(beg))) {
            AdvancedRocketry.logger.debug("Guidance computer somehow not inside of rocket bounding box...likely a bug. Contact Epix7");
            return new HashSet<BlockPos>(); //wtf moment
        }
        Set<BlockPos> blocksCollected = new HashSet<BlockPos>();
        blocksCollected.add(beg);
        Queue<BlockPos> uncheckedBlocks = new ArrayDeque<>(Arrays.asList(beg));


        while (!uncheckedBlocks.isEmpty()) {
            BlockPos bp = uncheckedBlocks.remove();
            blocksCollected.add(bp);
            uncheckedBlocks.addAll(getBlockNeighbors(world, aaBB, bp).stream()
                    .filter(p -> !world.isAirBlock(p) && !blocksCollected.contains(p) && !uncheckedBlocks.contains(p)).collect(Collectors.toSet()));
        }
        return blocksCollected;
    }

    private boolean actualContains(AxisAlignedBB aaBB, BlockPos bp) {
        return aaBB.grow(1).contains(new Vec3d(bp));
    }

    private static Vec3i neighborVecs[] = new Vec3i[] {new Vec3i(1,0,0), new Vec3i(0,1,0), new Vec3i(0, 0, 1)};
    // only accepting those in the bounding box
    private ArrayList<BlockPos> getBlockDirNeighbors(World world, AxisAlignedBB aaBB, BlockPos beg) {
        ArrayList<BlockPos> neighbors = new ArrayList<>();
        for (int dir = -1; dir < 2; dir += 2) {
            for (int i = 0; i < 3; i++) {
                BlockPos newPos = beg.add(new Vec3i(neighborVecs[i].getX()*dir, neighborVecs[i].getY()*dir, neighborVecs[i].getZ()*dir));
                if (actualContains(aaBB, newPos))
                    neighbors.add(newPos);
            }
        }
        return neighbors;
    }

    private ArrayList<BlockPos> getBlockNeighbors(World world, AxisAlignedBB aaBB, BlockPos beg) {
        ArrayList<BlockPos> neighbors = new ArrayList<>();
        for (int dirX = -1; dirX < 2; dirX++) {
            for (int dirY = -1; dirY < 2; dirY++) {
                for (int dirZ = -1; dirZ < 2; dirZ++) {
                    if (!(dirX == 0 && dirY == 0 && dirZ == 0)) {
                        BlockPos newPos = beg.add(new Vec3i(dirX, dirY, dirZ));
                        if (actualContains(aaBB, newPos))
                            neighbors.add(newPos);
                    }
                }
            }
        }
        return neighbors;
    }

    private void removeReplaceableBlocks(AxisAlignedBB bb) {
        for (int yCurr = (int) bb.minY; yCurr <= bb.maxY; yCurr++) {
            for (int xCurr = (int) bb.minX; xCurr <= bb.maxX; xCurr++) {
                for (int zCurr = (int) bb.minZ; zCurr <= bb.maxZ; zCurr++) {

                    BlockPos currBlockPos = new BlockPos(xCurr, yCurr, zCurr);

                    if (!world.isAirBlock(currBlockPos)) {
                        IBlockState state = world.getBlockState(currBlockPos);
                        Block block = state.getBlock();
                        if (ARConfiguration.getCurrentConfig().blackListRocketBlocks.contains(block) &&
                                block.isReplaceable(world, currBlockPos)) {
                            if (!world.isRemote)
                                world.setBlockToAir(currBlockPos);
                        }
                    }
                }
            }
        }
    }

    public void assembleRocket() {
        if (bbCache == null || world.isRemote)
            return;
        // Need to scan again b/c something may have changed
        AxisAlignedBB rocketBB = scanRocket(world, pos, bbCache);

        if (status != ErrorCodes.SUCCESS)
            return;

        // Remove replacable blocks that don't belong on the rocket
        removeReplaceableBlocks(bbCache);

        StorageChunk storageChunk;
        try {
            storageChunk = StorageChunk.cutWorldBB(world, bbCache);
        } catch (NegativeArraySizeException e) {
            return;
        }

        EntityRocket rocket = new EntityRocket(world, storageChunk, stats.copy(),
                rocketBB.minX + (rocketBB.maxX - rocketBB.minX) / 2f + .5f,
                this.getPos().getY(),
                rocketBB.minZ + (rocketBB.maxZ - rocketBB.minZ) / 2f + .5f);

        world.spawnEntity(rocket);
        NBTTagCompound nbtdata = new NBTTagCompound();

        rocket.writeToNBT(nbtdata);
        PacketHandler.sendToNearby(new PacketEntity(rocket, (byte) 0, nbtdata), rocket.world.provider.getDimension(),
                this.pos, 64);

        stats.reset();
        this.status = ErrorCodes.FINISHED;
        this.markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        for (IInfrastructure infrastructure : getConnectedInfrastructure()) {
            rocket.linkInfrastructure(infrastructure);
        }
    }

    /**
     * Does not make sure the structure is complete, only gets max bounds!
     *
     * @param world the world
     * @param pos   coords to evaluate from
     * @return AxisAlignedBB bounds of structure if valid otherwise null
     */
    public AxisAlignedBB getRocketPadBounds(World world, BlockPos pos) {
        EnumFacing direction = RotatableBlock.getFront(world.getBlockState(pos)).getOpposite();
        int xMin, zMin, xMax, zMax;
        int yCurrent = pos.getY() - 1;
        int xCurrent = pos.getX() + direction.getXOffset();
        int zCurrent = pos.getZ() + direction.getZOffset();
        xMax = xMin = xCurrent;
        zMax = zMin = zCurrent;
        int xSize, zSize;

        BlockPos currPos = new BlockPos(xCurrent, yCurrent, zCurrent);

        if (world.isRemote)
            return null;

        // Get min and maximum Z/X bounds
        if (direction.getXOffset() != 0) {
            xSize = ZUtils.getContinuousBlockLength(world, direction, currPos, MAX_SIZE, viableBlocks);
            zMin = ZUtils.getContinuousBlockLength(world, EnumFacing.NORTH, currPos, MAX_SIZE, viableBlocks);
            zMax = ZUtils.getContinuousBlockLength(world, EnumFacing.SOUTH, currPos.add(0, 0, 1), MAX_SIZE - zMin,
                    viableBlocks);
            zSize = zMin + zMax;

            zMin = zCurrent - zMin + 1;
            zMax = zCurrent + zMax;

            if (direction.getXOffset() > 0) {
                xMax = xCurrent + xSize - 1;
            }

            if (direction.getXOffset() < 0) {
                xMin = xCurrent - xSize + 1;
            }
        } else {
            zSize = ZUtils.getContinuousBlockLength(world, direction, currPos, MAX_SIZE, viableBlocks);
            xMin = ZUtils.getContinuousBlockLength(world, EnumFacing.WEST, currPos, MAX_SIZE, viableBlocks);
            xMax = ZUtils.getContinuousBlockLength(world, EnumFacing.EAST, currPos.add(1, 0, 0), MAX_SIZE - xMin,
                    viableBlocks);
            xSize = xMin + xMax;

            xMin = xCurrent - xMin + 1;
            xMax = xCurrent + xMax;

            if (direction.getZOffset() > 0) {
                zMax = zCurrent + zSize - 1;
            }

            if (direction.getZOffset() < 0) {
                zMin = zCurrent - zSize + 1;
            }
        }

        int maxTowerSize = 0;
        // Check perimeter for structureBlocks and get the size
        for (int i = xMin; i <= xMax; i++) {
            if (world.getBlockState(new BlockPos(i, yCurrent, zMin - 1)).getBlock() ==
                    AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP,
                        new BlockPos(i, yCurrent, zMin - 1), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }

            if (world.getBlockState(new BlockPos(i, yCurrent, zMax + 1)).getBlock() ==
                    AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP,
                        new BlockPos(i, yCurrent, zMax + 1), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }
        }

        for (int i = zMin; i <= zMax; i++) {
            if (world.getBlockState(new BlockPos(xMin - 1, yCurrent, i)).getBlock() ==
                    AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP,
                        new BlockPos(xMin - 1, yCurrent, i), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }

            if (world.getBlockState(new BlockPos(xMax + 1, yCurrent, i)).getBlock() ==
                    AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP,
                        new BlockPos(xMax + 1, yCurrent, i), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }
        }

        // if tower does not meet criteria then reutrn null
        if (maxTowerSize < MIN_SIZE_Y || xSize < MIN_SIZE || zSize < MIN_SIZE) {
            return null;
        }

        return new AxisAlignedBB(new BlockPos(xMin, yCurrent + 1, zMin),
                new BlockPos(xMax, yCurrent + maxTowerSize - 1, zMax));
    }

    protected boolean verifyScan(AxisAlignedBB bb, World world) {
        boolean whole = true;

        boundLoop:
        for (int xx = (int) bb.minX; xx <= (int) bb.maxX; xx++) {
            for (int zz = (int) bb.minZ; zz <= (int) bb.maxZ; zz++) {
                Block blockAtSpot = world.getBlockState(new BlockPos(xx, (int) bb.minY - 1, zz)).getBlock();
                boolean contained = false;
                for (Block b : viableBlocks) {
                    if (blockAtSpot == b) {
                        contained = true;
                        break;
                    }
                }

                if (!contained) {
                    whole = false;
                    break boundLoop;
                }
            }
        }

        return whole;
    }

    public int getVolume(World world, AxisAlignedBB bb) {
        return (int) ((bb.maxX - bb.minX) * (bb.maxY - bb.minY) * (bb.maxZ - bb.minZ));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        stats.writeToNBT(nbt);
        nbt.setInteger("scanTime", progress);
        nbt.setInteger("scanTotalBlocks", totalProgress);
        nbt.setBoolean("building", building);
        nbt.setInteger("status", status.ordinal());

        if (bbCache != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setDouble("minX", bbCache.minX);
            tag.setDouble("minY", bbCache.minY);
            tag.setDouble("minZ", bbCache.minZ);
            tag.setDouble("maxX", bbCache.maxX);
            tag.setDouble("maxY", bbCache.maxY);
            tag.setDouble("maxZ", bbCache.maxZ);

            nbt.setTag("bb", tag);
        }

        if (!blockPos.isEmpty()) {
            int[] array = new int[blockPos.size() * 3];
            int counter = 0;
            for (HashedBlockPosition pos : blockPos) {
                array[counter] = pos.x;
                array[counter + 1] = pos.y;
                array[counter + 2] = pos.z;
                counter += 3;
            }

            nbt.setIntArray("infrastructureLocations", array);
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        stats.readFromNBT(nbt);

        prevProgress = progress = nbt.getInteger("scanTime");
        totalProgress = nbt.getInteger("scanTotalBlocks");
        status = ErrorCodes.values()[nbt.getInteger("status")];

        building = nbt.getBoolean("building");
        if (nbt.hasKey("bb")) {

            NBTTagCompound tag = nbt.getCompoundTag("bb");
            bbCache = new AxisAlignedBB(tag.getDouble("minX"),
                    tag.getDouble("minY"), tag.getDouble("minZ"),
                    tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ"));

        }

        blockPos.clear();
        if (nbt.hasKey("infrastructureLocations")) {
            int[] array = nbt.getIntArray("infrastructureLocations");

            for (int counter = 0; counter < array.length; counter += 3) {
                blockPos.add(new HashedBlockPosition(array[counter], array[counter + 1], array[counter + 2]));
            }
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        super.getUpdatePacket();
        NBTTagCompound nbt = new NBTTagCompound();

        writeToNBT(nbt);

        return new SPacketUpdateTileEntity(pos, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte id) {
        // Used to sync clinet/server
        if (id == 2) {
            out.writeInt(energy.getUniversalEnergyStored());
            out.writeInt(this.progress);
        } else if (id == 3) {
            out.writeInt(lastRocketID);
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte id,
                                    NBTTagCompound nbt) {
        if (id == 2) {
            nbt.setInteger("pwr", in.readInt());
            nbt.setInteger("tik", in.readInt());
        } else if (id == 3) {
            nbt.setInteger("id", in.readInt());
        }
    }

    public boolean canScan() {
        return bbCache != null;
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id,
                               NBTTagCompound nbt) {
        if (id == 0) {

            bbCache = getRocketPadBounds(world, pos);
            if (!canScan())
                return;

            totalProgress = (int) (ARConfiguration.getCurrentConfig().buildSpeedMultiplier *
                    this.getVolume(world, bbCache) / 10);
            this.markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        } else if (id == 1) {

            if (isScanning())
                return;

            building = true;

            bbCache = getRocketPadBounds(world, pos);
            if (!canScan())
                return;

            totalProgress = (int) (ARConfiguration.getCurrentConfig().buildSpeedMultiplier *
                    this.getVolume(world, bbCache) / 10);
            this.markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        } else if (id == 2) {
            energy.setEnergyStored(nbt.getInteger("pwr"));
            this.progress = nbt.getInteger("tik");
        } else if (id == 3) {
            EntityRocket rocket = (EntityRocket) world.getEntityByID(nbt.getInteger("id"));
            for (IInfrastructure infrastructure : getConnectedInfrastructure()) {
                rocket.linkInfrastructure(infrastructure);
            }
        }
    }

    protected void updateText() {
        thrustText.setText(isScanning() ? (LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.thrust") + ": ???") :
                String.format("%s: %dkN", LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.thrust"), getThrust()));
        weightText.setText(isScanning() ? (LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.weight") + ": ???") :
                String.format("%s: %.2fkN", LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.weight"),
                        (getWeight() * getGravityMultiplier())));
        fuelText.setText(isScanning() ? (LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.fuel") + ": ???") :
                String.format("%s: %dmb/s", LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.fuel"),
                        20 * getRocketStats().getFuelRate((stats.getFuelCapacity(FuelType.LIQUID_MONOPROPELLANT) > 0) ?
                                FuelType.LIQUID_MONOPROPELLANT :
                                (stats.getFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID) > 0) ?
                                        FuelType.NUCLEAR_WORKING_FLUID : FuelType.LIQUID_BIPROPELLANT)));
        accelerationText
                .setText(isScanning() ? (LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.acc") + ": ???") :
                        String.format("%s: %.2fm/s\u00b2", LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.acc"),
                                getAcceleration(getGravityMultiplier()) * 20f));
        if (!world.isRemote) {
            if (getRocketPadBounds(world, pos) == null)
                setStatus(ErrorCodes.INCOMPLETESTRCUTURE.ordinal());
            else if (ErrorCodes.INCOMPLETESTRCUTURE.equals(getStatus()))
                setStatus(ErrorCodes.UNSCANNED.ordinal());
        }

        errorText.setText(getStatus().getErrorCode());
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> modules = new LinkedList<>();

        modules.add(new ModulePower(160, 90, this));

        if (world.isRemote)
            modules.add(new ModuleImage(4, 9, new IconResource(4, 9, 168, 74, backdrop)));

        modules.add(new ModuleProgress(89, 47, 0, horizontalProgressBar, this));
        modules.add(new ModuleProgress(89, 66, 1, horizontalProgressBar, this));
        modules.add(new ModuleProgress(89, 28, 3, horizontalProgressBar, this));
        modules.add(new ModuleProgress(89, 9, 4, horizontalProgressBar, this));

        modules.add(new ModuleProgress(149, 90, 2, verticalProgressBar, this));

        modules.add(new ModuleButton(5, 94, 0, LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.scan"), this,
                zmaster587.libVulpes.inventory.TextureResources.buttonScan));

        ModuleButton buttonBuild;
        modules.add(
                buttonBuild = new ModuleButton(5, 120, 1, LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.build"),
                        this, zmaster587.libVulpes.inventory.TextureResources.buttonBuild));
        buttonBuild.setColor(0xFFFF2222);

        modules.add(thrustText = new ModuleText(8, 15, "", 0xFF22FF22));
        modules.add(weightText = new ModuleText(8, 34, "", 0xFF22FF22));
        modules.add(fuelText = new ModuleText(8, 52, "", 0xFF22FF22));
        modules.add(accelerationText = new ModuleText(8, 71, "", 0xFF22FF22));
        modules.add(errorText = new ModuleText(5, 84, "", 0xFFFFFF22));

        updateText();

        for (int i = 0; i < 6; i++)
            modules.add(new ModuleSync(i, this));

        return modules;
    }

    @Override
    public String getModularInventoryName() {
        return "";
    }

    @Override
    public float getNormallizedProgress(int id) {
        if (isScanning() && id != 2)
            return 0f;

        switch (id) {
            case 0:
                FuelType fuelType = (stats.getBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT) > 0) ?
                        FuelType.LIQUID_MONOPROPELLANT : (stats.getBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID) > 0) ?
                                FuelType.NUCLEAR_WORKING_FLUID : FuelType.LIQUID_BIPROPELLANT;
                return (this.getAcceleration(getGravityMultiplier()) > 0) ? MathHelper
                        .clamp(0.5f + 0.5f * ((float) (this.getFuel(fuelType) - this.stats.getFuelCapacity(fuelType)) /
                                this.stats.getFuelCapacity(fuelType)), 0f, 1f) :
                        0;
            case 1:
                return MathHelper.clamp(0.5f + this.getAcceleration(getGravityMultiplier()) * 10, 0f, 1f);
            case 2:
                return (float) this.getNormallizedProgress();
            case 3:
                return this.getWeight() > 0 ? 0.5f : 0f;
            case 4:
                return this.getThrust() > 0 ? 0.9f : 0f;
        }

        return 0f;
    }

    @Override
    public void setProgress(int id, int progress) {
        if (id == 2)
            setProgress(progress);
    }

    @Override
    public int getProgress(int id) {
        if (id == 2)
            return getProgress();
        return 0;
    }

    @Override
    public int getTotalProgress(int id) {
        if (id == 2)
            return getTotalProgress();
        return 0;
    }

    @Override
    public void setTotalProgress(int id, int progress) {
        if (id == 2) {
            setTotalProgress(progress);
            updateText();
        }
    }

    @Override
    public void setData(int id, int value) {
        switch (id) {
            case 0:
                getRocketStats().setWeight(value / 1000f);
                break;
            case 1:
                getRocketStats().setThrust(value);
                break;
            case 2:
                setStatus(value);

            case 3:
                getRocketStats().setBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT, value);
                break;
            case 4:
                getRocketStats().setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT, value);
                break;
            case 5:
                getRocketStats().setFuelRate(FuelType.LIQUID_MONOPROPELLANT, value);
                break;

            case 6:
                getRocketStats().setFuelRate(FuelType.LIQUID_BIPROPELLANT, value);
            case 7:
                getRocketStats().setFuelRate(FuelType.LIQUID_BIPROPELLANT, value);
            case 8:
                getRocketStats().setFuelRate(FuelType.LIQUID_BIPROPELLANT, value);

            case 9:
                getRocketStats().setFuelRate(FuelType.NUCLEAR_WORKING_FLUID, value);
            case 10:
                getRocketStats().setFuelRate(FuelType.NUCLEAR_WORKING_FLUID, value);
            case 11:
                getRocketStats().setFuelRate(FuelType.NUCLEAR_WORKING_FLUID, value);

        }
        updateText();
    }

    @Override
    public int getData(int id) {
        switch (id) {

            case 0:
                return (int) (getRocketStats().getWeight() * 1000);// because it is a float really so take it *1000
            case 1:
                return getRocketStats().getThrust();
            case 2:
                return getStatus().ordinal();

            // I think this is missing the other fuel types...
            case 3:
                return getRocketStats().getBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT);
            case 4:
                return getRocketStats().getFuelCapacity(FuelType.LIQUID_MONOPROPELLANT);
            case 5:
                return getRocketStats().getFuelRate(FuelType.LIQUID_MONOPROPELLANT);

            case 6:
                return getRocketStats().getBaseFuelRate(FuelType.LIQUID_BIPROPELLANT);
            case 7:
                return getRocketStats().getFuelCapacity(FuelType.LIQUID_BIPROPELLANT);
            case 8:
                return getRocketStats().getFuelRate(FuelType.LIQUID_BIPROPELLANT);

            case 9:
                return getRocketStats().getBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID);
            case 10:
                return getRocketStats().getFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID);
            case 11:
                return getRocketStats().getFuelRate(FuelType.NUCLEAR_WORKING_FLUID);

        }
        return 0;
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        PacketHandler.sendToServer(new PacketMachine(this, (byte) (buttonId)));
        updateText();
    }

    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing arg0) {
        return true;
    }

    @Override
    public boolean onLinkStart(@Nonnull ItemStack item, TileEntity entity,
                               EntityPlayer player, World world) {
        return true;
    }

    @Override
    public boolean onLinkComplete(@Nonnull ItemStack item, TileEntity entity,
                                  EntityPlayer player, World world) {
        TileEntity tile = world.getTileEntity(ItemLinker.getMasterCoords(item));
        float maxlinkDistance = 15;

        if (tile instanceof IInfrastructure) {
            HashedBlockPosition pos = new HashedBlockPosition(tile.getPos());

            if (pos.getDistance(new HashedBlockPosition(this.pos)) > maxlinkDistance) {
                if (!world.isRemote)
                    player.sendMessage(new TextComponentTranslation("the machine is too far away to be linked"));
                return false;
            }

            if (!blockPos.contains(pos))
                blockPos.add(pos);

            if (getBBCache() == null) {
                bbCache = getRocketPadBounds(world, getPos());
            }

            if (getBBCache() != null) {

                List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, bbCache);
                for (EntityRocketBase rocket : rockets) {
                    rocket.linkInfrastructure((IInfrastructure) tile);
                }
            }

            if (!world.isRemote) {
                player.sendMessage(new TextComponentTranslation("msg.linker.success"));

                if (tile instanceof IMultiblock)
                    ((IMultiblock) tile).setMasterBlock(getPos());
            }

            ItemLinker.resetPosition(item);
            return true;
        }
        return false;
    }

    public void removeConnectedInfrastructure(TileEntity tile) {
        blockPos.remove(new HashedBlockPosition(tile.getPos()));

        if (getBBCache() == null) {
            bbCache = getRocketPadBounds(world, this.getPos());
        }

        if (getBBCache() != null) {
            List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, bbCache);

            for (EntityRocketBase rocket : rockets) {
                rocket.unlinkInfrastructure((IInfrastructure) tile);
            }
        }
    }

    public List<IInfrastructure> getConnectedInfrastructure() {
        List<IInfrastructure> infrastructure = new LinkedList<>();

        Iterator<HashedBlockPosition> iter = blockPos.iterator();

        while (iter.hasNext()) {
            HashedBlockPosition position = iter.next();
            TileEntity tile = world.getTileEntity(position.getBlockPos());
            if (tile instanceof IInfrastructure) {
                infrastructure.add((IInfrastructure) tile);
            } else
                iter.remove();
        }

        return infrastructure;
    }

    @SubscribeEvent
    public void onRocketLand(RocketLandedEvent event) {
        if (event.world.isRemote)
            return;
        EntityRocketBase rocket = (EntityRocketBase) event.getEntity();

        // This apparently happens sometimes
        if (world == null) {
            AdvancedRocketry.logger.debug("World null for rocket builder during rocket land event @ " + this.pos);
            return;
        }

        if (getBBCache() == null) {
            bbCache = getRocketPadBounds(world, pos);
        }

        if (getBBCache() != null) {
            double buffer = 0.0001;
            AxisAlignedBB bufferedBB = bbCache.grow(buffer, buffer, buffer);
            List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, bufferedBB);

            if (rockets.contains(rocket)) {
                lastRocketID = rocket.getEntityId();
                for (IInfrastructure infrastructure : getConnectedInfrastructure()) {
                    rocket.linkInfrastructure(infrastructure);
                }

                PacketHandler.sendToPlayersTrackingEntity(new PacketMachine(this, (byte) 3), rocket);
            }
        }
    }

    protected enum ErrorCodes {

        SUCCESS(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.success")),
        NOFUEL(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nofuel")),
        NOSEAT(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.noseat")),
        NOENGINES(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.noengines")),
        NOGUIDANCE(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.noguidance")),
        UNSCANNED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.unscanned")),
        SUCCESS_STATION(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.success_station")),
        DISCONNECTED_STRUCT(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.structuredisconnected")),
        HULL_FULL(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nosealedair")),
        HULL_IMPROPER(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.hullimproper")),
        EMPTY(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.empty")),
        FINISHED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.finished")),
        INCOMPLETESTRCUTURE(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.incompletestructure")),
        NOSATELLITEHATCH(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nosatellitehatch")),
        NOSATELLITECHIP(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nosatellitechip")),
        OUTPUTBLOCKED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.outputblocked")),
        INVALIDBLOCK(LibVulpes.proxy.getLocalizedString("msg.rocketbuild.invalidblock")),
        COMBINEDTHRUST(LibVulpes.proxy.getLocalizedString("msg.rocketbuild.combinedthrust")),
        ALREADY_ASSEMBLED("rocket already assembled");

        String code;

        ErrorCodes(String code) {
            this.code = code;
        }

        public String getErrorCode() {
            return code;
        }
    }
}
