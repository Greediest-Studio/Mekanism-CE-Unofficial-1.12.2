package mekanism.common.item;

import mekanism.api.EnumColor;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemBlockSuperFumo extends ItemBlock {


    public ItemBlockSuperFumo(Block block) {
        super(block);
        setMaxStackSize(1);
    }


    @Override
    public int getMetadata(int i) {
        return i;
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return EnumColor.ORANGE + super.getItemStackDisplayName(stack);
    }
}
