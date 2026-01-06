package mekanism.common.recipe.outputs;

import net.minecraft.nbt.NBTTagCompound;

public class EnergyOutput extends MachineOutput<EnergyOutput> {

    public double energyOutput = 0;

    public EnergyOutput(double energy) {
        energyOutput = energy;
    }

    public EnergyOutput() {
    }


    @Override
    public EnergyOutput copy() {
        return new EnergyOutput(energyOutput);
    }

    @Override
    public void load(NBTTagCompound nbtTags) {
        energyOutput = nbtTags.getDouble("energyOutput");
    }
}
