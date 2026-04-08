package mekanism.common.content.gear.mekasuit;

import mekanism.api.annotations.ParametersAreNotNullByDefault;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.common.MekanismModules;
import mekanism.common.util.LangUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


@ParametersAreNotNullByDefault
public class ModuleElytraUnit implements ICustomModule<ModuleElytraUnit> {

    @Override
    public boolean canChangeModeWhenDisabled(IModule<ModuleElytraUnit> module) {
        return true;
    }

    @Override
    public void changeMode(IModule<ModuleElytraUnit> module, EntityPlayer player, ItemStack stack, int shift, boolean displayChangeMessage) {
        module.toggleEnabled(player, LangUtils.localize(MekanismModules.ELYTRA_UNIT.getTranslationKey()));
    }
}