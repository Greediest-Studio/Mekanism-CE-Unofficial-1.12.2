package mekanism.common.inventory;

import mekanism.common.lib.inventory.HashedItem;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public interface ISlotClickHandler {

    void onClick(IScrollableSlot slot, int button, boolean hasShiftDown, ItemStack heldItem);

    interface IScrollableSlot {

        HashedItem getItem();

        UUID getItemUUID();

        long getCount();

        String getDisplayName();

        String getModID();
    }
}
