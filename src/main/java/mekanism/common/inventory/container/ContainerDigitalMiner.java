package mekanism.common.inventory.container;

import mekanism.common.inventory.slot.SlotEnergy.SlotDischarge;
import mekanism.common.inventory.slot.SlotToolClass;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.util.ChargeUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerDigitalMiner extends ContainerMekanism<TileEntityDigitalMiner> {

    public ContainerDigitalMiner(InventoryPlayer inventory, TileEntityDigitalMiner tile) {
        super(tile, inventory);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID) {
        ItemStack stack = ItemStack.EMPTY;
        Slot currentSlot = inventorySlots.get(slotID);
        final int STORAGE_START = 0;           // 0..26
        final int STORAGE_END_EXCL = 27;       // exclusive end for storage area
        final int ENERGY_SLOT = 27;            // discharge slot
        final int PICKAXE_SLOT = 28;           // marker pickaxe slot
        final int PLAYER_START = 29;           // first player inventory slot index in this container
        if (currentSlot != null && currentSlot.getHasStack()) {
            ItemStack slotStack = currentSlot.getStack();
            stack = slotStack.copy();
            boolean isEnergy = ChargeUtils.canBeDischarged(slotStack);
            boolean isPickaxe = false;
            try {
                isPickaxe = slotStack.getItem().getToolClasses(slotStack).contains("pickaxe");
            } catch (Throwable ignored) {}

            if (slotID >= PLAYER_START) {
                // Moving from player inventory to machine
                if (isEnergy) {
                    if (!mergeItemStack(slotStack, ENERGY_SLOT, ENERGY_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isPickaxe) {
                    if (!mergeItemStack(slotStack, PICKAXE_SLOT, PICKAXE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!mergeItemStack(slotStack, STORAGE_START, STORAGE_END_EXCL, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                // Moving from machine to player inventory
                if (!mergeItemStack(slotStack, PLAYER_START, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
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

    @Override
    protected void addSlots() {
        for (int slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                addSlotToContainer(new Slot(tileEntity, slotX + slotY * 9, 8 + slotX * 18, 92 + slotY * 18));
            }
        }
        addSlotToContainer(new SlotDischarge(tileEntity, 27, 152, 20));
    // New pickaxe slot on the left side
    addSlotToContainer(new SlotToolClass(tileEntity, 28, 7, 19, "pickaxe"));
    }

    @Override
    protected int getInventorYOffset() {
        return 160;
    }
}
