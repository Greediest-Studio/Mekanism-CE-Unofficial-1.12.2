package mekanism.common.tile.multiblock;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasItem;
import mekanism.common.Mekanism;
import mekanism.common.base.IFluidContainerManager;
import mekanism.common.block.BlockBasic;
import mekanism.common.content.tank.SynchronizedTankData;
import mekanism.common.content.tank.SynchronizedTankData.ValveData;
import mekanism.common.content.tank.TankCache;
import mekanism.common.content.tank.TankUpdateProtocol;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.util.*;
import mekanism.common.util.FluidContainerUtils.ContainerEditMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.Set;

public class TileEntityDynamicTank extends TileEntityMultiblock<SynchronizedTankData> implements IComputerIntegration, IFluidContainerManager {

    protected static final int[] SLOTS = {0, 1};

    public static final String[] methods = new String[]{"getAmount", "getCapacity", "getLiquidType"};

    /**
     * A client-sided set of valves on this tank's structure that are currently active, used on the client for rendering fluids.
     */
    public Set<ValveData> valveViewing = new ObjectOpenHashSet<>();

    /**
     * The capacity this tank has on the client-side.
     */
    public int clientCapacity;

    public float prevScale;

    public TileEntityDynamicTank() {
        this("DynamicTank");
    }

    public TileEntityDynamicTank(String name) {
        super(name);
        inventory = NonNullListSynchronized.withSize(SLOTS.length, ItemStack.EMPTY);
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (clientHasStructure && isRendering) {
            if (structure != null) {
                float targetScale = clientCapacity <= 0 ? 0 : (float) (structure.fluidStored != null ? structure.fluidStored.amount : structure.gasstored != null ? structure.gasstored.amount : 0) / clientCapacity;
                if (Math.abs(prevScale - targetScale) > 0.01) {
                    prevScale = (9 * prevScale + targetScale) / 10;
                }
            }
        } else {
            valveViewing.forEach(data -> {
                TileEntityDynamicTank tileEntity = (TileEntityDynamicTank) data.location.getTileEntity(world);
                if (tileEntity != null) {
                    tileEntity.clientHasStructure = false;
                }
            });
            valveViewing.clear();
        }
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        if (structure != null) {
            if (structure.sanitizeStoredSubstances()) {
                markNoUpdateSync();
            }
            if (isRendering) {
                boolean needsValveUpdate = false;
                for (ValveData data : structure.valves) {
                    if (data.activeTicks > 0) {
                        data.activeTicks--;
                    }
                    if (data.activeTicks > 0 != data.prevActive) {
                        needsValveUpdate = true;
                    }
                    data.prevActive = data.activeTicks > 0;
                }
                if (needsValveUpdate || structure.needsRenderUpdate()) {
                    sendPacketToRenderer();
                }
                structure.prevFluid = structure.fluidStored != null ? structure.fluidStored.copy() : null;
                structure.prevGas = structure.gasstored != null ? structure.gasstored.copy() : null;
                manageInventory();
            }
        }
    }

    //todo
    public void manageInventory() {
        if (structure == null) {
            return;
        }
        ItemStack input = structure.inventory.get(0);
        if (input.isEmpty()) {
            return;
        }

        //Single-medium rules: when gas is present, only process gas items. When fluid is present, only process fluid containers.
        if ((structure.hasGas() || (input.getItem() instanceof IGasItem && !structure.hasFluid())) && handleGasContainerItem()) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            return;
        }

        if (structure.hasGas()) {
            return;
        }

