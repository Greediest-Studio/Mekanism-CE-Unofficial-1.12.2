package mekanism.generators.common.tile;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.IMachineSlotTip;
import mekanism.common.base.ISustainedData;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;

public class TileEntityGasGenerator extends TileEntityGenerator implements IGasHandler, ISustainedData, IComparatorSupport, IMachineSlotTip {

    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getGas", "getGasNeeded"};
    /**
     * The maximum amount of gas this block can store.
     */
    public int MAX_GAS = 18000;
    /**
     * The tank this block is storing fuel in.
     */
    public GasTank fuelTank;
    public int burnTicks = 0;
    public int maxBurnTicks;
    public double generationRate = 0;
    public double clientUsed;
    private int currentRedstoneLevel;
    public GasStackFuelToEnergyRecipe cachedRecipe;

    public TileEntityGasGenerator() {
        super("gas", "GasGenerator", MekanismConfig.current().general.FROM_H2.val() * 1000, MekanismConfig.current().general.FROM_H2.val() * 2);
        inventory = NonNullListSynchronized.withSize(2, ItemStack.EMPTY);
        fuelTank = new GasTank(MAX_GAS);
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.charge(1, this);


        if (!inventory.get(0).isEmpty() && fuelTank.getStored() < MAX_GAS) {
            Gas gasType = null;
            if (fuelTank.getGas() != null) {
                gasType = fuelTank.getGas().getGas();
            } else if (!inventory.get(0).isEmpty() && inventory.get(0).getItem() instanceof IGasItem gasItem) {
                if (gasItem.getGas(inventory.get(0)) != null) {
                    gasType = gasItem.getGas(inventory.get(0)).getGas();
                }
            }
            if (gasType != null && RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(gasType)) {
                GasStack removed = GasUtils.removeGas(inventory.get(0), gasType, fuelTank.getNeeded());
                boolean isTankEmpty = fuelTank.getGas() == null;
                int fuelReceived = fuelTank.receive(removed, true);
                if (fuelReceived > 0 && isTankEmpty) {
                    if (RecipeHandler.getGasStackFuelToEnergyRecipe(fuelTank.getGas()) != null) {
                        output = RecipeHandler.getGasStackFuelToEnergyRecipe(fuelTank.getGas()).getOutput().energyOutput * 2;
                    }
                }
            }
        }

        boolean operate = canOperate();
        GasStackFuelToEnergyRecipe recipe = getRecipe();
        if (operate && getEnergy() + generationRate < getMaxEnergy()) {
            setActive(true);
            if (fuelTank.getStored() != 0) {
                maxBurnTicks = recipe.getInput().ingredient.amount;
                generationRate = recipe.getOutput().energyOutput;
            }

            int toUse = getToUse();
            output = Math.max(MekanismConfig.current().general.FROM_H2.val() * 2, generationRate * getToUse() * 2);

            int total = burnTicks + fuelTank.getStored() * maxBurnTicks;
            total -= toUse;
            setEnergy(getEnergy() + generationRate * toUse);

            if (fuelTank.getStored() > 0) {
                fuelTank.setGas(new GasStack(fuelTank.getGasType(), total / maxBurnTicks));
            }
            burnTicks = total % maxBurnTicks;
            clientUsed = toUse /(double)  maxBurnTicks;
        } else {
            if (!operate) {
                reset();
            }
            clientUsed = 0;
            setActive(false);
        }
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    public void reset() {
        burnTicks = 0;
        maxBurnTicks = 0;
        generationRate = 0;
        output = MekanismConfig.current().general.FROM_H2.val() * 2;
    }

    public int getToUse() {
        if (generationRate == 0 || fuelTank.getGas() == null) {
            return 0;
        }
        int max = (int) Math.ceil(((float) fuelTank.getStored() / (float) fuelTank.getMaxGas()) * 256F);
        max = Math.min((fuelTank.getStored() * maxBurnTicks) + burnTicks, max);
        max = (int) Math.min((getMaxEnergy() - getEnergy()) / generationRate, max);
        return max;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return ChargeUtils.canBeOutputted(itemstack, true);
        } else if (slotID == 0) {
            return itemstack.getItem() instanceof IGasItem gasItem && gasItem.getGas(itemstack) == null;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return itemstack.getItem() instanceof IGasItem gasItem && gasItem.getGas(itemstack) != null &&
                    RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(gasItem.getGas(itemstack).getGas());
        } else if (slotID == 1) {
            return ChargeUtils.canBeCharged(itemstack);
        }
        return true;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return side == MekanismUtils.getRight(facing) ? new int[]{1} : new int[]{0};
    }

