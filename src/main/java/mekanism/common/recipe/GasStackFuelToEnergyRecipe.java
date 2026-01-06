package mekanism.common.recipe;

import mekanism.api.gas.GasStack;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.MachineRecipe;
import mekanism.common.recipe.outputs.EnergyOutput;

public class GasStackFuelToEnergyRecipe extends MachineRecipe<GasInput, EnergyOutput, GasStackFuelToEnergyRecipe> {

    public GasStackFuelToEnergyRecipe(GasInput input, EnergyOutput output) {
        super(input, output);
    }

    public GasStackFuelToEnergyRecipe(GasStack input, double outout) {
        this(new GasInput(input), new EnergyOutput(outout));
    }

    @Override
    public GasStackFuelToEnergyRecipe copy() {
        return new GasStackFuelToEnergyRecipe(getInput().copy(), getOutput().copy());
    }
}
