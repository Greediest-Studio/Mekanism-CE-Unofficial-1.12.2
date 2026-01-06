package mekanism.common.integration.crafttweaker.handlers;

import crafttweaker.annotations.ZenRegister;
import mekanism.api.Coord4D;
import mekanism.common.Mekanism;
import mekanism.common.integration.crafttweaker.gas.IGasStack;
import mekanism.common.integration.crafttweaker.helpers.GasHelper;
import mekanism.common.integration.crafttweaker.helpers.IngredientHelper;
import mekanism.common.lib.radiation.RadiationManager;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.mekanism.Radiation")
@ZenRegister
public class Radiation {

    public static final String NAME = Mekanism.MOD_NAME + " Radiation";

    @ZenMethod
    public static void addRadiation(IGasStack gasInput, double posX, double posY, double posZ, int dimensionId) {
        if (IngredientHelper.checkNotNull(NAME, gasInput)) {
            RadiationManager.INSTANCE.dumpRadiation(new Coord4D(posX, posY, posZ, dimensionId), GasHelper.toGas(gasInput));
        }
    }

}
