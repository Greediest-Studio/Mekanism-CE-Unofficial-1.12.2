package mekanism.multiblockmachine.common.block;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalWasher;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class BlockLargeChemicalWasher extends BlockLargeBase {

    public BlockLargeChemicalWasher() {
        super();
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public int getGuiID() {
        return 2;
    }

    @Override
    public Block getMachineBlock() {
        return MultiblockMachineBlocks.LargeChemicalWasher;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityLargeChemicalWasher();
    }
}
