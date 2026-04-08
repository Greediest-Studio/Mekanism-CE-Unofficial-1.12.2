package mekanism.client.gui;

import mekanism.api.Coord4D;
import mekanism.client.gui.button.GuiDisableableButton;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.slot.GuiNormalSlot;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.inventory.container.ContainerUpgradeManagement;
import mekanism.common.network.PacketRemoveUpgrade.RemoveUpgradeMessage;
import mekanism.common.network.PacketSimpleGui;
import mekanism.common.network.PacketSimpleGui.SimpleGuiMessage;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@SideOnly(Side.CLIENT)
public class GuiUpgradeManagement extends GuiMekanism {

    private static final int CONTENT_X_OFFSET = 12;
    private GuiButton backButton;
    private GuiButton removeButton;
    private IUpgradeTile tileEntity;
    @Nullable
    private Upgrade selectedType;
    private boolean isDragging = false;
    private int dragOffset = 0;
    private float scroll;


    public GuiUpgradeManagement(InventoryPlayer inventory, IUpgradeTile tile) {
        super(new ContainerUpgradeManagement(inventory, tile));
        tileEntity = tile;
        xSize = 190;
        ySize = 166;
        addGuiElement(new GuiPlayerSlot(this, getGuiLocation()));
        addGuiElement(new GuiNormalSlot(this, getGuiLocation(), 153 + CONTENT_X_OFFSET, 6).with(GuiSlot.SlotOverlay.UPGRADE));
        addGuiElement(new GuiProgress(
                new GuiProgress.IProgressInfoHandler() {
                    @Override
                    public double getProgress() {
                        return Math.max(Math.min((double) tileEntity.getComponent().upgradeTicks / TileComponentUpgrade.UPGRADE_TICKS_REQUIRED, 1.0D), 0.0D);
                    }
                }, GuiProgress.ProgressBar.INSTALLING, this, getGuiLocation(), 153 + CONTENT_X_OFFSET, 25, false, false));
        addGuiElement(new GuiInnerScreen(this, getGuiLocation(), 90 + CONTENT_X_OFFSET, 6, 59, 50));
        addGuiElement(new GuiElementScreen(this, getGuiLocation(), 24 + CONTENT_X_OFFSET, 56, 125, 24));
        addGuiElement(new GuiElementScreen(this, getGuiLocation(), 24 + CONTENT_X_OFFSET, 6, 66, 50));
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(backButton = new GuiDisableableButton(0, guiLeft + 6, guiTop + 6, 14, 14).with(GuiDisableableButton.ImageOverlay.BACK));
        buttonList.add(removeButton = new GuiDisableableButton(1, guiLeft + 92 + CONTENT_X_OFFSET, guiTop + 42, 55, 12, LangUtils.localize("gui.upgrades.unistall")).handoff(true).enabledTextColor(0xFF30C97A).disabledTextColor(0XFF24965B).hoveredtextColor(0xFF3CFE9A));
        updateEnabledButtons();
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        super.actionPerformed(guibutton);
        TileEntity tile = (TileEntity) tileEntity;
        if (guibutton.id == backButton.id) {
            int guiId = tileEntity.getBlockGuiID(tile.getBlockType(), tile.getBlockMetadata());
            List<IGuiProvider> handlers = PacketSimpleGui.handlers;
            int hand = handlers.indexOf(tileEntity.guiProvider());
            Mekanism.packetHandler.sendToServer(new SimpleGuiMessage(Coord4D.get(tile), hand, guiId));
        } else if (guibutton.id == removeButton.id) {
            if (selectedType != null) {
                Mekanism.packetHandler.sendToServer(new RemoveUpgradeMessage(Coord4D.get(tile), selectedType.ordinal(), Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : 0));
            }
        }
    }

    private boolean overUpgradeType(int xAxis, int yAxis, int xPos, int yPos) {
        return xAxis >= xPos && xAxis <= xPos + 58 && yAxis >= yPos && yAxis <= yPos + 12;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateEnabledButtons();
    }

