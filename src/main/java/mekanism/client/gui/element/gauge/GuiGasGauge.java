package mekanism.client.gui.element.gauge;

import mekanism.api.EnumColor;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiGasGauge extends GuiTankGauge<Gas, GasTank> {

    public GuiGasGauge(IGasInfoHandler handler, Type type, IGuiWrapper gui, ResourceLocation def, int x, int y) {
        super(type, gui, def, x, y, handler);
    }

    public static GuiGasGauge getDummy(Type type, IGuiWrapper gui, ResourceLocation def, int x, int y) {
        GuiGasGauge gauge = new GuiGasGauge(null, type, gui, def, x, y);
        gauge.dummy = true;
        return gauge;
    }

    @Override
    public TransmissionType getTransmission() {
        return TransmissionType.GAS;
    }

    @Override
    public int getScaledLevel() {
        if (dummy) {
            return height - 2;
        }
        if (infoHandler.getTank().getGas() == null || infoHandler.getTank().getMaxGas() == 0) {
            return 0;
        }
        double scale = infoHandler.getTank().getStored() / (double) infoHandler.getTank().getMaxGas();
        if (vertical) {
            return MathUtils.clampToInt(Math.round(scale * (height - 2)));
        } else {
            return MathUtils.clampToInt(Math.round(scale * (width - 2)));
        }
    }

    @Override
    public TextureAtlasSprite getIcon() {
        if (dummy) {
            return dummyType.getSprite();
        }
        return (infoHandler.getTank() != null && infoHandler.getTank().getGas() != null && infoHandler.getTank().getGas().getGas() != null) ?
                infoHandler.getTank().getGas().getGas().getSprite() : null;
    }

    @Override
    public String getTooltipText() {
        return "";
    }

    @Override
    public List<String> getTooltipTexts() {
        List<String> list = super.getTooltipTexts();
        if (dummy) {
            list.add(dummyType.getLocalizedName());
        } else {
            GasStack stack = infoHandler.getTank().getGas();
            if (stack != null) {
                list.add(stack.getGas().getLocalizedName() + ": " + infoHandler.getTank().getStored());
                if (stack.getGas().isRadiation()) {
                    list.add(EnumColor.GREY + LangUtils.localize("chemical.mekanism.attribute.radiation") + EnumColor.INDIGO + UnitDisplayUtils.getDisplayShort(stack.getGas().getRadioactivity(), UnitDisplayUtils.RadiationUnit.SVH, 2));
                }
                if (RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(stack.getGas())) {
                    GasStackFuelToEnergyRecipe recipe = RecipeHandler.getGasStackFuelToEnergyRecipe(stack);
                    if (recipe != null) {
                        list.add(LangUtils.localize("chemical.mekanism.attribute.fuel.burn_ticks") + EnumColor.INDIGO + recipe.getInput().ingredient.amount + EnumColor.GREY + " t");
                        list.add(LangUtils.localize("chemical.mekanism.attribute.fuel.energy_density") + EnumColor.INDIGO + MekanismUtils.getEnergyDisplay(recipe.getOutput().energyOutput * recipe.getInput().ingredient.amount));
                    }
                }
            } else {
                list.add(LangUtils.localize("gui.empty"));
            }
        }
        return list;
    }

    @Override
    protected void applyRenderColor() {
        if (dummy) {
            MekanismRenderer.color(dummyType);
        } else {
            MekanismRenderer.color(infoHandler.getTank().getGas());
        }
    }

    public interface IGasInfoHandler extends ITankInfoHandler<GasTank> {
    }
}