    @Override
    public boolean canOperate() {
        return (fuelTank.getStored() > 0 || burnTicks > 0) && MekanismUtils.canFunction(this);
    }


    /**
     * Gets the scaled gas level for the GUI.
     *
     * @param i - multiplier
     * @return Scaled gas level
     */
    public int getScaledGasLevel(int i) {
        return fuelTank.getStored() * i / MAX_GAS;
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        return switch (method) {
            case 0 -> new Object[]{getEnergy()};
            case 1 -> new Object[]{output};
            case 2 -> new Object[]{getMaxEnergy()};
            case 3 -> new Object[]{getNeedEnergy()};
            case 4 -> new Object[]{fuelTank.getStored()};
            case 5 -> new Object[]{fuelTank.getNeeded()};
            default -> throw new NoSuchMethodException();
        };
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            TileUtils.readTankData(dataStream, fuelTank);
            generationRate = dataStream.readDouble();
            output = dataStream.readDouble();
            clientUsed = dataStream.readDouble();
            maxBurnTicks = dataStream.readInt();
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        TileUtils.addTankData(data, fuelTank);
        data.add(generationRate);
        data.add(output);
        data.add(clientUsed);
        data.add(maxBurnTicks);
        return data;
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        boolean isTankEmpty = fuelTank.getGas() == null;
        if (canReceiveGas(side, stack.getGas()) && (isTankEmpty || fuelTank.getGas().isGasEqual(stack))) {
            int fuelReceived = fuelTank.receive(stack, doTransfer);
            if (doTransfer && isTankEmpty && fuelReceived > 0) {
                output = RecipeHandler.getGasStackFuelToEnergyRecipe(fuelTank.getGas()).getOutput().energyOutput * 2;
            }
            return fuelReceived;
        }
        return 0;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{fuelTank};
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        fuelTank.read(nbtTags.getCompoundTag("fuelTank"));
        boolean isTankEmpty = fuelTank.getGas() == null;
        GasStackFuelToEnergyRecipe recipe = RecipeHandler.getGasStackFuelToEnergyRecipe(fuelTank.getGas());
        if (!isTankEmpty) {
            output = recipe.getOutput().energyOutput * 2;
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(type) && side != facing;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return null;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return false;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return side == facing;
        }
        return super.isCapabilityDisabled(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fuelTank != null) {
            ItemDataUtils.setCompound(itemStack, "fuelTank", fuelTank.write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        if (ItemDataUtils.hasData(itemStack, "fuelTank")) {
            fuelTank.read(ItemDataUtils.getCompound(itemStack, "fuelTank"));
            boolean isTankEmpty = fuelTank.getGas() == null;
            //Update energy output based on any existing fuel in tank
            GasStackFuelToEnergyRecipe recipe = RecipeHandler.getGasStackFuelToEnergyRecipe(fuelTank.getGas());
            if (!isTankEmpty) {
                output = recipe.getOutput().energyOutput * 2;
            }
        }
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(fuelTank.getStored(), fuelTank.getMaxGas());
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(1).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
    }

    public GasStackFuelToEnergyRecipe getRecipe() {
        GasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getGasStackFuelToEnergyRecipe(getInput());
        }
        return cachedRecipe;
    }

    public GasInput getInput() {
        return new GasInput(fuelTank.getGas());
    }


    public double getUsed() {
        return Math.round(clientUsed * 100) / 100D;
    }

    public int getMaxBurnTicks() {
        return maxBurnTicks;
    }
}
