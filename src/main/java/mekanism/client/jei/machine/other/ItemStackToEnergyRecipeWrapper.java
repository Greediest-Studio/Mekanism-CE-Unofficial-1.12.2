package mekanism.client.jei.machine.other;

import mekanism.client.jei.machine.MekanismRecipeWrapper;
import mekanism.common.recipe.ItemStackToEnergyRecipe;
import mekanism.common.util.MekanismUtils;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;

import java.util.ArrayList;
import java.util.List;

public class ItemStackToEnergyRecipeWrapper<RECIPE extends ItemStackToEnergyRecipe> extends MekanismRecipeWrapper<RECIPE> {


    public ItemStackToEnergyRecipeWrapper(RECIPE recipe) {
        super(recipe);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, recipe.recipeInput.ingredient);
    }


    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();
        if (mouseX >= 132 - 20 && mouseX < 132 - 20 + 18 && mouseY >= 1 && mouseY < 61) {
            tooltip.add(MekanismUtils.getEnergyDisplay(recipe.getOutput().energyOutput));
        }
        return tooltip;
    }


}