        int neededFluid = (structure.volume * TankUpdateProtocol.FLUID_PER_TANK) - (structure.fluidStored != null ? structure.fluidStored.amount : 0);
        if (FluidContainerUtils.isFluidContainer(input)) {
            FluidStack previousFluid = structure.fluidStored == null ? null : structure.fluidStored.copy();
            ItemStack previousInput = structure.inventory.get(0).copy();
            ItemStack previousOutput = structure.inventory.get(1).copy();
            structure.fluidStored = FluidContainerUtils.handleContainerItem(this, structure.inventory, structure.editMode, structure.fluidStored, neededFluid, 0, 1, null);
            if (structure.fluidStored != null && structure.fluidStored.amount <= 0) {
                structure.fluidStored = null;
            }
            boolean sanitized = structure.sanitizeStoredSubstances();
            boolean changed = sanitized || !fluidStacksEqual(previousFluid, structure.fluidStored)
                    || !ItemStack.areItemStacksEqual(previousInput, structure.inventory.get(0))
                    || !ItemStack.areItemStacksEqual(previousOutput, structure.inventory.get(1));
            if (changed) {
                markNoUpdateSync();
                Mekanism.packetHandler.sendUpdatePacket(this);
            }
        }
    }

    private boolean handleGasContainerItem() {
        ItemStack input = structure.inventory.get(0);
        if (input.isEmpty() || !(input.getItem() instanceof IGasItem gasItem)) {
            return false;
        }
        ItemStack singleInputCopy = StackUtils.size(input.copy(), 1);
        GasStack gasInItem = gasItem.getGas(singleInputCopy);
        int capacity = structure.volume * TankUpdateProtocol.FLUID_PER_TANK;
        boolean changed = false;

        if (structure.editMode == ContainerEditMode.FILL || (structure.editMode == ContainerEditMode.BOTH && gasInItem == null)) {
            if (structure.gasstored == null) {
                return false;
            }
            int added = GasUtils.addGas(singleInputCopy, structure.gasstored);
            if (added > 0) {
                if (!canOutput(singleInputCopy)) {
                    return false;
                }
                structure.gasstored.amount -= added;
                if (structure.gasstored.amount <= 0) {
                    structure.gasstored = null;
                }
                moveInputToOutput(singleInputCopy);
                changed = true;
            }
        } else if (structure.editMode == ContainerEditMode.EMPTY || structure.editMode == ContainerEditMode.BOTH) {
            if (gasInItem == null || !gasItem.canProvideGas(singleInputCopy, gasInItem.getGas()) || structure.hasFluid()) {
                return false;
            }
            Gas type = gasInItem.getGas();
            if (type == null) {
                return false;
            }
            if (structure.gasstored != null && !structure.gasstored.isGasEqual(gasInItem)) {
                return false;
            }
            int needed = capacity - (structure.gasstored != null ? structure.gasstored.amount : 0);
            if (needed <= 0) {
                return false;
            }
            GasStack removed = GasUtils.removeGas(singleInputCopy, type, needed);
            if (removed == null || removed.amount <= 0) {
                return false;
            }
            if (!canOutput(singleInputCopy)) {
                return false;
            }
            if (structure.gasstored == null) {
                structure.gasstored = removed;
            } else {
                structure.gasstored.amount += removed.amount;
            }
            moveInputToOutput(singleInputCopy);
            changed = true;
        }

        if (changed) {
            if (structure.gasstored != null && structure.gasstored.amount > capacity) {
                structure.gasstored.amount = capacity;
            }
            structure.sanitizeStoredSubstances();
            markNoUpdateSync();
        }
        return changed;
    }

    private boolean canOutput(ItemStack resultStack) {
        ItemStack output = structure.inventory.get(1);
        return output.isEmpty() || (ItemHandlerHelper.canItemStacksStack(output, resultStack) && output.getCount() < output.getMaxStackSize());
    }

    private void moveInputToOutput(ItemStack resultStack) {
        if (structure.inventory.get(1).isEmpty()) {
            structure.inventory.set(1, resultStack);
        } else {
            structure.inventory.get(1).grow(1);
        }
        structure.inventory.get(0).shrink(1);
    }

    private static boolean fluidStacksEqual(FluidStack first, FluidStack second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.amount == second.amount && first.isFluidEqual(second);
    }

    @Override
    public boolean onActivate(EntityPlayer player, EnumHand hand, ItemStack stack) {
        if (!player.isSneaking() && structure != null) {
            if (!BlockBasic.manageInventory(player, this, hand, stack)) {
                Mekanism.packetHandler.sendUpdatePacket(this);
                player.openGui(Mekanism.instance, 18, world, getPos().getX(), getPos().getY(), getPos().getZ());
            } else {
                player.inventory.markDirty();
                sendPacketToRenderer();
            }
            return true;
        }
        return false;
    }

    @Override
    protected SynchronizedTankData getNewStructure() {
        return new SynchronizedTankData();
    }

    @Override
    public TankCache getNewCache() {
        return new TankCache();
    }

    @Override
    protected TankUpdateProtocol getProtocol() {
        return new TankUpdateProtocol(this);
    }

    @Override
    public MultiblockManager<SynchronizedTankData> getManager() {
        return Mekanism.tankManager;
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        if (structure != null) {
            data.add(structure.volume * TankUpdateProtocol.FLUID_PER_TANK);
            data.add(structure.editMode.ordinal());
            TileUtils.addFluidStack(data, structure.fluidStored);
            TileUtils.addGasStack(data, structure.gasstored);
            if (isRendering) {
                Set<ValveData> toSend = new ObjectOpenHashSet<>();
                structure.valves.forEach(valveData -> {
                    if (valveData.activeTicks > 0) {
                        toSend.add(valveData);
                    }
                });
                data.add(toSend.size());
                toSend.forEach(valveData -> {
                    valveData.location.write(data);
                    data.add(valveData.side);
                });
            }
        }
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            if (clientHasStructure) {
                clientCapacity = dataStream.readInt();
                structure.editMode = MekanismUtils.getByIndex(ContainerEditMode.values(), dataStream.readInt(), structure.editMode);
                structure.fluidStored = TileUtils.readFluidStack(dataStream);
                structure.gasstored = TileUtils.readGasStack(dataStream);
                structure.sanitizeStoredSubstances();
                if (isRendering) {
                    int size = dataStream.readInt();
                    valveViewing.clear();
                    for (int i = 0; i < size; i++) {
                        ValveData data = new ValveData();
                        data.location = Coord4D.read(dataStream);
                        data.side = EnumFacing.byIndex(dataStream.readInt());
                        valveViewing.add(data);
                        TileEntityDynamicTank tileEntity = (TileEntityDynamicTank) data.location.getTileEntity(world);
                        if (tileEntity != null) {
                            tileEntity.clientHasStructure = true;
                        }
                    }
                }
            }
        }
    }

    public int getScaledFluidLevel(long i) {
        if (clientCapacity == 0 || structure.fluidStored == null) {
            return 0;
        }
        return (int) (structure.fluidStored.amount * i / clientCapacity);
    }

    @Override
    public ContainerEditMode getContainerEditMode() {
        if (structure != null) {
            return structure.editMode;
        }
        return ContainerEditMode.BOTH;
    }

    @Override
    public void setContainerEditMode(ContainerEditMode mode) {
        if (structure == null) {
            return;
        }
        structure.editMode = mode;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return InventoryUtils.EMPTY;
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.isCapabilityDisabled(capability, side);
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] args) throws NoSuchMethodException {
        return switch (method) {
            case 0 ->
                    new Object[]{structure != null ? structure.fluidStored != null ? structure.fluidStored.amount : structure.gasstored != null ? structure.gasstored.amount : 0 : 0};
            case 1 -> new Object[]{structure != null ? structure.volume * TankUpdateProtocol.FLUID_PER_TANK : 0};
            case 2 ->
                    new Object[]{structure != null ? structure.fluidStored != null ? structure.fluidStored.getLocalizedName() : structure.gasstored != null ? structure.gasstored.getGas().getLocalizedName() : null : null};
            default -> throw new NoSuchMethodException();
        };
    }
}
