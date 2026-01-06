package mekanism.common.recipe;

import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.MachineRecipe;
import mekanism.common.recipe.outputs.EnergyOutput;
import net.minecraft.item.ItemStack;

public class ItemStackToEnergyRecipe extends MachineRecipe<ItemStackInput, EnergyOutput, ItemStackToEnergyRecipe> {

    public ItemStackToEnergyRecipe(ItemStackInput input, EnergyOutput output) {
        super(input, output);
    }

    public ItemStackToEnergyRecipe(ItemStack input, double outout) {
        this(new ItemStackInput(input), new EnergyOutput(outout));
    }

    @Override
    public ItemStackToEnergyRecipe copy() {
        return new ItemStackToEnergyRecipe(getInput().copy(), getOutput().copy());
    }
}