    private void updateEnabledButtons() {
        removeButton.enabled = selectedType != null;
        //       backButton.enabled = tileEntity.getComponent().getBackButton();
        //    backButton.visible = tileEntity.getComponent().getBackButton();
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiScrollList.png"));
        MekanismRenderer.resetColor();
        drawTexturedModalRect(84 + CONTENT_X_OFFSET, 8 + getScroll(), 16, 0, 4, 4);
        mc.renderEngine.bindTexture(getGuiLocation());
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        String supportedLabel = LangUtils.localize("gui.upgrades.supported") + ":";
        fontRenderer.drawString(supportedLabel, 26 + CONTENT_X_OFFSET, 59, 0x404040);
        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        List<String> hoveredTooltip = null;
        if (selectedType == null) {
            renderText(LangUtils.localize("gui.upgrades.noSelection") + ".", 92 + CONTENT_X_OFFSET, 8, 0.8F, true);
        } else {
            int amount = tileEntity.getComponent().getUpgrades(selectedType);
            String typeName = selectedType.getName() + " " + LangUtils.localize("gui.upgrade");
            int length = fontRenderer.getStringWidth(typeName);
            if (length <= 55) {
                renderText(typeName, 92 + CONTENT_X_OFFSET, 8, 0.6F, true);
            } else {
                renderScaledText(typeName, 92 + CONTENT_X_OFFSET, 8, 0x00CD00, 55);
            }
            renderText(LangUtils.localize("gui.upgrades.amount") + ": " + amount + "/" + selectedType.getMaxInstalled(), 92 + CONTENT_X_OFFSET, 16, 0.6F, true);
            int text = 0;
            for (String s : selectedType.getInfo((TileEntity) tileEntity)) {
                renderText(s, 92 + CONTENT_X_OFFSET, 22 + (6 * text++), 0.6F, true);
            }
        }
        if (!tileEntity.getComponent().getSupportedTypes().isEmpty()) {
            Upgrade[] supported = tileEntity.getComponent().getSupportedTypes().toArray(new Upgrade[0]);
            int startX = 26 + CONTENT_X_OFFSET + fontRenderer.getStringWidth(supportedLabel) + 2;
            int startY = 56;
            int endX = 147 + CONTENT_X_OFFSET;
            int endY = 78;
            int iconSpacing = 14;
            int rowSpacing = 11;
            int iconSize = 12;
            int x = startX;
            int y = startY;
            for (Upgrade upgrade : supported) {
                if (x + iconSize - 1 > endX) {
                    x = startX;
                    y += rowSpacing;
                }
                if (y + iconSize - 1 > endY) {
                    break;
                }
                renderUpgrade(upgrade, x, y, 0.75F, true);
                if (xAxis >= x && xAxis <= x + iconSize && yAxis >= y && yAxis <= y + iconSize) {
                    List<String> tooltip = new ArrayList<>();
                    tooltip.add(upgrade.getStack().getDisplayName());
                    tooltip.addAll(MekanismUtils.splitTooltip(upgrade.getDescription(), upgrade.getStack()));
                    hoveredTooltip = tooltip;
                }
                x += iconSpacing;
            }
        }
        Upgrade[] upgrades = getCurrentUpgrades().toArray(new Upgrade[0]);
        for (int i = 0; i < 4; i++) {
            int index = getUpgradeIndex() + i;
            if (index > upgrades.length - 1) {
                break;
            }
            Upgrade upgrade = upgrades[index];
            int xPos = 25 + CONTENT_X_OFFSET;
            int yPos = 7 + (i * 12);
            renderScaledText(upgrade.getName(), xPos + 12, yPos + 2, 0x404040, 44);
            //   fontRenderer.drawString(upgrade.getName(), xPos + 12, yPos + 2, 0x404040);
            renderUpgrade(upgrade, xPos + 2, yPos + 2, 0.5F, true);
            if (hoveredTooltip == null && overUpgradeType(xAxis, yAxis, xPos, yPos)) {
                hoveredTooltip = MekanismUtils.splitTooltip(upgrade.getDescription(), upgrade.getStack());
            }
        }
        if (hoveredTooltip == null && removeButton.isMouseOver() && removeButton.enabled) {
            hoveredTooltip = Collections.singletonList(LangUtils.localize("gui.upgrades.uninstall"));
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        if (hoveredTooltip != null) {
            this.displayTooltips(hoveredTooltip, xAxis, yAxis);
        }
    }

    private void renderText(String text, int x, int y, float size, boolean scale) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(size, size, size);
        fontRenderer.drawString(text, scale ? (int) ((1F / size) * x) : x, scale ? (int) ((1F / size) * y) : y, 0x00CD00);
        GlStateManager.popMatrix();
    }

