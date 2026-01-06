package mekanism.client.gui.element.tab;

import mekanism.api.Coord4D;
import mekanism.client.gui.IGuiWrapper;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketSimpleGui.SimpleGuiMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiTransporterConfigTab extends GuiTabElement<TileEntity> {

    private int y;

    public GuiTransporterConfigTab(IGuiWrapper gui, int y, TileEntity tile, ResourceLocation def) {
        super(gui, def, tile, y);
        this.y = y;
    }

    public GuiTransporterConfigTab(IGuiWrapper gui, TileEntity tile, ResourceLocation def) {
        this(gui, 34, tile, def);
    }

    @Override
    public void buttonClicked() {
        Mekanism.packetHandler.sendToServer(new SimpleGuiMessage(Coord4D.get(tileEntity), 0, 51));
    }

    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        super.renderBackground(xAxis, yAxis, guiWidth, guiHeight);
        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.BUTTON_TAB, "button_tab_icon.png"));
        guiObj.drawTexturedRect(guiWidth - 21, guiHeight + y + 4, 54, 18, 18, 18);
    }

    @Override
    public void displayForegroundTooltip(int xAxis, int yAxis) {
        displayTooltip(LangUtils.localize("gui.configuration.transporter"), xAxis, yAxis);
    }
}
