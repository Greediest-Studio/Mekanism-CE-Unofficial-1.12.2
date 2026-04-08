package mekanism.common.item;

import mekanism.common.Resource;
import mekanism.common.base.IMetaItem;
import mekanism.common.util.MekanismUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.Locale;

public class ItemShard extends ItemMekanism implements IMetaItem {

    public ItemShard() {
        super();
        setHasSubtypes(true);
    }

    @Override
    public String getTexture(int meta) {
        return MekanismUtils.getByIndex(Resource.values(), meta, Resource.IRON).getName() + "Shard";
    }

    @Override
    public int getVariants() {
        return Resource.values().length;
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> itemList) {
        if (isInCreativeTab(tabs)) {
            for (int counter = 0; counter < Resource.values().length; counter++) {
                itemList.add(new ItemStack(this, 1, counter));
            }
        }
    }

    @Nonnull
    @Override
    public String getTranslationKey(ItemStack item) {
        Resource resource = MekanismUtils.getByIndex(Resource.values(), item.getItemDamage(), null);
        return resource == null ? "Invalid" : "item." + resource.getName().toLowerCase(Locale.ROOT) + "Shard";
    }
}
