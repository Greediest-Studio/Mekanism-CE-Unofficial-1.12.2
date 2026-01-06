package mekanism.common.integration.groovyscript.machinerecipe;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.mekanism.Mekanism;
import com.cleanroommc.groovyscript.compat.mods.mekanism.recipe.GasRecipeBuilder;
import com.cleanroommc.groovyscript.compat.mods.mekanism.recipe.VirtualizedMekanismRegistry;
import com.cleanroommc.groovyscript.helper.Alias;
import mekanism.api.gas.GasStack;
import mekanism.common.integration.groovyscript.GrSMekanismAdd;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import org.jetbrains.annotations.Nullable;

public class GasToEnergy extends VirtualizedMekanismRegistry<GasStackFuelToEnergyRecipe> {

    public GasToEnergy() {
        super(RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE, Alias.generateOfClassAnd(GasToEnergy.class, "GasToEnergy"));
    }

    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder();
    }


    public GasStackFuelToEnergyRecipe add(GasStack ingredient, double output) {
        GroovyLog.Msg msg = GroovyLog.msg("Error adding Mekanism Gas To Energy recipe").error();
        msg.add(Mekanism.isEmpty(ingredient), () -> "input must not be empty");
        msg.add(output <= 0.0, () -> "Energy output cannot be less than 0");
        if (msg.postIfNotEmpty()) return null;
        GasStackFuelToEnergyRecipe recipe = new GasStackFuelToEnergyRecipe(ingredient.copy(), output);
        recipeRegistry.put(recipe);
        addScripted(recipe);
        return recipe;
    }

    public boolean removeByInput(GasStack input) {
        if (Mekanism.isEmpty(input)) {
            removeError("input must not be empty");
            return false;
        } else {
            GasStackFuelToEnergyRecipe recipe = recipeRegistry.get().remove(new GasInput(input));
            if (recipe != null) {
                addBackup(recipe);
                return true;
            } else {
                removeError("could not find recipe for %s", input);
                return false;
            }

        }
    }


    public static class RecipeBuilder extends GasRecipeBuilder<GasStackFuelToEnergyRecipe> {

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
            validateItems(msg);
            validateFluids(msg);
            validateGases(msg, 1, 1, 0, 0);
            msg.add(energyout <= 0.0, "Energy output cannot be less than 0,yet it was {}", this.energyout);
        }

        @Override
        public @Nullable GasStackFuelToEnergyRecipe register() {
            if (!validate()) return null;
            GasStackFuelToEnergyRecipe recipe = new GasStackFuelToEnergyRecipe(gasInput.get(0), energyout);
            GrSMekanismAdd.get().gasToEnergy.add(recipe);
            return recipe;
        }
    }
}
