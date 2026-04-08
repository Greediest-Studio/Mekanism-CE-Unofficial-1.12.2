package mekanism.common.inventory.container;

import mekanism.api.gas.IGasItem;
import mekanism.common.inventory.slot.SlotOutput;
import mekanism.common.tile.multiblock.TileEntityDynamicTank;
import mekanism.common.util.FluidContainerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerDynamicTank extends ContainerFluidStorage<TileEntityDynamicTank> {

    public ContainerDynamicTank(InventoryPlayer inventory, TileEntityDynamicTank tile) {
        super(tile, inventory);
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new Slot(tileEntity, 0, 146, 20));
        addSlotToContainer(new SlotOutput(tileEntity, 1, 146, 51));
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID) {
        ItemStack stack = ItemStack.EMPTY;
        Slot currentSlot = inventorySlots.get(slotID);
        if (currentSlot != null && currentSlot.getHasStack()) {
            ItemStack slotStack = currentSlot.getStack();
            stack = slotStack.copy();
            if (FluidContainerUtils.isFluidContainer(slotStack) || slotStack.getItem() instanceof IGasItem) {
                if (slotID != 0 && slotID != 1) {
                    if (!mergeItemStack(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!mergeItemStack(slotStack, 2, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 2 && slotID <= 28) {
                if (!mergeItemStack(slotStack, 29, inventorySlots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID > 28) {
                if (!mergeItemStack(slotStack, 2, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(slotStack, 2, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.getCount() == 0) {
                currentSlot.putStack(ItemStack.EMPTY);
            } else {
                currentSlot.onSlotChanged();
            }
            if (slotStack.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            currentSlot.onTake(player, slotStack);
        }
        return stack;
    }
}
