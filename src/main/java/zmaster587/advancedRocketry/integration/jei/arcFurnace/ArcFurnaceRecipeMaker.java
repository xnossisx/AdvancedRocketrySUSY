package zmaster587.advancedRocketry.integration.jei.arcFurnace;

import java.util.LinkedList;
import java.util.List;

import mezz.jei.api.IJeiHelpers;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.RecipesMachine;

public class ArcFurnaceRecipeMaker {

    public static List<ArcFurnaceWrapper> getMachineRecipes(IJeiHelpers helpers, Class clazz) {
        List<ArcFurnaceWrapper> list = new LinkedList<>();
        for (IRecipe rec : RecipesMachine.getInstance().getRecipes(clazz)) {
            list.add(new ArcFurnaceWrapper(rec));
        }
        return list;
    }
}
