package mekanism.common.integration.groovyscript.machinerecipe;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.api.documentation.annotations.*;
import com.cleanroommc.groovyscript.compat.mods.mekanism.Mekanism;
import com.cleanroommc.groovyscript.compat.mods.mekanism.recipe.GasRecipeBuilder;
import com.cleanroommc.groovyscript.compat.mods.mekanism.recipe.VirtualizedMekanismRegistry;
import com.cleanroommc.groovyscript.helper.Alias;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import mekanism.api.gas.GasStack;
import mekanism.common.integration.groovyscript.GrSMekanismAdd;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.WasherRecipe;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@RegistryDescription
public class Washer extends VirtualizedMekanismRegistry<WasherRecipe> {

    public Washer() {
        super(RecipeHandler.Recipe.CHEMICAL_WASHER, Alias.generateOfClassAnd(Washer.class, "ChemicalWasher"));
    }

    @RecipeBuilderDescription(example = @Example(".gasInput(gas('water') * 10).fluidInput(fluid('water')).gasOutput(gas('hydrogen') * 20)"))
    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder();
    }


    @MethodDescription(type = MethodDescription.Type.ADDITION, example = @Example(value = "gas('water'),gas('hydrogen')", commented = true))
    public WasherRecipe add(GasStack input, GasStack output) {
        return recipeBuilder().gasOutput(output).gasInput(input).register();
    }

    @MethodDescription(type = MethodDescription.Type.ADDITION, example = @Example(value = "gas('water'),fluid('water'),gas('hydrogen')", commented = true))
    public WasherRecipe add(GasStack input, FluidStack stack, GasStack output) {
        return recipeBuilder().gasOutput(output).gasInput(input).fluidInput(stack).register();
    }

    @MethodDescription(example = @Example("gas('iron'),fluid('water')"))
    public boolean removeByInput(GasStack input, FluidStack stack) {
        GroovyLog.Msg msg = GroovyLog.msg("Error removing Mekanism Washer recipe").error();
        msg.add(Mekanism.isEmpty(input), () -> "input must not be empty");
        msg.add(IngredientHelper.isEmpty(stack), () -> "input must not be empty");
        if (msg.postIfNotEmpty()) return false;
        WasherRecipe recipe = recipeRegistry.get().remove(new GasAndFluidInput(input, stack));
        if (recipe != null) {
            addBackup(recipe);
            return true;
        }
        removeError("could not find recipe for % and %", input, stack);
        return false;
    }

    @Property(property = "gasInput", comp = @Comp(eq = 1))
    @Property(property = "fluidInput", comp = @Comp(eq = 1))
    @Property(property = "gasOutput", comp = @Comp(eq = 1))
    public static class RecipeBuilder extends GasRecipeBuilder<WasherRecipe> {

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg);
            validateFluids(msg, 1, 1, 0, 0);
            validateGases(msg, 1, 1, 1, 1);
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Mekanism Washer recipe";
        }


        @Override
        @RecipeBuilderRegistrationMethod
        public @Nullable WasherRecipe register() {
            if (!validate()) return null;
            WasherRecipe recipe = new WasherRecipe(gasInput.get(0), fluidInput.get(0), gasOutput.get(0));
            GrSMekanismAdd.get().washer.add(recipe);
            return recipe;
        }
    }

}
