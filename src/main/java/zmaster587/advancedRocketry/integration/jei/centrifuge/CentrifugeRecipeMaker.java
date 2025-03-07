package zmaster587.advancedRocketry.integration.jei.centrifuge;

import java.util.LinkedList;
import java.util.List;

import mezz.jei.api.IJeiHelpers;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.RecipesMachine;

public class CentrifugeRecipeMaker {

    public static List<CentrifugeWrapper> getMachineRecipes(IJeiHelpers helpers, Class clazz) {
        List<CentrifugeWrapper> list = new LinkedList<>();
        for (IRecipe rec : RecipesMachine.getInstance().getRecipes(clazz)) {
            list.add(new CentrifugeWrapper(rec));
        }
        return list;
    }
}
