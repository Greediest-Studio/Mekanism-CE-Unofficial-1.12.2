package mekanism.common.recipe.inputs;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An input of gasses for recipe use.
 *
 * @author aidancbrady
 */
public class ChemicalGasInput extends MachineInput<ChemicalGasInput> {

    /**
     * The left gas of this chemical input
     */
    public GasStack input;

    /**
     * The right gas of this chemical input
     */
    public GasStack uu;

    /**
     * Creates a chemical input with two defined gasses.
     *
     * @param left  - left gas
     * @param right - right gas
     */
    public ChemicalGasInput(GasStack left, GasStack right) {
        input = left;
        uu = right;
    }

    public ChemicalGasInput() {
    }

    @Override
    public void load(NBTTagCompound nbtTags) {
        input = GasStack.readFromNBT(nbtTags.getCompoundTag("input"));
        uu = GasStack.readFromNBT(nbtTags.getCompoundTag("uu"));
    }

    public boolean useGas(GasTank inputTank, GasTank UUTank, boolean deplete, int scale) {
        if ((inputTank.getGasType() == input.getGas() && inputTank.getStored() >= input.amount * scale) && (UUTank.getGasType() == uu.getGas() && UUTank.getStored() >= uu.amount * scale)) {
            UUTank.draw(uu.amount * scale, deplete);
            return true;
        }
        return false;
    }

    /**
     * @return True if this is a valid ChemicalPair
     */
    @Override
    public boolean isValid() {
        return input != null && uu != null;
    }


    @Override
    public ChemicalGasInput copy() {
        return new ChemicalGasInput(input.copy(), uu.copy());
    }

    @Override
    public int hashIngredients() {
        return (input.hashCode() << 8 | uu.hashCode()) + (uu.hashCode() << 8 | input.hashCode());
    }

    @Override
    public boolean testEquality(ChemicalGasInput other) {
        if (!isValid()) {
            return !other.isValid();
        }
        return (other.input.hashCode() == input.hashCode() && other.uu.hashCode() == uu.hashCode());
    }


    @Override
    public boolean isInstance(Object other) {
        return other instanceof ChemicalGasInput;
    }
}
