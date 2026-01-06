package mekanism.common.integration.groovyscript.machinerecipe;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.api.IIngredient;
import com.cleanroommc.groovyscript.compat.mods.mekanism.recipe.VirtualizedMekanismRegistry;
import com.cleanroommc.groovyscript.helper.Alias;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import mekanism.common.integration.groovyscript.GrSMekanismAdd;
import mekanism.common.recipe.ItemStackToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.ItemStackInput;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemToEnergy extends VirtualizedMekanismRegistry<ItemStackToEnergyRecipe> {

    public ItemToEnergy() {
        super(RecipeHandler.Recipe.ENERGY_RECIPE, Alias.generateOfClassAnd(ItemToEnergy.class, "ItemStackToEnergy"));
    }

    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder();
    }


    public ItemStackToEnergyRecipe add(IIngredient ingredient, double output) {
        GroovyLog.Msg msg = GroovyLog.msg("Error adding Mekanism ItemStack To Energy recipe").error();
        msg.add(IngredientHelper.isEmpty(ingredient), () -> "input must not be empty");
        msg.add(output <= 0.0, () -> "Energy output cannot be less than 0");
        if (msg.postIfNotEmpty()) return null;
        ItemStackToEnergyRecipe recipe1 = null;
        for (ItemStack itemStack : ingredient.getMatchingStacks()) {
            ItemStackToEnergyRecipe recipe = new ItemStackToEnergyRecipe(itemStack.copy(), output);
            if (recipe1 == null) recipe1 = recipe;
            recipeRegistry.put(recipe);
            addScripted(recipe);
        }
        return recipe1;
    }

    public boolean removeByInput(IIngredient ingredient) {
        if (IngredientHelper.isEmpty(ingredient)) {
            removeError("input must not be empty");
            return false;
        }
        boolean found = false;
        for (ItemStack itemStack : ingredient.getMatchingStacks()) {
            ItemStackToEnergyRecipe recipe = recipeRegistry.get().remove(new ItemStackInput(itemStack));
            if (recipe != null) {
                addBackup(recipe);
                found = true;
            }
        }
        if (!found) {
            removeError("could not find recipe for %s", ingredient);
        }
        return found;
    }


    public static class RecipeBuilder extends AbstractRecipeBuilder<ItemStackToEnergyRecipe> {

        private double energyout;

        public RecipeBuilder energyout(double energyout) {
            this.energyout = energyout;
            return this;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Mekanism Item to Energy recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 1, 0, 0);
            validateFluids(msg);
            msg.add(energyout <= 0.0, "Energy output cannot be less than 0,yet it was {}", this.energyout);
        }

        @Override
        public @Nullable ItemStackToEnergyRecipe register() {
            if (!validate()) return null;
            ItemStackToEnergyRecipe recipe = null;
            for (ItemStack itemStack : input.get(0).getMatchingStacks()) {
                ItemStackToEnergyRecipe r = new ItemStackToEnergyRecipe(itemStack.copy(), energyout);
                if (recipe == null) recipe = r;
                GrSMekanismAdd.get().itemToEnergy.add(r);
            }
            return recipe;
        }
    }
}
