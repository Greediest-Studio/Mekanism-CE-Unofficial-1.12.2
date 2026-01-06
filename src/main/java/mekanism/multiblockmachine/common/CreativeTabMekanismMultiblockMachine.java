package mekanism.multiblockmachine.common;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class CreativeTabMekanismMultiblockMachine extends CreativeTabs {

    public CreativeTabMekanismMultiblockMachine() {
        super("tabMekanismMultiblockMachine");
    }


    @Override
    public ItemStack createIcon() {
        return new ItemStack(MultiblockMachineBlocks.LargeElectrolyticSeparator);
    }
}
