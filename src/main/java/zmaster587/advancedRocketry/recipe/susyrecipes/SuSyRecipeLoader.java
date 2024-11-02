package zmaster587.advancedRocketry.recipe.susyrecipes;

import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import zmaster587.advancedRocketry.block.susy.ARSuSyBlocks;

import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;

public class SuSyRecipeLoader {
    public static void init() {
        ASSEMBLER_RECIPES.recipeBuilder().
                input(OrePrefix.plate, Materials.Glass, 3).
                input(OrePrefix.plate, Materials.Aluminium, 2).
                output(ARSuSyBlocks.blockHullTile).
                duration(50).
                buildAndRegister();
    }
}
