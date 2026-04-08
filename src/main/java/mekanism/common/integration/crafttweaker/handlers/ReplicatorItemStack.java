package mekanism.common.integration.crafttweaker.handlers;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.minecraft.CraftTweakerMC;
import mekanism.api.gas.GasStack;
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
import mekanism.common.recipe.inputs.NucleosynthesizerInput;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

@ZenClass("mods.mekanism.replicator.itemstack")
@ZenRegister
public class ReplicatorItemStack {

    public static final String NAME = Mekanism.MOD_NAME + " Replicator ItemStack";

    @ZenMethod
    public static void addRecipe(IIngredient ingredientInput, IGasStack gasInput, double energy, int duration) {
        if (IngredientHelper.checkNotNull(NAME, ingredientInput, gasInput)) {
            GasStack gas = GasHelper.toGas(gasInput);
            List<ReplicatorItemStackRecipe> recipes = new ArrayList<>();
            for (ItemStack stack : CraftTweakerMC.getIngredient(ingredientInput).getMatchingStacks()) {
                recipes.add(new ReplicatorItemStackRecipe(new NucleosynthesizerInput(stack, gas), new ItemStackOutput(stack), energy, duration));
            }
            CrafttweakerIntegration.LATE_ADDITIONS.add(new AddMekanismRecipe<>(NAME, Recipe.REPLICATOR_ITEMSTACK_RECIPE, recipes));
        }
    }

    @ZenMethod
    public static void removeRecipe(IIngredient itemInput, @Optional IIngredient gasInput) {
        if (IngredientHelper.checkNotNull(NAME)) {
            CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveMekanismRecipe<>(NAME, Recipe.REPLICATOR_ITEMSTACK_RECIPE, new IngredientWrapper(itemInput),
                    new IngredientWrapper(itemInput, gasInput)));
        }
    }

    @ZenMethod
    public static void removeAllRecipes() {
        CrafttweakerIntegration.LATE_REMOVALS.add(new RemoveAllMekanismRecipe<>(NAME, Recipe.REPLICATOR_ITEMSTACK_RECIPE));
    }
}
