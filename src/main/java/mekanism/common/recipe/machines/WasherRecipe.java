package mekanism.common.recipe.machines;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.outputs.GasOutput;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;


public class WasherRecipe extends MachineRecipe<GasAndFluidInput, GasOutput, WasherRecipe> {

    public WasherRecipe(GasAndFluidInput input, GasOutput output) {
        super(input, output);
    }

    public WasherRecipe(GasStack input, FluidStack stack, GasStack output) {
        this(new GasAndFluidInput(input, stack), new GasOutput(output));
    }

    public WasherRecipe(GasStack input, GasStack output) {
        this(new GasAndFluidInput(input, new FluidStack(FluidRegistry.WATER, 5)), new GasOutput(output));
    }


    @Override
    public WasherRecipe copy() {
        return new WasherRecipe(getInput().copy(), getOutput().copy());
    }

    public boolean canOperate(GasTank inputTank, FluidTank fluidTank, GasTank outputTank) {
        return getInput().useGas(inputTank, false, 1) && getInput().useFluid(fluidTank, false, 1) && getOutput().applyOutputs(outputTank, false, 1);
    }

    public void operate(GasTank inputTank, FluidTank fluidTank, GasTank outputTank, int scale) {
        operate(inputTank, fluidTank, outputTank, scale, true);
    }

    public void operate(GasTank inputTank, FluidTank fluidTank, GasTank outputTank, int scale, boolean deplete) {
        if (getInput().useGas(inputTank, deplete, scale) && getInput().useFluid(fluidTank, deplete, scale)) {
            getOutput().applyOutputs(outputTank, true, scale);
        }
    }
}
