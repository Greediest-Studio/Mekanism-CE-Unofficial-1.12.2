package mekanism.common.content.gear.mekafishrod;

import mekanism.api.annotations.ParametersAreNotNullByDefault;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.common.MekanismModules;
import mekanism.common.util.LangUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

@ParametersAreNotNullByDefault
public class ModuleintelligentUnit implements ICustomModule<ModuleintelligentUnit> {

    @Override
    public boolean canChangeModeWhenDisabled(IModule<ModuleintelligentUnit> module) {
        return true;
    }

    @Override
    public void changeMode(IModule<ModuleintelligentUnit> module, EntityPlayer player, ItemStack stack, int shift, boolean displayChangeMessage) {
        module.toggleEnabled(player, LangUtils.localize(MekanismModules.FISHING_INTELLIGENT_UNIT.getTranslationKey()));
    }
}
