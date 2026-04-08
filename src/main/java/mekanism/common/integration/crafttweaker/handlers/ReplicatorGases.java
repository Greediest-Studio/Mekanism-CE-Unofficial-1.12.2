package mekanism.common.integration.crafttweaker.handlers;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
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
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.mekanism.replicator.gases")
@ZenRegister
public class ReplicatorGases {

    public static final String NAME = Mekanism.MOD_NAME + " Replicator Gases";

    @ZenMethod
    public static void addRecipe(IGasStack input, IGasStack uu, double energy, int duration) {
        if (IngredientHelper.checkNotNull(NAME, input, uu)) {
            CrafttweakerIntegration.LATE_ADDITIONS.add(new AddMekanismRecipe<>(NAME, Recipe.REPLICATOR_GASES_RECIPE, new ReplicatorGasStackRecipe(GasHelper.toGas(input),
                    GasHelper.toGas(uu), GasHelper.toGas(input), energy, duration)));
        }
    }

    @ZenMethod
    public static void removeRecipe(IIngredient input, @Optional IIngredient uu) {
        if (IngredientHelper.checkNotNull(NAME, input)) {
            CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveMekanismRecipe<>(NAME, Recipe.REPLICATOR_GASES_RECIPE, new IngredientWrapper(input),
                    new IngredientWrapper(input, uu)));
        }
    }

    @ZenMethod
    public static void removeAllRecipes() {
        CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveAllMekanismRecipe<>(NAME, Recipe.REPLICATOR_GASES_RECIPE));
    }
}
