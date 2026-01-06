package mekanism.client.jei.machine.other;

import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.gauge.GuiNumberGauge;
import mekanism.client.gui.element.gauge.GuiNumberGauge.INumberInfoHandler;
import mekanism.client.gui.element.slot.GuiInputSlot;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.recipe.ItemStackToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.util.MekanismUtils;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ItemStackToEnergyRecipeCategory<WRAPPER extends ItemStackToEnergyRecipeWrapper<ItemStackToEnergyRecipe>> extends BaseRecipeCategory<WRAPPER> {

    public IGuiHelper helper;

    public ItemStackToEnergyRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.ENERGY_RECIPE.getJEICategory(),
                "conversion.mekanism.energy", GuiProgress.ProgressBar.LARGE_RIGHT, 20, 12, 132, 62);
        this.helper = helper;
    }

    @Override
    protected void addGuiElements() {
        guiElements.add(new GuiNumberGauge(new INumberInfoHandler() {
            @Override
            public TextureAtlasSprite getIcon() {
                return MekanismRenderer.energyIcon;
            }

            @Override
            public double getLevel() {
                return 1D;
            }

            @Override
            public double getMaxLevel() {
                return 1D;
            }

            @Override
            public String getText(double level) {
                return "";
            }
        }, GuiGauge.Type.STANDARD, this, guiLocation, 131, 13));
        guiElements.add(new GuiInputSlot(this, guiLocation, 25, 35));
        guiElements.add(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return (double) timer.getValue() / 20F;
            }
        }, progressBar, this, guiLocation, 62, 39, false));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WRAPPER recipeWrapper, IIngredients ingredients) {
        ItemStackToEnergyRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 25 - xOffset, 35 - yOffset);
        itemStacks.set(0, tempRecipe.getInput().ingredient);
    }

    @Override
    public IDrawable getIcon() {
        return createIcon(helper, MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "energy.png"));
    }
}
