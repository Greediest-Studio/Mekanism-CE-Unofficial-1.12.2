package mekanism.multiblockmachine.common.block;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeElectrolyticSeparator;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class BlockLargeElectrolyticSeparator extends BlockLargeBase {

    public BlockLargeElectrolyticSeparator() {
        super();
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public int getGuiID() {
        return 0;
    }

    @Override
    public Block getMachineBlock() {
        return MultiblockMachineBlocks.LargeElectrolyticSeparator;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityLargeElectrolyticSeparator();
    }
}
