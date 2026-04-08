package mekanism.multiblockmachine.common.block;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeSolarNeutronActivator;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class BlockLargeSolarNeutronActivator extends BlockLargeBase {

    public BlockLargeSolarNeutronActivator() {
        super();
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public int getGuiID() {
        return 5;
    }

    @Override
    public Block getMachineBlock() {
        return MultiblockMachineBlocks.LargeSolarNeutronActivator;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityLargeSolarNeutronActivator();
    }

}
