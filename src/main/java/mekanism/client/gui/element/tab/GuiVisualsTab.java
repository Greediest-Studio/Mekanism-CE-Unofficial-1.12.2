package mekanism.client.gui.element.tab;

import mekanism.client.gui.IGuiWrapper;
import mekanism.common.base.IHasVisualization;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class GuiVisualsTab extends GuiTabElement<TileEntity> {

    public GuiVisualsTab(IGuiWrapper gui, TileEntity tile, ResourceLocation def, int y) {
        super(gui, def, tile, y);
    }

    public GuiVisualsTab(IGuiWrapper gui, TileEntity tile, ResourceLocation def) {
        this(gui, tile, def, 6);
    }

    @Override
    public void displayForegroundTooltip(int xAxis, int yAxis) {
        if (tileEntity instanceof IHasVisualization visualization) {
            if (visualization.canDisplayVisuals()) {
                displayTooltip(LangUtils.localize("gui.visuals") + ": " + LangUtils.transOnOff(visualization.isClientRendering()), xAxis, yAxis);
            } else {
                displayTooltips(Arrays.asList(LangUtils.localize("gui.visuals") + ": " + LangUtils.transOnOff(visualization.isClientRendering()),
                        TextFormatting.RED + LangUtils.localize("mekanism.gui.visuals.toobig")), xAxis, yAxis);
            }
        }
    }

    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        super.renderBackground(xAxis, yAxis, guiWidth, guiHeight);
        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.BUTTON_TAB, "button_tab_icon.png"));
        guiObj.drawTexturedRect(guiWidth - 21, guiHeight + yPos + 4, 90, 18, 18, 18);
    }

    @Override
    public void buttonClicked() {
        if (tileEntity instanceof IHasVisualization visualization) {
            visualization.toggleClientRendering();
        }
    }

}
