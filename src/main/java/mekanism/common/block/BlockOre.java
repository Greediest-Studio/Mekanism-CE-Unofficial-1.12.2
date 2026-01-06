package mekanism.common.block;

import mekanism.common.Mekanism;
import mekanism.common.MekanismItems;
import mekanism.common.block.states.BlockStateOre;
import mekanism.common.block.states.BlockStateOre.EnumOreType;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Block class for handling multiple ore block IDs. 0: Osmium Ore 1: Copper Ore 2: Tin Ore
 *
 * @author AidanBrady
 */
public class BlockOre extends Block {

    public BlockOre() {
        super(Material.ROCK);
        setHardness(3F);
        setResistance(5F);
        setCreativeTab(Mekanism.tabMekanism);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateOre(this);
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BlockStateOre.typeProperty, EnumOreType.values()[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockStateOre.typeProperty).ordinal();
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public void getSubBlocks(CreativeTabs creativetabs, NonNullList<ItemStack> list) {
        for (EnumOreType ore : EnumOreType.values()) {
            list.add(new ItemStack(this, 1, ore.ordinal()));
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (state.getValue(BlockStateOre.typeProperty).ordinal() == EnumOreType.FLUORITE.ordinal()) {
            return MekanismItems.FluoriteClump;
        } else {
            return super.getItemDropped(state, rand, fortune);
        }
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        if (getItemDropped(state, RANDOM, fortune) == MekanismItems.FluoriteClump) {
            return MathHelper.getInt(RANDOM, 1, 4);
        } else {
            return super.getExpDrop(state, world, pos, fortune);
        }
    }


    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        if (getItemDropped(state, RANDOM, fortune) == MekanismItems.FluoriteClump) {
            return MathHelper.getInt(random, 2, 4) + random.nextInt(fortune + 1);
        } else {
            return super.quantityDropped(state, fortune, random);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random rand = world instanceof World isWorld ? isWorld.rand : RANDOM;
        int count = this.quantityDropped(state, fortune, rand);
        for (int i = 0; i < count; i++) {
            Item item = this.getItemDropped(state, rand, fortune);
            if (item != Items.AIR) {
                drops.add(new ItemStack(item, 1, item == MekanismItems.FluoriteClump ? 0 : this.damageDropped(state)));
            }
        }
    }


}
