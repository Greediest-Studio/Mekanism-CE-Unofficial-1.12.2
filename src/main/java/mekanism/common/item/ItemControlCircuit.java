package mekanism.common.item;

import mekanism.common.base.IMetaItem;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.MekanismUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

public class ItemControlCircuit extends ItemMekanism implements IMetaItem {

    public ItemControlCircuit() {
        super();
        setHasSubtypes(true);
    }

    @Override
    public String getTexture(int meta) {
        return MekanismUtils.getByIndex(BaseTier.values(), meta, BaseTier.BASIC).getSimpleName() + "ControlCircuit";
    }

    @Override
    public int getVariants() {
        return BaseTier.values().length;
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> itemList) {
        if (isInCreativeTab(tabs)) {
            for (BaseTier tier : BaseTier.values()) {
                itemList.add(new ItemStack(this, 1, tier.ordinal()));
            }
        }
    }

    @Nonnull
    @Override
    public String getTranslationKey(ItemStack item) {
        return "item." + MekanismUtils.getByIndex(BaseTier.values(), item.getItemDamage(), BaseTier.BASIC).getSimpleName() + "ControlCircuit";
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack itemstack) {
        return MekanismUtils.getByIndex(BaseTier.values(), itemstack.getItemDamage(), BaseTier.BASIC).getColor() + super.getItemStackDisplayName(itemstack);
    }
}
