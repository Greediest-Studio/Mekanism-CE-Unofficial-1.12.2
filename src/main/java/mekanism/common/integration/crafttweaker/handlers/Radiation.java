package mekanism.common.integration.crafttweaker.handlers;

import crafttweaker.annotations.ZenRegister;
import mekanism.api.Coord4D;
import mekanism.api.MekanismAPI;
import mekanism.common.Mekanism;
import mekanism.common.integration.crafttweaker.gas.IGasStack;
import mekanism.common.integration.crafttweaker.helpers.GasHelper;
import mekanism.common.integration.crafttweaker.helpers.IngredientHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.mekanism.Radiation")
@ZenRegister
public class Radiation {

    public static final String NAME = Mekanism.MOD_NAME + " Radiation";

    @ZenMethod
    public static void addRadiation(IGasStack gasInput, double posX, double posY, double posZ, int dimensionId) {
        if (IngredientHelper.checkNotNull(NAME, gasInput)) {
            MekanismAPI.getRadiationManager().dumpRadiation(new Coord4D(posX, posY, posZ, dimensionId), GasHelper.toGas(gasInput));
        }
    }

    @ZenMethod
    public static void addRadiation(double magnitude, double posX, double posY, double posZ, int dimensionId) {
        MekanismAPI.getRadiationManager().radiate(new Coord4D(posX, posY, posZ, dimensionId), magnitude);
    }

}
