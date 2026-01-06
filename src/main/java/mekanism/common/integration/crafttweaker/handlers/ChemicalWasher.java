package mekanism.common.integration.crafttweaker.handlers;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.liquid.ILiquidStack;
import mekanism.common.Mekanism;
import mekanism.common.integration.crafttweaker.CrafttweakerIntegration;
import mekanism.common.integration.crafttweaker.gas.IGasStack;
import mekanism.common.integration.crafttweaker.helpers.GasHelper;
import mekanism.common.integration.crafttweaker.helpers.IngredientHelper;
import mekanism.common.integration.crafttweaker.util.AddMekanismRecipe;
import mekanism.common.integration.crafttweaker.util.IngredientWrapper;
import mekanism.common.integration.crafttweaker.util.RemoveAllMekanismRecipe;
import mekanism.common.integration.crafttweaker.util.RemoveMekanismRecipe;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.machines.WasherRecipe;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.mekanism.chemical.washer")
@ZenRegister
public class ChemicalWasher {

    public static final String NAME = Mekanism.MOD_NAME + " Chemical Washer";

    @ZenMethod
    public static void addRecipe(IGasStack gasInput, ILiquidStack liquidStack, IGasStack gasOutput) {
        if (IngredientHelper.checkNotNull(NAME, gasInput, liquidStack, gasOutput)) {
            CrafttweakerIntegration.LATE_ADDITIONS.add(new AddMekanismRecipe<>(NAME, Recipe.CHEMICAL_WASHER, new WasherRecipe(GasHelper.toGas(gasInput), IngredientHelper.toFluid(liquidStack), GasHelper.toGas(gasOutput))));
        }
    }

    @ZenMethod
    public static void addRecipe(IGasStack gasInput, IGasStack gasOutput) {
        if (IngredientHelper.checkNotNull(NAME, gasInput, gasOutput)) {
            CrafttweakerIntegration.LATE_ADDITIONS.add(new AddMekanismRecipe<>(NAME, Recipe.CHEMICAL_WASHER, new WasherRecipe(GasHelper.toGas(gasInput), GasHelper.toGas(gasOutput))));
        }
    }


    @ZenMethod
    public static void removeRecipe(IIngredient gasOutput, @Optional IIngredient gasInput, @Optional IIngredient liquidInput) {
        if (IngredientHelper.checkNotNull(NAME, gasOutput)) {
            CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveMekanismRecipe<>(NAME, Recipe.CHEMICAL_WASHER, new IngredientWrapper(gasOutput),
                    new IngredientWrapper(gasInput, liquidInput)));
        }
    }

    @ZenMethod
    public static void removeAllRecipes() {
        CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveAllMekanismRecipe<>(NAME, Recipe.CHEMICAL_WASHER));
    }
}
