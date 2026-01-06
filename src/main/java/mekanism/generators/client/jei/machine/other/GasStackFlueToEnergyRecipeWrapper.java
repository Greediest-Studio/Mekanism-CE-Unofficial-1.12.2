package mekanism.generators.client.jei.machine.other;

import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.machine.MekanismRecipeWrapper;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mezz.jei.api.ingredients.IIngredients;

public class GasStackFlueToEnergyRecipeWrapper<RECIPE extends GasStackFuelToEnergyRecipe> extends MekanismRecipeWrapper<RECIPE> {

    public GasStackFlueToEnergyRecipeWrapper(RECIPE recipe) {
        super(recipe);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(MekanismJEI.TYPE_GAS, recipe.recipeInput.ingredient);
    }


}
