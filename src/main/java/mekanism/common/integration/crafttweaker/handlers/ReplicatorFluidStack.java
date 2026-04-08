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
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.mekanism.replicator.fluidstack")
@ZenRegister
public class ReplicatorFluidStack {

    public static final String NAME = Mekanism.MOD_NAME + " Replicator FluidStack";

    @ZenMethod
    public static void addRecipe(IGasStack gasInput, ILiquidStack liquidStack, double energy, int duration) {
        if (IngredientHelper.checkNotNull(NAME, gasInput, liquidStack)) {
            CrafttweakerIntegration.LATE_ADDITIONS.add(new AddMekanismRecipe<>(NAME, Recipe.REPLICATOR_FLUIDSTACK_RECIPE, new ReplicatorFluidStackRecipe(IngredientHelper.toFluid(liquidStack), GasHelper.toGas(gasInput),
                    IngredientHelper.toFluid(liquidStack), energy, duration)));
        }
    }

    @ZenMethod
    public static void removeRecipe(IIngredient liquidInput, @Optional IIngredient leftGasInput) {
        if (IngredientHelper.checkNotNull(NAME, liquidInput)) {
            CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveMekanismRecipe<>(NAME, Recipe.REPLICATOR_FLUIDSTACK_RECIPE, new IngredientWrapper(liquidInput),
                    new IngredientWrapper(leftGasInput, liquidInput)));
        }
    }

    @ZenMethod
    public static void removeAllRecipes() {
        CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveAllMekanismRecipe<>(NAME, Recipe.REPLICATOR_FLUIDSTACK_RECIPE));
    }
}
