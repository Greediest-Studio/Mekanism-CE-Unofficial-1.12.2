package mekanism.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ISpecialSelectionWireframeProvider {

    JsonModelSelectionBoxCache.OutlineBox[] getWireframes(IBlockState state, IBlockAccess world, BlockPos pos);
}

