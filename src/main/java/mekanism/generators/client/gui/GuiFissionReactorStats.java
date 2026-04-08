package mekanism.generators.client.gui;

import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.button.GuiDisableableButton;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiRateBarHorizontal;
import mekanism.client.gui.element.GuiRateBarHorizontal.IRateInfoHandler;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.UnitDisplayUtils;
import mekanism.generators.client.gui.element.GuiFissionReactorTab;
import mekanism.generators.client.gui.element.GuiFissionReactorTab.FissionReactorTab;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiFissionReactorStats extends GuiMekanismTile<TileEntityFissionReactorCasing> {

    private static final int RATE_WIDGET_X = 105;
    private static final int RATE_WIDGET_WIDTH = 84;
    private static final int RATE_FIELD_Y = 128;
    private static final int RATE_BUTTON_Y = 127;
    private GuiTextField rateLimitField;
    private GuiDisableableButton setRateButton;

    public GuiFissionReactorStats(InventoryPlayer inventory, TileEntityFissionReactorCasing tile) {
        super(tile, new ContainerNull(inventory.player, tile));
        xSize = 195;
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiFissionReactorTab(this, tileEntity, FissionReactorTab.MAIN, resource));
        addGuiElement(new GuiInnerScreen(this, resource, RATE_WIDGET_X, RATE_BUTTON_Y, RATE_WIDGET_WIDTH, 13));
        addGuiElement(new GuiRateBarHorizontal(this, new IRateInfoHandler() {
            @Override
            public String getTooltip() {
                if (tileEntity.structure == null) {
                    return LangUtils.localize("gui.burnRate") + ": 0 /t";
                }
                return LangUtils.localize("gui.burnRate") + ": " + UnitDisplayUtils.roundDecimals(tileEntity.structure.lastBurnRate) + " /t";
            }

            @Override
            public double getLevel() {
                if (tileEntity.structure == null) {
                    return 0;
                }
                double max = tileEntity.structure.getMaxBurnRate();
                return max <= 0 ? 0 : Math.min(1, tileEntity.structure.lastBurnRate / max);
            }
        }, resource, RATE_WIDGET_X, 113));
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        String previousRate = rateLimitField != null ? rateLimitField.getText() : "";
        rateLimitField = new GuiTextField(0, fontRenderer, guiLeft + RATE_WIDGET_X + 2, guiTop + RATE_FIELD_Y, RATE_WIDGET_WIDTH - 3, 11);
        rateLimitField.setMaxStringLength(6);
        rateLimitField.setEnableBackgroundDrawing(false);
        rateLimitField.setText(previousRate);
        buttonList.add(setRateButton = new GuiDisableableButton(0, guiLeft + RATE_WIDGET_X + RATE_WIDGET_WIDTH - 12, guiTop + RATE_BUTTON_Y + 1, 11, 11)
                .with(GuiDisableableButton.ImageOverlay.CHECKMARK));
        updateEnabledButtons();
    }

    @Override
    protected void actionPerformed(GuiButton guiButton) throws IOException {
        super.actionPerformed(guiButton);
        if (guiButton.id == setRateButton.id) {
            setRateLimit();
        }
    }

    private void updateEnabledButtons() {
        if (setRateButton != null) {
            setRateButton.enabled = tileEntity.structure != null && !rateLimitField.getText().isEmpty();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = LangUtils.localize("gui.fissionReactorStats");
        fontRenderer.drawString(title, (xSize / 2) - (fontRenderer.getStringWidth(title) / 2), 5, 0x404040);
        if (tileEntity.structure == null) {
            fontRenderer.drawString(LangUtils.localize("gui.status") + ": " + LangUtils.localize("gui.incomplete"), 8, 25, 0x404040);
            super.drawGuiContainerForegroundLayer(mouseX, mouseY);
            return;
        }

        SynchronizedFissionData data = tileEntity.structure;
        fontRenderer.drawString(LangUtils.localize("gui.fissionHeatStatistics"), 8, 21, 0x797979);
        fontRenderer.drawString(LangUtils.localize("gui.temp") + ": " + UnitDisplayUtils.roundDecimals(data.temperature) + " K", 14, 32, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.surfaceArea") + ": " + data.surfaceArea, 14, 42, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.boilEfficiency") + ": " + UnitDisplayUtils.roundDecimals(data.getBoilEfficiency() * 100) + "%", 14, 52, 0x404040);

        fontRenderer.drawString(LangUtils.localize("gui.fissionFuelStatistics"), 8, 68, 0x797979);
        fontRenderer.drawString(LangUtils.localize("gui.fuelAssemblies") + ": " + data.fuelAssemblies, 14, 79, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.maxBurnRate") + ": " + UnitDisplayUtils.roundDecimals(data.getMaxBurnRate()) + " /t", 14, 89, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.rateLimit") + ": " + UnitDisplayUtils.roundDecimals(data.rateLimit) + " /t", 14, 99, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.currentBurnRate"), 14, 113, 0x404040);
        fontRenderer.drawString(LangUtils.localize("gui.setRateLimit"), 14, 130, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        rateLimitField.drawTextBox();
        MekanismRenderer.resetColor();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        rateLimitField.updateCursorCounter();
        updateEnabledButtons();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        rateLimitField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void keyTyped(char c, int i) throws IOException {
        if (!rateLimitField.isFocused() || i == Keyboard.KEY_ESCAPE) {
            super.keyTyped(c, i);
        }
        if (i == Keyboard.KEY_RETURN && rateLimitField.isFocused()) {
            setRateLimit();
            return;
        }
        boolean decimalSeparator = c == '.' && !rateLimitField.getText().contains(".");
        if (Character.isDigit(c) || decimalSeparator || isTextboxKey(c, i)) {
            rateLimitField.textboxKeyTyped(c, i);
        }
    }

    private void setRateLimit() {
        if (tileEntity.structure == null || rateLimitField.getText().isEmpty()) {
            return;
        }
        try {
            double target = Double.parseDouble(rateLimitField.getText());
            target = Math.max(0, Math.min(target, tileEntity.structure.getMaxBurnRate()));
            target = UnitDisplayUtils.roundDecimals(target);
            double delta = target - tileEntity.structure.rateLimit;
            if (Math.abs(delta) > 0.0001) {
                Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(1, delta)));
            }
            rateLimitField.setText("");
        } catch (NumberFormatException ignored) {
            rateLimitField.setText("");
        }
    }
}
