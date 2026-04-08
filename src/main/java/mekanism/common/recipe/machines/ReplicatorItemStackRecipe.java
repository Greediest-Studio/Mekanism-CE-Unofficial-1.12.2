package mekanism.common.recipe.machines;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.common.recipe.inputs.NucleosynthesizerInput;
import mekanism.common.recipe.outputs.ItemStackOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

public class ReplicatorItemStackRecipe extends MachineRecipe<NucleosynthesizerInput, ItemStackOutput, ReplicatorItemStackRecipe> {

    public double extraEnergy;

    public int ticks;

    public ReplicatorItemStackRecipe(NucleosynthesizerInput input, ItemStackOutput output, double energy, int duration) {
        super(input, output);
        extraEnergy = energy;
        ticks = duration;
    }

    public ReplicatorItemStackRecipe(ItemStack input, GasStack gas, ItemStack output, double energy, int duration) {
        this(new NucleosynthesizerInput(input, gas), new ItemStackOutput(output), energy, duration);
    }

    public ReplicatorItemStackRecipe(NucleosynthesizerInput input, ItemStackOutput outputSolid, NBTTagCompound extraNBT) {
        super(input, outputSolid);
        extraEnergy = extraNBT.getDouble("extraEnergy");
        ticks = extraNBT.getInteger("duration");
    }

    public boolean canOperate(NonNullList<ItemStack> inventory, int inputIndex, int outputIndex, GasTank gasTank) {
        return getInput().use(inventory, inputIndex, gasTank, false) && getOutput().applyOutputs(inventory, outputIndex, false);
    }

    public void operate(NonNullList<ItemStack> inventory, int inputIndex, GasTank inputGasTank, int outputIndex) {
        operate(inventory, inputIndex, inputGasTank, outputIndex, true);
    }

    public void operate(NonNullList<ItemStack> inventory, int inputIndex, GasTank inputGasTank, int outputIndex, boolean deplete) {
        if (getInput().use(inventory, inputIndex, inputGasTank, false,deplete)) {
            getOutput().applyOutputs(inventory, outputIndex, true);
        }
    }

    @Override
    public ReplicatorItemStackRecipe copy() {
        return new ReplicatorItemStackRecipe(getInput().copy(), getOutput().copy(), extraEnergy, ticks);
    }
}
