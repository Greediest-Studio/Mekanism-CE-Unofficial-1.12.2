package mekanism.common.tile;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import mekanism.api.IConfigCardAccess;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.*;
import mekanism.common.util.FluidContainerUtils.ContainerEditMode;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.IntStream;

public class TileEntityHybridStorage extends TileEntityElectricBlock implements ISideConfiguration, IComputerIntegration,
        ISecurityTile, IConfigCardAccess, IGasHandler, ISustainedData, ITankManager, IFluidHandlerWrapper, IRedstoneControl, IFluidContainerManager {

    private static final int[] INV_SLOTS = IntStream.range(0, 120).toArray();
    private static final boolean[] INV_SLOTS_INPUT_OUTPUT = IntStream.range(0, 120).mapToObj(it -> it >= 0).collect(BooleanArrayList::new, BooleanArrayList::add, BooleanArrayList::addAll).toBooleanArray(new boolean[0]);
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public TileComponentSecurity securityComponent;
    public GasTank gasTank1 = new GasTank(8192000);
    public GasTank gasTank2 = new GasTank(8192000);
    public FluidTank fluidTank = new FluidTankSync(512000);
    public RedstoneControl controlType;
    public ContainerEditMode editMode = ContainerEditMode.BOTH;

    public int delayTicks;
    protected int successCounter = 0;
    protected boolean inventoryChanged = false;


    //TODO：Maybe remove this,Because it's not good to have both input and output in the same place at the same time
    public TileEntityHybridStorage() {
        super(MachineType.HYBRID_STORAGE.getBlockName(), MachineType.HYBRID_STORAGE.getStorage());
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.FLUID, TransmissionType.GAS);

        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, INV_SLOTS));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT, INV_SLOTS));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(INV_SLOTS, INV_SLOTS_INPUT_OUTPUT));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT_ENHANCED, INV_SLOTS));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT_ENHANCED_OUTPUT_ENHANCED, INV_SLOTS,INV_SLOTS_INPUT_OUTPUT));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT_ENHANCED, INV_SLOTS));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{1, 1, 1, 1, 1, 2});

        configComponent.setIOConfig(TransmissionType.ENERGY);
        configComponent.setIOConfig(TransmissionType.FLUID);
        configComponent.getOutputs(TransmissionType.FLUID).get(2).availableSlots = new int[]{0};

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT_1, new int[]{1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT_2, new int[]{2}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(new int[]{1, 2}, new boolean[]{true, true}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT_1, new int[]{1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT_2, new int[]{2}));
        configComponent.setConfig(TransmissionType.GAS, new byte[]{1, 1, 1, 1, 1, 2});

        inventory = NonNullListSynchronized.withSize(128, ItemStack.EMPTY);

        controlType = RedstoneControl.DISABLED;
        securityComponent = new TileComponentSecurity(this);
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.ITEM, configComponent.getOutputs(TransmissionType.ITEM).get(2));
        ejectorComponent.setInputOutputData(TransmissionType.ITEM, configComponent.getOutputs(TransmissionType.ITEM).get(3));
        ejectorComponent.setInputOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(3));
        ejectorComponent.setOutputData(TransmissionType.FLUID, configComponent.getOutputs(TransmissionType.FLUID).get(2));
    }


    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        TileUtils.receiveGasItem(inventory.get(120), gasTank1);
        TileUtils.drawGas(inventory.get(121), gasTank1);
        TileUtils.receiveGasItem(inventory.get(122), gasTank2);
        TileUtils.drawGas(inventory.get(123), gasTank2);
        manageInventory();
        ChargeUtils.charge(126, this);
        ChargeUtils.discharge(127, this);
        if (fluidTank.getFluid() != null && fluidTank.getFluidAmount() == 0) {
            fluidTank.setFluid(null);
        }
    }




    @Override
    public void addTileSyncTask() {
        energyOupt();
        AutomaticallyExtractItems(4);
        AutomaticallyExtractItems(5);
        BetterEjectingItem();
        handleGasTank(gasTank1, configComponent.getSidesForData(TransmissionType.GAS, facing, 4), true);
        handleGasTank(gasTank2, configComponent.getSidesForData(TransmissionType.GAS, facing, 5), true);
    }

    protected boolean canWork(int minWorkDelay, int maxWorkDelay) {
        if (inventoryChanged) {
            inventoryChanged = false;
            return true;
        }

        if (successCounter <= 0) {
            return ticksExisted % maxWorkDelay == 0;
        }
        int workDelay = Math.max(minWorkDelay, maxWorkDelay - (successCounter * 5));
        return ticksExisted % workDelay == 0;
    }

    protected void AutomaticallyExtractItems(int dataIndex) {
        if (getWorld().isRemote || !canWork(5, 60)) {
            return;
        }
        InputItems(dataIndex);
    }

    private void InputItems(int dataIndex) {
        for (EnumFacing facing : configComponent.getSidesForData(TransmissionType.ITEM, facing, dataIndex)) {
            BlockPos offset = getPos().offset(facing);
            TileEntity te = getWorld().getTileEntity(offset);
            if (!InventoryUtils.isItemHandler(te, facing.getOpposite())) {
                continue;
            }
            IItemHandler itemHandler = InventoryUtils.getItemHandler(te, facing.getOpposite());
            if (itemHandler == null) {
                continue;
            }
            inputFromExternal(itemHandler);
        }
    }

    private synchronized void inputFromExternal(IItemHandler external) {
        boolean successAtLeastOnce = false;

        external:
        for (int i = external.getSlots() - 1; i >= 0; i--) {
            ItemStack externalStack = external.getStackInSlot(i);
            if (externalStack.isEmpty()) {
                continue;
            }

            for (int internalSlotId : INV_SLOTS) {
                ItemStack internalStack = inventory.get(internalSlotId);
                int maxCanExtract = Math.min(externalStack.getCount(), externalStack.getMaxStackSize());
                if (internalStack.isEmpty()) {
                    // Extract external item and insert to internal.
                    if (!isItemValidForSlot(internalSlotId, externalStack)) {
                        continue;
                    }
                    ItemStack extracted = external.extractItem(i, maxCanExtract, false);
                    inventory.set(internalSlotId, extracted);
                    successAtLeastOnce = true;
                    // If there are no more items in the current slot, check the next external slot.
                    if (external.getStackInSlot(i).isEmpty()) {
                        continue external;
                    }
                    continue;
                }
                if (internalStack.getCount() >= internalStack.getMaxStackSize() || !matchStacks(internalStack, externalStack)) {
                    continue;
                }
                int extractAmt = Math.min(internalStack.getMaxStackSize() - internalStack.getCount(), maxCanExtract);
                // Extract external item and insert to internal.
                ItemStack extracted = external.extractItem(i, extractAmt, false);
                inventory.set(internalSlotId, copyStackWithSize(extracted, internalStack.getCount() + extracted.getCount()));
                successAtLeastOnce = true;
                // If there are no more items in the current slot, check the next external slot.
                if (external.getStackInSlot(i).isEmpty()) {
                    continue external;
                }
            }
        }

        if (successAtLeastOnce) {
            incrementSuccessCounter(60, 5);
            markNoUpdate();
        } else {
            decrementSuccessCounter();
        }
    }

    protected void incrementSuccessCounter(int maxWorkDelay, int minWorkDelay) {
        int max = (maxWorkDelay - minWorkDelay) / 5;
        if (successCounter < max) {
            successCounter++;
        }
    }

    protected void decrementSuccessCounter() {
        if (successCounter > 0) {
            successCounter--;
        }
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack s = stack.copy();
        s.setCount(amount);
        return s;
    }

    private void BetterEjectingItem() {
        if (delayTicks == 0 || MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
            outputItems(5);
            outputItems(6);
            if (!MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
                delayTicks = MekanismConfig.current().mekce.ItemEjectionDelay.val();
            }
        } else {
            delayTicks--;
        }
    }

    private void outputItems(int dataIndex) {
        if (!configComponent.isEjecting(TransmissionType.ITEM)) {
            return;
        }
        for (EnumFacing facing : configComponent.getSidesForData(TransmissionType.ITEM, facing, dataIndex)) {
            BlockPos offset = getPos().offset(facing);
            TileEntity te = getWorld().getTileEntity(offset);
            if (!InventoryUtils.isItemHandler(te, facing.getOpposite())) {
                continue;
            }
            IItemHandler itemHandler = InventoryUtils.getItemHandler(te, facing.getOpposite());
            if (itemHandler == null) {
                continue;
            }
            try {
                outputToExternal(itemHandler);
            } catch (Exception e) {
                Mekanism.logger.error("Exception when insert item: ", e);
            }
        }
    }

    private synchronized void outputToExternal(IItemHandler external) {
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack externalStack = external.getStackInSlot(externalSlotId);
            int slotLimit = external.getSlotLimit(externalSlotId);
            if (!externalStack.isEmpty() && externalStack.getCount() >= slotLimit) {
                continue;
            }
            for (int internalSlotId : INV_SLOTS) {
                ItemStack internalStack = inventory.get(internalSlotId);
                if (internalStack.isEmpty()) {
                    continue;
                }
                if (externalStack.isEmpty()) {
                    ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                    // Safeguard against Storage Drawers virtual slot
                    if (notInserted.getCount() == internalStack.getCount()) {
                        break;
                    }
                    inventory.set(internalSlotId, notInserted);
                    if (notInserted.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (!matchStacks(internalStack, externalStack)) {
                    continue;
                }
                // Extract internal item to external.
                ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                inventory.set(internalSlotId, notInserted);
                if (notInserted.isEmpty()) {
                    break;
                }
            }
        }

    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStack.areItemsEqual(stack, other)) return false;
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    private void energyOupt() {
        if (configComponent.isEjecting(TransmissionType.ENERGY) && MekanismUtils.canFunction(this) && getEnergy() > 0) {
            CableUtils.emit(this);
        }
    }

    private void handleGasTank(GasTank tank, Set<EnumFacing> side, boolean isGas) {
        if (tank.getGas() != null && tank.getGas().getGas() != null) {
            if (configComponent.isEjecting(TransmissionType.GAS) && (MekanismUtils.canFunction(this) && isGas)) {
                if (tank.getGas().getGas() != null) {
                    GasStack toSend = tank.getGas().copy().withAmount(tank.getStored());
                    tank.draw(GasUtils.emit(toSend, this, side), true);
                }
            }
        }
    }

    private void manageInventory() {
        if (!inventory.get(124).isEmpty()) {
            if (FluidContainerUtils.isFluidContainer(inventory.get(124))) {
                FluidContainerUtils.handleContainerItem(this, editMode, fluidTank, 124, 125);
            }
        }
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        Item gasitem = itemstack.getItem();
        if (slotID <= 119) {
            return true;
        } else if (slotID == 121 || slotID == 123) {
            return gasitem instanceof IGasItem item && item.getGas(itemstack) != null && item.getGas(itemstack).amount == item.getMaxGas(itemstack);
        } else if (slotID == 120 || slotID == 122) {
            return gasitem instanceof IGasItem item && item.getGas(itemstack) == null;
        } else if (slotID == 125) {
            return true;
        } else if (slotID == 126) {
            return ChargeUtils.canBeOutputted(itemstack, true);
        } else if (slotID == 127) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        Item item = itemstack.getItem();
        if (slotID <= 119) {
            return true;
        } else if (slotID == 120) {
            return item instanceof IGasItem gasItem && (gasTank1.getGas() == null || gasItem.canProvideGas(itemstack, gasTank1.getGas().getGas()));
        } else if (slotID == 121) {
            return item instanceof IGasItem gasItem && (gasTank1.getGas() == null || gasItem.canReceiveGas(itemstack, gasTank1.getGas().getGas()));
        } else if (slotID == 122) {
            return item instanceof IGasItem gasItem && (gasTank2.getGas() == null || gasItem.canProvideGas(itemstack, gasTank2.getGas().getGas()));
        } else if (slotID == 123) {
            return item instanceof IGasItem gasItem && (gasTank2.getGas() == null || gasItem.canReceiveGas(itemstack, gasTank2.getGas().getGas()));
        } else if (slotID == 124) {
            return FluidContainerUtils.isFluidContainer(itemstack);
        } else if (slotID == 126) {
            return ChargeUtils.canBeCharged(itemstack);
        } else if (slotID == 127) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return false;
    }

    @Override
    public TileComponentConfig getConfig() {
        return configComponent;
    }

    @Override
    public EnumFacing getOrientation() {
        return facing;
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, 1, side);
    }

    @Override
    public boolean sideIsOutput(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, 2, side);
    }

    @Override
    public double getMaxOutput() {
        return MachineType.HYBRID_STORAGE.getStorage();
    }

    @Override
    public double getMaxEnergy() {
        return MachineType.HYBRID_STORAGE.getStorage();
    }

    @Override
    public void setEnergy(double energy) {
        super.setEnergy(energy);
    }

    @Override
    public TileComponentEjector getEjector() {
        return ejectorComponent;
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
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (configComponent.isCapabilityDisabled(capability, side, facing)) {
            return true;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return side != null && configComponent.isCapabilityDisabled(capability, side, facing);
        }
        return super.isCapabilityDisabled(capability, side);
    }


    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIG_CARD_CAPABILITY ||
                capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == Capabilities.CONFIG_CARD_CAPABILITY) {
            return Capabilities.CONFIG_CARD_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        }
        return super.getCapability(capability, side);
    }

    @NotNull
    @Override
    public int[] getSlotsForFace(@NotNull EnumFacing side) {
        return configComponent.getOutput(TransmissionType.ITEM, side, facing).availableSlots;
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, gasTank1);
        TileUtils.addTankData(data, gasTank2);
        data.add(controlType.ordinal());
        data.add(editMode.ordinal());
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, gasTank1);
            TileUtils.readTankData(dataStream, gasTank2);
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), controlType);
            editMode = MekanismUtils.getByIndex(ContainerEditMode.values(), dataStream.readInt(), editMode);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        gasTank1.read(nbtTags.getCompoundTag("gasTank1"));
        gasTank2.read(nbtTags.getCompoundTag("gasTank2"));
        controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), controlType);
        editMode = MekanismUtils.getByIndex(ContainerEditMode.values(), nbtTags.getInteger("editMode"), editMode);
    }


    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
        nbtTags.setTag("gasTank1", gasTank1.write(new NBTTagCompound()));
        nbtTags.setTag("gasTank2", gasTank2.write(new NBTTagCompound()));
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setInteger("editMode", editMode.ordinal());
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack != null ? stack.getGas() : null)) {
            if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1, 2)) {
                if (stack != null) {
                    if (gasTank1.canReceive(stack.getGas()) && gasTank2.getGasType() != stack.getGas()) {
                        return gasTank1.receive(stack, doTransfer);
                    }
                    if (gasTank2.canReceive(stack.getGas()) && gasTank1.getGasType() != stack.getGas()) {
                        return gasTank2.receive(stack, doTransfer);
                    }
                }
            } else {
                return getTank(side).receive(stack, doTransfer);
            }
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return getTank(side).draw(amount, doTransfer);
        }
        return null;
    }

    public GasTank getTank(EnumFacing side) {
        if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1)) {
            return gasTank1;
        } else if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(2)) {
            return gasTank2;
        }
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1, 2)) {
            return gasTank1.canReceive(type) || gasTank2.canReceive(type);
        }
        return getTank(side) != null && getTank(side).canReceive(type);
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return getTank(side) != null && getTank(side).getGas() != null && getTank(side).stored.getGas() == type;
    }


    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (gasTank1.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "gasTank1", gasTank1.getGas().write(new NBTTagCompound()));
        }
        if (gasTank2.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "gasTank2", gasTank2.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        gasTank1.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "gasTank1")));
        gasTank2.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "gasTank2")));
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{gasTank1, gasTank2};
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{fluidTank, gasTank1, gasTank2};
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        return fluidTank.fill(resource, doFill);
    }

    @Override
    @Nullable
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return fluidTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        if (configComponent.getOutput(TransmissionType.FLUID, from, facing).ioState == SideData.IOState.INPUT) {
            return FluidContainerUtils.canFill(fluidTank.getFluid(), fluid);
        }
        return false;
    }

    @Override
    public boolean canDrain(EnumFacing from, @Nullable FluidStack fluid) {
        if (configComponent.getOutput(TransmissionType.FLUID, from, facing).ioState == SideData.IOState.OUTPUT) {
            return FluidContainerUtils.canDrain(fluidTank.getFluid(), fluid);
        }
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if (configComponent.getOutput(TransmissionType.FLUID, from, facing).ioState != SideData.IOState.OFF) {
            return new FluidTankInfo[]{fluidTank.getInfo()};
        }
        return PipeUtils.EMPTY;
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(RedstoneControl type) {
        controlType = type;
    }

    @Override
    public boolean canPulse() {
        return false;
    }

    @Override
    public ContainerEditMode getContainerEditMode() {
        return editMode;
    }

    @Override
    public void setContainerEditMode(ContainerEditMode mode) {
        editMode = mode;
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return MachineType.get(block, metadata) != null ? MachineType.get(block, metadata).guiId : -1;
    }

    @Override
    protected boolean shouldDumpRadiation() {
        return true;
    }
}
