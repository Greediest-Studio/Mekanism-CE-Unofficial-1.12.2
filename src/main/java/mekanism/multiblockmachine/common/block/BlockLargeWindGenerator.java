package mekanism.multiblockmachine.common.block;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeWindGenerator;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class BlockLargeWindGenerator extends BlockLargeBase {

    public BlockLargeWindGenerator() {
        super();
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public int getGuiID() {
        return 3;
    }

    @Override
    public Block getMachineBlock() {
        return MultiblockMachineBlocks.LargeWindGenerator;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityLargeWindGenerator();
    }

    @Override
    public boolean canRotate() {
        return false;
    }
}
