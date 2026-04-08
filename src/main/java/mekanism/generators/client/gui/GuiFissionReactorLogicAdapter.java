package mekanism.generators.client.gui;

import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorLogicAdapter;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorLogicAdapter.LogicMode;
import mekanism.generators.common.util.MekanismGeneratorUtils;
import mekanism.generators.common.util.MekanismGeneratorUtils.ResourceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiFissionReactorLogicAdapter extends GuiMekanismTile<TileEntityFissionReactorLogicAdapter> {

    private static final int DISPLAY_COUNT = 4;

    private final List<GuiButton> modeButtons = new ArrayList<>();
    private GuiButton scrollUpButton;
    private GuiButton scrollDownButton;
    private int scrollOffset;

    public GuiFissionReactorLogicAdapter(InventoryPlayer inventory, TileEntityFissionReactorLogicAdapter tile) {
        super(tile, new ContainerNull(inventory.player, tile));
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        modeButtons.clear();

        scrollUpButton = new GuiButton(0, guiLeft + 153, guiTop + 31, 14, 11, "^");
        scrollDownButton = new GuiButton(1, guiLeft + 153, guiTop + 110, 14, 11, "v");
        buttonList.add(scrollUpButton);
        buttonList.add(scrollDownButton);

        int buttonID = 2;
        for (int i = 0; i < DISPLAY_COUNT; i++) {
            GuiButton button = new GuiButton(buttonID++, guiLeft + 24, guiTop + 32 + i * 22, 126, 20, "");
            modeButtons.add(button);
            buttonList.add(button);
        }
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button.id == 0) {
            scrollOffset--;
            updateButtons();
            return;
        }
        if (button.id == 1) {
            scrollOffset++;
            updateButtons();
            return;
        }
        for (int i = 0; i < modeButtons.size(); i++) {
            if (modeButtons.get(i).id == button.id) {
                LogicMode mode = getModeForSlot(i);
                if (mode != null) {
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(0, mode.ordinal())));
                }
                break;
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtons();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = LangUtils.localize("gui.fissionLogicAdapter");
        fontRenderer.drawString(title, (xSize / 2) - (fontRenderer.getStringWidth(title) / 2), 6, 0x404040);

        renderScaledText(LangUtils.localize("fission.logic.mode") + ": " + EnumColor.RED + tileEntity.getLogicMode().getLabel(), 24, 124, 0x404040, 126);
        String status = LangUtils.localize("gui.status") + ": " + EnumColor.RED + LangUtils.localize(tileEntity.getStatusTranslationKey());
        fontRenderer.drawString(status, (xSize / 2) - (fontRenderer.getStringWidth(status) / 2), 136, 0x404040);

        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        for (int i = 0; i < modeButtons.size(); i++) {
            LogicMode mode = getModeForSlot(i);
            if (mode == null) {
                continue;
            }
            if (mode == tileEntity.getLogicMode()) {
                int x = 24;
                int y = 32 + i * 22;
                int w = 126;
                int h = 20;
                drawRect(x - 1, y - 1, x + w + 1, y, 0xFF48DDA6);
                drawRect(x - 1, y + h, x + w + 1, y + h + 1, 0xFF48DDA6);
                drawRect(x - 1, y, x, y + h, 0xFF48DDA6);
                drawRect(x + w, y, x + w + 1, y + h, 0xFF48DDA6);
            }
            int y = 34 + i * 22;
            renderItem(mode.getRenderStack(), 27, y);
            if (modeButtons.get(i).isMouseOver()) {
                displayTooltips(MekanismUtils.splitTooltip(mode.getDescription(), ItemStack.EMPTY), xAxis, yAxis);
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismGeneratorUtils.getResource(ResourceType.GUI, "GuiReactorLogicAdapter.png");
    }

    private int getMaxScroll() {
        return Math.max(0, LogicMode.values().length - DISPLAY_COUNT);
    }

    private void updateButtons() {
        scrollOffset = Math.max(0, Math.min(getMaxScroll(), scrollOffset));
        if (scrollUpButton != null) {
            scrollUpButton.enabled = scrollOffset > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.enabled = scrollOffset < getMaxScroll();
        }
        for (int i = 0; i < modeButtons.size(); i++) {
            GuiButton button = modeButtons.get(i);
            LogicMode mode = getModeForSlot(i);
            button.visible = mode != null;
            button.enabled = mode != null && mode != tileEntity.getLogicMode();
            button.displayString = mode == null ? "" : mode.getLabel();
        }
    }

    private LogicMode getModeForSlot(int slot) {
        LogicMode[] modes = LogicMode.values();
        int index = scrollOffset + slot;
        if (index < 0 || index >= modes.length) {
            return null;
        }
        return modes[index];
    }
}
