package mekanism.multiblockmachine.common.block;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeGasGenerator;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class BlockLargeGasGenerator extends BlockLargeBase {

    public BlockLargeGasGenerator() {
        super();
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public int getGuiID() {
        return 4;
    }

    @Override
    public Block getMachineBlock() {
        return MultiblockMachineBlocks.LargeGasGenerator;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityLargeGasGenerator();
    }

}
