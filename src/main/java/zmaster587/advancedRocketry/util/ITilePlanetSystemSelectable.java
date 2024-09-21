package zmaster587.advancedRocketry.util;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

public interface ITilePlanetSystemSelectable {

    @Nonnull
    ItemStack getChipWithId(int id);

    void setSelectedPlanetId(int id);

    List<Integer> getVisiblePlanets();
}
