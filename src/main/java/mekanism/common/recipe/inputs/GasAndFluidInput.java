package mekanism.common.recipe.inputs;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

public class GasAndFluidInput extends MachineInput<GasAndFluidInput> {

    public GasStack ingredientGas;
    public FluidStack ingredientFluid;

    public GasAndFluidInput(GasStack gas, FluidStack fluid) {
        ingredientGas = gas;
        ingredientFluid = fluid;
    }

    public GasAndFluidInput() {
    }

    @Override
    public void load(NBTTagCompound nbtTags) {
        ingredientGas = GasStack.readFromNBT(nbtTags.getCompoundTag("ingredientGas"));
        ingredientFluid = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("ingredientFluid"));
    }

    @Override
    public GasAndFluidInput copy() {
        return new GasAndFluidInput(ingredientGas.copy(), ingredientFluid.copy());
    }

    @Override
    public boolean isValid() {
        return ingredientGas != null && ingredientFluid != null;
    }

    public boolean useGas(GasTank gasTank, boolean deplete, int scale) {
        if (gasTank.getGasType() == ingredientGas.getGas() && gasTank.getStored() >= ingredientGas.amount * scale) {
            gasTank.draw(ingredientGas.amount * scale, deplete);
            return true;
        }
        return false;
    }

    public boolean useFluid(FluidTank fluidTank, boolean deplete, int scale) {
        if (fluidTank.getFluid() != null && fluidTank.getFluid().containsFluid(ingredientFluid)) {
            fluidTank.drain(ingredientFluid.amount * scale, deplete);
            return true;
        }
        return false;
    }

    @Override
    public int hashIngredients() {
        return (ingredientFluid.getFluid() != null ? ingredientFluid.getFluid().hashCode() : 0) << 8 | ingredientGas.hashCode();
    }

    @Override
    public boolean testEquality(GasAndFluidInput other) {
       return other.containsType(ingredientFluid) && other.containsType(ingredientGas);
    }

    public boolean containsType(FluidStack stack) {
        if (stack == null || stack.amount == 0) {
            return false;
        }
        return stack.isFluidEqual(ingredientFluid);
    }

    public boolean containsType(GasStack stack) {
        if (stack == null || stack.amount == 0) {
            return false;
        }
        return stack.isGasEqual(ingredientGas);
    }

    @Override
    public boolean isInstance(Object other) {
        return other instanceof GasAndFluidInput;
    }
}
