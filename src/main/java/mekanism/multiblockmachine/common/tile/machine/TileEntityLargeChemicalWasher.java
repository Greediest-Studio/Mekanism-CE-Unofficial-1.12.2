package mekanism.multiblockmachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.Upgrade.IUpgradeInfoHandler;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.WasherRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekanism.multiblockmachine.client.render.block.machine.bloom.BloomRenderLargeChemicalWasher;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TileEntityLargeChemicalWasher extends TileEntityBasicMachine<GasAndFluidInput, GasOutput, WasherRecipe> implements IGasHandler, IFluidHandlerWrapper, ISustainedData, IUpgradeInfoHandler, ITankManager, IAdvancedBoundingBlock {

    public FluidTank fluidTank = new FluidTankSync(8192000);
    public GasTank inputTank = new GasTank(8192000);
    public GasTank outputTank = new GasTank(8192000);

    public WasherRecipe cachedRecipe;
    public double clientEnergyUsed;
    private int currentRedstoneLevel;
    private final EjectSpeedController gasSpeedController = new EjectSpeedController();
    public int processes = MekanismConfig.current().multiblock.LargeChemicalWasherProcesses.val();
    public int numPowering;
    public int updateDelay;
    public boolean needsPacket;

    public TileEntityLargeChemicalWasher() {
        super("washer", "LargeChemicalWasher", 0, MachineType.CHEMICAL_WASHER.getUsage(), 4, 1);
        inventory = NonNullListSynchronized.withSize(5, ItemStack.EMPTY);
        upgradeComponent.setSupported(Upgrade.THREAD);
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (updateDelay > 0) {
            updateDelay--;
            if (updateDelay == 0) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        if (updateDelay > 0) {
            updateDelay--;
            if (updateDelay == 0) {
                needsPacket = true;
            }
        }
        ChargeUtils.discharge(3, this);
        manageBuckets();
        TileUtils.drawGas(inventory.get(2), outputTank);
        WasherRecipe recipe = getRecipe();
        getProcess(recipe, true, energyPerTick, true, false);
        prevEnergy = getEnergy();
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
        if (needsPacket) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            needsPacket = false;
        }
    }

    @Override
    protected void setUpOtherActions() {
        double prev = getEnergy();
        if (getRecipe() != null) {
            setEnergy(getEnergy() - energyPerTick * getUpgradedUsage(getRecipe()));
        }
        clientEnergyUsed = prev - getEnergy();
    }

    @Override
    public void addTileSyncTask() {
        this.gasSpeedController.ensureSize(1, () -> Collections.singletonList(new TankProvider.Gas(outputTank)));
        handleTank(outputTank, getRightTankSide(), facing);
        handleTank(outputTank, getRightTankSide(), MekanismUtils.getRight(facing));
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    private TileEntity getRightTankSide() {
        BlockPos pos = getPos().offset(MekanismUtils.getRight(facing)).offset(facing);
        if (world.getTileEntity(pos) != null) {
            return world.getTileEntity(pos);
        }
        return null;
    }


    private void handleTank(GasTank tank, TileEntity tile, EnumFacing side) {
        if (tile != null) {
            ejectGas(Collections.singleton(side), tank, this.gasSpeedController, tile);
        }
    }

    private void ejectGas(Set<EnumFacing> outputSides, GasTank tank, EjectSpeedController speedController, TileEntity tile) {
        speedController.record(0);
        if (tank.getGas() == null || tank.getStored() <= 0 || tank.getGas().getGas() == null) {
            return;
        }
        if (!speedController.canEject(0)) {
            return;
        }
        GasStack toEmit = tank.getGas().copy().withAmount(Math.min(tank.getMaxGas(), tank.getStored()));
        int emitted = GasUtils.emit(toEmit, tile, outputSides);
        speedController.eject(0, emitted);
        if (emitted <= 0) {
            return;
        }
        tank.draw(emitted, true);
    }

    @Override
    public WasherRecipe getRecipe() {
        GasAndFluidInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getChemicalWasherRecipe(getInput());
        }
        return cachedRecipe;
    }

    @Override
    public GasAndFluidInput getInput() {
        return new GasAndFluidInput(inputTank.getGas(), fluidTank.getFluid());
    }

    @Override
    public boolean canOperate(WasherRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, fluidTank, outputTank);
    }

    @Override
    public void operate(WasherRecipe recipe) {
        recipe.operate(inputTank, fluidTank, outputTank, getUpgradedUsage(recipe));
    }

    @Override
    public Map<GasAndFluidInput, WasherRecipe> getRecipes() {
        return RecipeHandler.Recipe.CHEMICAL_WASHER.get();
    }

    private void manageBuckets() {
        if (FluidContainerUtils.isFluidContainer(inventory.get(0)) && fluidTank.getFluidAmount() != fluidTank.getCapacity()) {
            FluidContainerUtils.handleContainerItemEmpty(this, fluidTank, 0, 1, FluidContainerUtils.FluidChecker.check(FluidRegistry.WATER));
        }
    }

    public int getThread() {
        int thread = 1;
        if (upgradeComponent.isUpgradeInstalled(Upgrade.THREAD)) {
            thread += upgradeComponent.getUpgrades(Upgrade.THREAD);
        }
        return thread;
    }

    public int getUpgradedUsage(WasherRecipe recipe) {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= processes;
        possibleProcess *= getThread();
        possibleProcess = Math.min(Math.min(inputTank.getStored(), outputTank.getNeeded()), possibleProcess);
        possibleProcess = Math.min((int) (getEnergy() / energyPerTick), possibleProcess);
        possibleProcess = Math.max(possibleProcess, 1);
        return Math.min(fluidTank.getFluidAmount() / recipe.getInput().ingredientFluid.amount, possibleProcess);
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, inputTank);
            TileUtils.readTankData(dataStream, outputTank);
            numPowering = dataStream.readInt();
            if (updateDelay == 0) {
                updateDelay = MekanismConfig.current().general.UPDATE_DELAY.val();
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(clientEnergyUsed);
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, inputTank);
        TileUtils.addTankData(data, outputTank);
        data.add(numPowering);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        fluidTank.readFromNBT(nbtTags.getCompoundTag("leftTank"));
        inputTank.read(nbtTags.getCompoundTag("rightTank"));
        outputTank.read(nbtTags.getCompoundTag("centerTank"));
        numPowering = nbtTags.getInteger("numPowering");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("leftTank", fluidTank.writeToNBT(new NBTTagCompound()));
        nbtTags.setTag("rightTank", inputTank.write(new NBTTagCompound()));
        nbtTags.setTag("centerTank", outputTank.write(new NBTTagCompound()));
        nbtTags.setInteger("numPowering", numPowering);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (side == facing || side == MekanismUtils.getLeft(facing)) {
            return inputTank.canReceive(type) && RecipeHandler.Recipe.CHEMICAL_WASHER.containsRecipe(type);
        }
        return false;
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack != null ? stack.getGas() : null)) {
            return inputTank.receive(stack, doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return outputTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return outputTank.canDraw(type) && (side == facing || side == MekanismUtils.getRight(facing));
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{inputTank, outputTank};
    }


    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return InventoryUtils.EMPTY;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return FluidUtil.getFluidContained(itemstack) != null && FluidUtil.getFluidContained(itemstack).getFluid() == FluidRegistry.WATER;
        } else if (slotID == 2) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return !itemstack.isEmpty() && itemstack.getItem() instanceof IGasItem gasItem && gasItem.canProvideGas(itemstack, null);
        } else if (slotID == 2) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return false;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }

    public double getTierEnergy() {
        return MachineType.CHEMICAL_WASHER.getStorage() * processes * getThread();
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        return fluidTank.fill(resource, doFill);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        return RecipeHandler.Recipe.CHEMICAL_WASHER.containsRecipe(fluid.getFluid());
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (inputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "inputTank", inputTank.getGas().write(new NBTTagCompound()));
        }
        if (outputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "outputTank", outputTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        inputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank")));
        outputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank")));
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{fluidTank, inputTank, outputTank};
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(inputTank.getStored(), inputTank.getMaxGas());
    }

    @Override
    public boolean isPowered() {
        return redstone || numPowering > 0;
    }

    @Override
    public String[] getMethods() {
        return new String[0];
    }

    @Override
    public Object[] invoke(int method, Object[] args) throws NoSuchMethodException {
        return new Object[0];
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(3).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
    }


    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 2;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MekanismMultiblockMachine.proxy;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.LargeChemicalWasher.name");
    }

    @Override
    public boolean canBoundReceiveEnergy(BlockPos coord, EnumFacing side) {
        EnumFacing back = MekanismUtils.getBack(facing);
        if (coord.equals(getPos().offset(back))) {
            return side == back;
        }
        return false;
    }

    @Override
    public boolean canBoundOutPutEnergy(BlockPos location, EnumFacing side) {
        return false;
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return side == MekanismUtils.getBack(facing);
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

    @Override
    public void onPlace() {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos pos1 = getPos().add(x, y, z);
                    if (y == 0) {
                        MekanismUtils.makeAdvancedBoundingBlock(world, pos1, Coord4D.get(this));
                    } else {
                        MekanismUtils.makeBoundingBlock(world, pos1, Coord4D.get(this));
                    }
                    world.notifyNeighborsOfStateChange(pos1, getBlockType(), true);
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
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return false;
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return true;
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return hasCapability(capability, side);
    }

    @Nullable
    @Override
    public <T> T getOffsetCapability(@NotNull Capability<T> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        } else if (isStrictEnergy(capability)) {
            return (T) this;
        } else if (isTesla(capability, side)) {
            return (T) getTeslaEnergyWrapper(side);
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(getForgeEnergyWrapper(side));
        }
        return getCapability(capability, side);
    }

    @Override
    public boolean isOffsetCapabilityDisabled(@NotNull Capability<?> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        EnumFacing back = facing.getOpposite();
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.EAST) {
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != facing && side != left;
                }
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != facing && side != right;
                }
            } else if (facing == EnumFacing.SOUTH) {
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != facing && side != left;
                }
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != facing && side != right;
                }
            } else if (facing == EnumFacing.WEST) {
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != facing && side != right;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != facing && side != left;
                }
            } else if (facing == EnumFacing.NORTH) {
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != facing && side != right;
                }
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != facing && side != left;
                }
            }
            return true;
        }
        if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            if (offset.equals(new Vec3i(back.getXOffset(), 0, back.getZOffset()))) {
                return side != back;
            }
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.EAST) {
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != right && side != back;
                }
            } else if (facing == EnumFacing.SOUTH) {
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != right && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != left && side != back;
                }
            } else if (facing == EnumFacing.WEST) {
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != right && side != back;
                }
            } else if (facing == EnumFacing.NORTH) {
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != right && side != back;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return true;
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return false;
    }

    @Override
    public void validate() {
        super.validate();
        if (isRemote()) {
            if (Mekanism.hooks.Bloom && MekanismConfig.current().client.enableBloom.val()) {
                new BloomRenderLargeChemicalWasher(this);
            }
        }
    }


    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (updateDelay == 0) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            updateDelay = 10;
        }
    }
}
