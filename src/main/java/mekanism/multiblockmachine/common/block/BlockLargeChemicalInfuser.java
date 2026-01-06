package mekanism.multiblockmachine.common.block;

import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalInfuser;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class BlockLargeChemicalInfuser extends BlockLargeBase {

    public BlockLargeChemicalInfuser() {
        super();
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public int getGuiID() {
        return 1;
    }

    @Override
    public Block getMachineBlock() {
        return MultiblockMachineBlocks.LargeChemicalInfuser;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityLargeChemicalInfuser();
    }
}
