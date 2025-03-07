package zmaster587.advancedRocketry.integration.jei.precisionLaserEtcher;

import java.util.LinkedList;
import java.util.List;

import mezz.jei.api.IJeiHelpers;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.RecipesMachine;

public class PrecisionLaserEtcherRecipeMaker {

    public static List<PrecisionLaserEtcherWrapper> getMachineRecipes(IJeiHelpers helpers, Class clazz) {
        List<PrecisionLaserEtcherWrapper> list = new LinkedList<>();
        for (IRecipe rec : RecipesMachine.getInstance().getRecipes(clazz)) {
            list.add(new PrecisionLaserEtcherWrapper(rec));
        }
        return list;
    }
}
