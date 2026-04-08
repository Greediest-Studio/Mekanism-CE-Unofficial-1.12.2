package mekanism.common.block.interfaces;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface IHighlightBoxProvider {

    AxisAlignedBB[] getHighlightBoxes(IBlockState state, IBlockAccess world, BlockPos pos);

    default float getHighlightRed(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0.0F;
    }

    default float getHighlightGreen(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0.0F;
    }

    default float getHighlightBlue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0.0F;
    }

    default float getHighlightAlpha(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0.4F;
    }
}