    private void renderUpgrade(Upgrade type, int x, int y, float size, boolean scale) {
        if (scale) {
            renderItem(type.getStack(), (int) ((float) x / size), (int) ((float) y / size), size);
        } else {
            renderItem(type.getStack(), x, y, size);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiScrollList.png"));
        drawTexturedModalRect(guiLeft + 83 + CONTENT_X_OFFSET, guiTop + 7, 21, 0, 6, 48);
        if (selectedType != null && tileEntity.getComponent().getUpgrades(selectedType) == 0) {
            selectedType = null;
        }
        Upgrade[] upgrades = getCurrentUpgrades().toArray(new Upgrade[0]);
        for (int i = 0; i < 4; i++) {
            int index = getUpgradeIndex() + i;
            if (index > upgrades.length - 1) {
                break;
            }
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "Scroll_Icon.png"));
            Upgrade upgrade = upgrades[index];
            int xPos = 25 + CONTENT_X_OFFSET;
            int yPos = 7 + (i * 12);
            int yRender;
            if (upgrade == selectedType) {
                yRender = 30 + 24;
            } else if (overUpgradeType(xAxis, yAxis, xPos, yPos)) {
                yRender = 30;
            } else {
                yRender = 30 + 12;
            }
            MekanismRenderer.color(upgrade.getColor(), 1.0F, 2.5F);
            drawTexturedModalRect(guiLeft + xPos, guiTop + yPos, 143, yRender, 58, 12);
            MekanismRenderer.resetColor();
        }
    }

    private Set<Upgrade> getCurrentUpgrades() {
        return tileEntity.getComponent().getInstalledTypes();
    }

    public int getScroll() {
        return Math.max(Math.min((int) (scroll * 42), 42), 0);
    }

    public int getUpgradeIndex() {
        if (getCurrentUpgrades().size() <= 4) {
            return 0;
        }
        return (int) ((getCurrentUpgrades().size() * scroll) - (4F / (float) getCurrentUpgrades().size()) * scroll);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long ticks) {
        super.mouseClickMove(mouseX, mouseY, button, ticks);
        if (isDragging) {
            int yAxis = mouseY - (height - ySize) / 2;
            scroll = Math.min(Math.max((float) (yAxis - 8 - dragOffset) / 42F, 0), 1);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int type) {
        super.mouseReleased(mouseX, mouseY, type);
        if (type == 0 && isDragging) {
            dragOffset = 0;
            isDragging = false;
        }
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 0) {
            int xAxis = mouseX - guiLeft;
            int yAxis = mouseY - guiTop;
            if (xAxis >= 84 + CONTENT_X_OFFSET && xAxis <= 88 + CONTENT_X_OFFSET && yAxis >= getScroll() + 8 && yAxis <= getScroll() + 8 + 4) {
                if (getCurrentUpgrades().size() > 4) {
                    dragOffset = yAxis - (getScroll() + 8);
                    isDragging = true;
                } else {
                    scroll = 0;
                }
            }
            Upgrade[] upgrades = getCurrentUpgrades().toArray(new Upgrade[0]);
            for (int i = 0; i < 4; i++) {
                int index = getUpgradeIndex() + i;
                if (index > upgrades.length - 1) {
                    break;
                }
                int xPos = 25 + CONTENT_X_OFFSET;
                int yPos = 7 + (i * 12);
                if (overUpgradeType(xAxis, yAxis, xPos, yPos)) {
                    selectedType = upgrades[index];
                    break;
                }
            }
        }
    }
}
