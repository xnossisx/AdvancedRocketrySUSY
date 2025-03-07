package zmaster587.advancedRocketry.integration.jei.sawmill;

import java.util.LinkedList;
import java.util.List;

import mezz.jei.api.IJeiHelpers;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.RecipesMachine;

public class SawMillRecipeMaker {

    public static List<SawMillWrapper> getMachineRecipes(IJeiHelpers helpers, Class clazz) {
        List<SawMillWrapper> list = new LinkedList<>();
        for (IRecipe rec : RecipesMachine.getInstance().getRecipes(clazz)) {
            list.add(new SawMillWrapper(rec));
        }
        return list;
    }
}
