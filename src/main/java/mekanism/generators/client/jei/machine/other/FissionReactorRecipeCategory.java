package mekanism.generators.client.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraftforge.fluids.FluidStack;

public class FissionReactorRecipeCategory extends BaseRecipeCategory<FissionReactorRecipeWrapper> {

    public static final String UID = "mekanismgenerators.fission_reactor";

    public FissionReactorRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", UID, "gui.fissionReactor", null, 6, 13, 184, 60);
    }

    @Override
    protected void addGuiElements() {
        guiElements.add(new GuiInnerScreen(this, guiLocation, 45, 17, 105, 56));
        guiElements.add(GuiFluidGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 6, 13).withColor(GuiGauge.TypeColor.BLUE));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 25, 13).withColor(GuiGauge.TypeColor.RED));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 152, 13).withColor(GuiGauge.TypeColor.ORANGE));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 171, 13).withColor(GuiGauge.TypeColor.YELLOW));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, FissionReactorRecipeWrapper recipeWrapper, IIngredients ingredients) {
        int gasIndex = 0;
        int fluidIndex = 0;
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();

        GasStack coolantInputGas = recipeWrapper.getCoolantInputGas();
        FluidStack coolantInputFluid = recipeWrapper.getCoolantInputFluid();
        if (coolantInputGas != null) {
            initGas(gasStacks, gasIndex++, true, 7 - xOffset, 14 - yOffset, 16, 58, coolantInputGas, true);
        } else if (coolantInputFluid != null) {
            fluidStacks.init(fluidIndex, true, 7 - xOffset, 14 - yOffset, 16, 58, Math.max(1, coolantInputFluid.amount), false, fluidOverlayLarge);
            fluidStacks.set(fluidIndex++, coolantInputFluid);
        }

        initGas(gasStacks, gasIndex++, true, 26 - xOffset, 14 - yOffset, 16, 58, recipeWrapper.getFuelInput(), true);

        GasStack heatedOutputGas = recipeWrapper.getHeatedCoolantOutputGas();
        FluidStack heatedOutputFluid = recipeWrapper.getHeatedCoolantOutputFluid();
        if (heatedOutputGas != null) {
            initGas(gasStacks, gasIndex++, false, 153 - xOffset, 14 - yOffset, 16, 58, heatedOutputGas, true);
        } else if (heatedOutputFluid != null) {
            fluidStacks.init(fluidIndex, false, 153 - xOffset, 14 - yOffset, 16, 58, Math.max(1, heatedOutputFluid.amount), false, fluidOverlayLarge);
            fluidStacks.set(fluidIndex++, heatedOutputFluid);
        }

        initGas(gasStacks, gasIndex, false, 172 - xOffset, 14 - yOffset, 16, 58, recipeWrapper.getWasteOutput(), true);
    }
}
