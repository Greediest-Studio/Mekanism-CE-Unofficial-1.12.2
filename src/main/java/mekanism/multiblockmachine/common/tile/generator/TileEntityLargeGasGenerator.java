package mekanism.multiblockmachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.util.*;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.multiblockmachine.client.render.block.generator.bloom.BloomRendererLargeGasGenerator;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class TileEntityLargeGasGenerator extends TileEntityGenerator implements IAdvancedBoundingBlock, IGasHandler, ISustainedData, IComparatorSupport, IMachineSlotTip, IUpgradeTile {

    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getGas", "getGasNeeded"};
    /**
     * The maximum amount of gas this block can store.
     */
    public int MAX_GAS = 8192000;
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
    public TileComponentUpgrade upgradeComponent;
    public int processes = MekanismConfig.current().multiblock.LargeGasGeneratorProcesses.val();
    public int numPowering;

    public TileEntityLargeGasGenerator() {
        super("gas", "LargeGasGenerator", 0, 0);
        inventory = NonNullListSynchronized.withSize(3, ItemStack.EMPTY);
        fuelTank = new GasTank(MAX_GAS);
        upgradeComponent = new TileComponentUpgrade(this, 2, Upgrade.ENERGY);
        upgradeComponent.setSupported(Upgrade.THREAD);
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
                fuelTank.receive(removed, true);
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

    @Override
    public void addTileSyncTask() {
        CableUtils.emit(this, 3);
    }

    public int getThread() {
        int thread = 1;
        if (upgradeComponent.isUpgradeInstalled(Upgrade.THREAD)) {
            thread += upgradeComponent.getUpgrades(Upgrade.THREAD);
        }
        return thread;
    }

    public void reset() {
        burnTicks = 0;
        maxBurnTicks = 0;
        generationRate = 0;
    }

    public int getToUse() {
        if (generationRate == 0 || fuelTank.getGas() == null) {
            return 0;
        }
        int max = (int) Math.ceil(((float) fuelTank.getStored() / (float) fuelTank.getMaxGas()) * 256F);
        max *= processes;
        max *= getThread();
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

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        return switch (method) {
            case 0 -> new Object[]{getEnergy()};
            case 1 -> new Object[]{getOutput()};
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
            clientUsed = dataStream.readDouble();
            maxBurnTicks = dataStream.readInt();
            numPowering = dataStream.readInt();
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        TileUtils.addTankData(data, fuelTank);
        data.add(generationRate);
        data.add(clientUsed);
        data.add(maxBurnTicks);
        data.add(numPowering);
        return data;
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        boolean isTankEmpty = fuelTank.getGas() == null;
        if (canReceiveGas(side, stack.getGas()) && (isTankEmpty || fuelTank.getGas().isGasEqual(stack))) {
            return fuelTank.receive(stack, doTransfer);
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
        numPowering = nbtTags.getInteger("numPowering");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
        nbtTags.setInteger("numPowering", numPowering);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(type) && side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN;
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
        return (side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN && capability == Capabilities.GAS_HANDLER_CAPABILITY) || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN && capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        EnumFacing back = MekanismUtils.getBack(facing);
        if ((side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN && capability == Capabilities.GAS_HANDLER_CAPABILITY)
                || (side != facing && side != left && side != right && side != back && side != EnumFacing.DOWN && (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)))) {
            return true;
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

    @Override
    public boolean canBoundReceiveEnergy(BlockPos location, EnumFacing side) {
        return false;
    }

    @Override
    public boolean canBoundOutPutEnergy(BlockPos coord, EnumFacing side) {
        if (coord.equals(getPos().offset(EnumFacing.UP, 2))) {
            return side == EnumFacing.UP;
        }
        return false;
    }

    @Override
    public void onPower() {
        numPowering++;
    }

    @Override
    public void onNoPower() {
        numPowering--;
    }

    @Override
    public boolean isPowered() {
        return redstone || numPowering > 0;
    }

    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {

    }

    @Override
    public String getDataType() {
        return getName();
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.LargeGasGenerator.name");
    }

    @Override
    public void onPlace() {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    if (y != 2) {
                        if (x == z || x == -z ||
                                facing == EnumFacing.NORTH && (x == 0 && z == -1) ||
                                facing == EnumFacing.WEST && (z == 0 && x == -1) ||
                                facing == EnumFacing.SOUTH && (x == 0 && z == 1) ||
                                facing == EnumFacing.EAST && (z == 0 && x == 1)) {
                            MekanismUtils.makeBoundingBlock(world, getPos().add(x, y, z), Coord4D.get(this));
                        } else {
                            MekanismUtils.makeAdvancedBoundingBlock(world, getPos().add(x, y, z), Coord4D.get(this));
                        }
                    } else {
                        if (x == 0 && z == 0) {
                            MekanismUtils.makeAdvancedBoundingBlock(world, getPos().add(x, y, z), Coord4D.get(this));
                        } else {
                            MekanismUtils.makeBoundingBlock(world, getPos().add(x, y, z), Coord4D.get(this));
                        }
                    }
                    world.notifyNeighborsOfStateChange(getPos().add(x, y, z), getBlockType(), true);
                }
            }
        }
    }

    @Override
    public void onBreak() {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlockToAir(getPos().add(x, y, z));
                }
            }
        }
    }

    @Override
    public boolean hasOffsetCapability(@NotNull Capability<?> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        EnumFacing back = MekanismUtils.getBack(facing);
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return false;
        }
        if ((side != facing && side != left && side != right && side != back && side != EnumFacing.DOWN && (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side))) || (side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN && capability == Capabilities.GAS_HANDLER_CAPABILITY)) {
            return true;
        }
        return hasCapability(capability, side);
    }

    @Override
    public @Nullable <T> T getOffsetCapability(@NotNull Capability<T> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        EnumFacing back = MekanismUtils.getBack(facing);
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return null;
        } else if (side != facing && side != left && side != right && side != back && side != EnumFacing.DOWN) {
            if (isStrictEnergy(capability)) {
                return (T) this;
            } else if (isTesla(capability, side)) {
                return (T) getTeslaEnergyWrapper(side);
            } else if (capability == CapabilityEnergy.ENERGY) {
                return CapabilityEnergy.ENERGY.cast(getForgeEnergyWrapper(side));
            }
        } else if (side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN && capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        }
        return getCapability(capability, side);
    }

    @Override
    public boolean isOffsetCapabilityDisabled(@NotNull Capability<?> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        EnumFacing back = MekanismUtils.getBack(facing);
        if (side != facing && side != left && side != right && side != back && side != EnumFacing.DOWN && (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side))) {
            if (offset.equals(new Vec3i(0, 2, 0))) {
                return side != EnumFacing.UP;
            }
        } else if (side != facing && side != EnumFacing.UP && side != EnumFacing.DOWN && capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            for (int y = 0; y < 1; y++) {
                if (offset.equals(new Vec3i(back.getXOffset(), y, back.getZOffset()))) {
                    return side != back;
                } else if (offset.equals(new Vec3i(left.getXOffset(), y, left.getZOffset()))) {
                    return side != left;
                } else if (offset.equals(new Vec3i(right.getXOffset(), y, right.getZOffset()))) {
                    return side != right;
                }
            }

        }
        return false;
    }

    @Override
    public boolean renderUpdate() {
        return false;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    @Override
    public boolean sideIsOutput(EnumFacing side) {
        return side == EnumFacing.UP;
    }

    @Override
    public void validate() {
        super.validate();
        if (isRemote()) {
            if (Mekanism.hooks.Bloom && MekanismConfig.current().client.enableBloom.val()) {
                new BloomRendererLargeGasGenerator(this);
            }
        }
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 4;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MekanismMultiblockMachine.proxy;
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }


    @Override
    public double getMaxOutput() {
        return (upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy()) * 2;
    }

    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }

    public double getTierEnergy() {
        return MekanismConfig.current().general.FROM_H2.val() * 1000 * processes * getThread();
    }

    public double getUsed() {
        return Math.round(clientUsed * 100) / 100D;
    }

    public int getMaxBurnTicks() {
        return maxBurnTicks;
    }


}


