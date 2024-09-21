package zmaster587.advancedRocketry.integration.jei.platePresser;

import java.util.LinkedList;
import java.util.List;

import mezz.jei.api.IJeiHelpers;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.RecipesMachine;

public class PlatePressRecipeMaker {

    public static List<PlatePressWrapper> getMachineRecipes(IJeiHelpers helpers, Class clazz) {
        List<PlatePressWrapper> list = new LinkedList<>();
        for (IRecipe rec : RecipesMachine.getInstance().getRecipes(clazz)) {
            list.add(new PlatePressWrapper(rec));
        }
        return list;
    }
}
