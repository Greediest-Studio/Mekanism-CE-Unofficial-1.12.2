package mekanism.generators.client.gui.element;

import mekanism.api.Coord4D;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.GuiTabElementType;
import mekanism.client.gui.element.tab.TabType;
import mekanism.common.Mekanism;
import mekanism.common.base.IGuiProvider;
import mekanism.common.network.PacketSimpleGui;
import mekanism.common.network.PacketSimpleGui.SimpleGuiMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.client.gui.element.GuiFissionReactorTab.FissionReactorTab;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiFissionReactorTab extends GuiTabElementType<TileEntityFissionReactorCasing, FissionReactorTab> {

    private final FissionReactorTab tab;

    public GuiFissionReactorTab(IGuiWrapper gui, TileEntityFissionReactorCasing tile, FissionReactorTab type, ResourceLocation def) {
        super(gui, tile, type, def);
        tab = type;
    }

    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        super.renderBackground(xAxis, yAxis, guiWidth, guiHeight);
        mc.renderEngine.bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.BUTTON_TAB, "button_tab_icon.png"));
        guiObj.drawTexturedRect(guiWidth - 21, guiHeight + tab.getYPos() + 4, tab.xLocation, tab.yLocation, 18, 18);
    }

    public enum FissionReactorTab implements TabType {
        MAIN(162, 0, 16, "gui.main", 6),
        STAT(198, 18, 17, "gui.stats", 34);

        private final String description;
        public final int xLocation;
        public final int yLocation;
        private final int guiId;
        private final int yPos;

        FissionReactorTab(int x, int y, int id, String desc, int yPos) {
            xLocation = x;
            yLocation = y;
            guiId = id;
            description = desc;
            this.yPos = yPos;
        }

        @Override
        public ResourceLocation getResource() {
            return MekanismUtils.getResource(ResourceType.GUI, "Null.png");
        }

        @Override
        public void openGui(TileEntity tile) {
            List<IGuiProvider> handlers = PacketSimpleGui.handlers;
            int hand = handlers.indexOf(MekanismGenerators.proxy);
            Mekanism.packetHandler.sendToServer(new SimpleGuiMessage(Coord4D.get(tile), hand, guiId));
        }

        @Override
        public String getDesc() {
            return LangUtils.localize(description);
        }

        @Override
        public int getYPos() {
            return yPos;
        }
    }
}
