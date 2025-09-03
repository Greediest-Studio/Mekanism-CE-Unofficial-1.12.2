package mekanism.common.inventory.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * A slot that only accepts tools that declare a specific Forge tool class, e.g., "pickaxe".
 */
public class SlotToolClass extends Slot {

    private final String toolClass;

    public SlotToolClass(IInventory inventory, int index, int x, int y, String toolClass) {
        super(inventory, index, x, y);
        this.toolClass = toolClass;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            return stack.getItem().getToolClasses(stack).contains(toolClass);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
