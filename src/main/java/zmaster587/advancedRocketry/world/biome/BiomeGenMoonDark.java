package zmaster587.advancedRocketry.world.biome;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;

import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;

public class BiomeGenMoonDark extends Biome {

    public BiomeGenMoonDark(BiomeProperties properties) {
        super(properties);

        // cold and dry
        this.decorator.generateFalls = false;
        this.decorator.flowersPerChunk = 0;
        this.decorator.grassPerChunk = 0;
        this.decorator.treesPerChunk = 0;
        this.decorator.mushroomsPerChunk = 0;
        this.fillerBlock = this.topBlock = AdvancedRocketryBlocks.blockMoonTurfDark.getDefaultState();
    }

    @Override
    @Nonnull
    public List<Biome.SpawnListEntry> getSpawnableList(EnumCreatureType p_76747_1_) {
        return new LinkedList<>();
    }

    @Override
    public float getSpawningChance() {
        return 0f; // Nothing spawns
    }
}
