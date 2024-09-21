package zmaster587.advancedRocketry.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.block.BlockBipropellantRocketMotor;
import zmaster587.advancedRocketry.block.BlockFuelTank;
import zmaster587.advancedRocketry.block.BlockPressurizedFluidTank;
import zmaster587.advancedRocketry.block.BlockRocketMotor;

public enum WeightEngine {

    INSTANCE("config/advRocketry/weights.json");

    private final String file;
    private Map<String, Double> weights;

    WeightEngine(String file) {
        this.file = file;
        load();
    }

    public float getWeight(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return 0;
        }
        double weight = weights.getOrDefault(stack.getItem().getRegistryName().toString(), -1.0) * stack.getCount();
        if (weight >= 0) {
            return (float) weight;
        }

        double tankWeight = 0.8;
        double motorWeight = 3;
        double guidanceComputerWeight = 1.8;

        double pressureTankWeight = 5;
        double satelliteHatchWeight = 5;

        if (stack.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.getItem()).getBlock();

            if (block instanceof BlockFuelTank) {
                weights.put(stack.getItem().getRegistryName().toString(), (double) tankWeight);
                return (float) tankWeight;
            }
            if (block instanceof BlockRocketMotor || block instanceof BlockBipropellantRocketMotor) {
                weights.put(stack.getItem().getRegistryName().toString(), (double) motorWeight);
                return (float) motorWeight;
            }
            if (block instanceof BlockPressurizedFluidTank) {
                weights.put(stack.getItem().getRegistryName().toString(), (double) pressureTankWeight);
                return (float) pressureTankWeight;
            }
            if (stack.getItem().getRegistryName().toString().equals("advancedrocketry:guidancecomputer")) {
                weights.put(stack.getItem().getRegistryName().toString(), (double) guidanceComputerWeight);
                return (float) guidanceComputerWeight;
            }
            if (stack.getItem().getRegistryName().toString().equals("advancedrocketry:loader")) {
                weights.put(stack.getItem().getRegistryName().toString(), (double) satelliteHatchWeight);
                return (float) satelliteHatchWeight;
            }
        }

        weights.put(stack.getItem().getRegistryName().toString(), 0.1);
        return 0.1F;
        // TODO Make weight selection by regular expressions
    }

    public float getWeight(Collection<ItemStack> stacks) {
        return stacks.stream().map(this::getWeight).reduce(0.0F, Float::sum);
    }

    public float getWeight(World world, BlockPos pos) {
        return getWeight(world.getTileEntity(pos), world.getBlockState(pos).getBlock());
    }

    public float getTEWeight(TileEntity te) {
        if (!ARConfiguration.getCurrentConfig().advancedWeightSystemInventories) return 0;

        float weight = 0;

        if (te == null) {
            return weight;
        }

        IItemHandler capability = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (capability == null) {
            return weight;
        }
        for (int i = 0; i < capability.getSlots(); i++) {
            weight += getWeight(capability.getStackInSlot(i));
        }
        return weight;
    }

    public float getWeight(TileEntity te, Block blk) {
        if (blk == null) {
            // if block is null, TE should be not null
            blk = te.getBlockType();
        }
        float weight = getWeight(new ItemStack(blk));

        return weight + getTEWeight(te);
    }

    public float getWeight(World world, Collection<BlockPos> poses) {
        return poses.stream().map(pos -> getWeight(world, pos)).reduce(0.0F, Float::sum);
    }

    public void load() {
        File f = new File(file);
        if (!f.exists()) {
            weights = new HashMap<>();
            return;
        }
        try (Reader r = new FileReader(file)) {
            Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            weights = GSON.fromJson(root.getAsJsonObject("individual"), HashMap.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (FileWriter w = new FileWriter(file)) {
            Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
            JsonObject json = new JsonObject();
            json.add("individual", GSON.toJsonTree(weights));
            w.write(GSON.toJson(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
