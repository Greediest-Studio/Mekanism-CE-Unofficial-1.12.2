package mekanism.client.gui;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.button.GuiDisableableButton;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.slot.GuiNormalSlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.gui.element.tab.GuiVisualsTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismSounds;
import mekanism.common.content.miner.ThreadMinerSearch.State;
import mekanism.common.inventory.container.ContainerDigitalMiner;
import mekanism.common.network.PacketDigitalMinerGui.DigitalMinerGuiMessage;
import mekanism.common.network.PacketDigitalMinerGui.MinerGuiPacket;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiDigitalMiner extends GuiMekanismTile<TileEntityDigitalMiner> {

    public static final int START_BUTTON_ID = 0;
    public static final int STOP_BUTTON_ID = 1;
    public static final int CONFIG_BUTTON_ID = 2;
    public static final int RESET_BUTTON_ID = 3;
    private GuiDisableableButton startButton;
    private GuiDisableableButton stopButton;
    private GuiDisableableButton configButton;
    private GuiDisableableButton resetButton;

    public GuiDigitalMiner(InventoryPlayer inventory, TileEntityDigitalMiner tile) {
        super(tile, new ContainerDigitalMiner(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiPowerBarshort(this, tileEntity, resource, 157, 38));
        addGuiElement(new GuiVisualsTab(this, tileEntity, resource));
        addGuiElement(new GuiEnergyInfo(() -> {
            double perTick = tileEntity.getPerTick();
            String multiplier = MekanismUtils.getEnergyDisplay(perTick);
            ArrayList<String> ret = new ArrayList<>(4);
            ret.add(LangUtils.localize("mekanism.gui.digitalMiner.capacity") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy()));
            ret.add(LangUtils.localize("gui.needed") + ": " + multiplier + "/t");
            if (perTick > tileEntity.getMaxEnergy()) {
                ret.add(TextFormatting.RED + LangUtils.localize("mekanism.gui.insufficientbuffer"));
            }
            ret.add(LangUtils.localize("mekanism.gui.bufferfree") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy()));
            return ret;
        }, this, resource));
        addGuiElement(new GuiEnergySlot(this, resource, 151, 19, tileEntity));
    addGuiElement(new GuiInnerScreen(this, resource, 25, 19, 60, 69));
    // Add a new slot visual for the pickaxe slot at (7,19)
    addGuiElement(new GuiNormalSlot(this, resource, 7, 19));
        addGuiElement(new GuiPlayerSlot(this, resource, 7, 159));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addGuiElement(new GuiNormalSlot(this, resource, 7 + x * 18, 91 + y * 18));
            }
        }
        ySize += 76;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        int buttonStart = 19;
        buttonList.add(startButton = new GuiDisableableButton(START_BUTTON_ID, guiLeft + 87, guiTop + buttonStart, 61, 18, LangUtils.localize("gui.start")));
        buttonList.add(stopButton = new GuiDisableableButton(STOP_BUTTON_ID, guiLeft + 87, guiTop + buttonStart + 17, 61, 18, LangUtils.localize("gui.stop")));
        buttonList.add(configButton = new GuiDisableableButton(CONFIG_BUTTON_ID, guiLeft + 87, guiTop + buttonStart + 34, 61, 18, LangUtils.localize("gui.config")));
        buttonList.add(resetButton = new GuiDisableableButton(RESET_BUTTON_ID, guiLeft + 87, guiTop + buttonStart + 51, 61, 18, LangUtils.localize("gui.digitalMiner.reset")));
        updateEnabledButtons();
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        super.actionPerformed(guibutton);
        switch (guibutton.id) {
            case START_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(3)));
            case STOP_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(4)));
            case CONFIG_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new DigitalMinerGuiMessage(MinerGuiPacket.SERVER, Coord4D.get(tileEntity), 0, 0, 0));
            case RESET_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(5)));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateEnabledButtons();
    }

    private void updateEnabledButtons() {
        startButton.enabled = tileEntity.searcher.state == State.IDLE || !tileEntity.running;
        stopButton.enabled = tileEntity.searcher.state != State.IDLE && tileEntity.running;
        configButton.enabled = tileEntity.searcher.state == State.IDLE;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        String runningType;
        if (tileEntity.getPerTick() > tileEntity.getMaxEnergy()) {
            runningType = LangUtils.localize("mekanism.gui.digitalMiner.lowPower");
        } else if (tileEntity.running) {
            runningType = LangUtils.localize("gui.digitalMiner.running");
        } else {
            runningType = LangUtils.localize("gui.idle");
        }
    fontRenderer.drawString(runningType, 29, 21, 0xFF3CFE9A);
    fontRenderer.drawString(tileEntity.searcher.state.localize(), 29, 31, 0xFF3CFE9A);
    fontRenderer.drawString(LangUtils.localize("gui.digitalMiner.toMine") + ":" + " " + tileEntity.clientToMine, 29, 41, 0xFF3CFE9A);
    fontRenderer.drawString("Level: " + tileEntity.level, 29, 51, 0xFF3CFE9A);
        if (!tileEntity.missingStack.isEmpty()) {
            drawColorIcon(64, 21, EnumColor.DARK_RED, 0.8F);
            renderItem(tileEntity.missingStack, 64, 21);
        } else {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(ResourceType.SLOT, "Slot_Icon.png"));
            drawTexturedModalRect(64, 21, SlotOverlay.CHECK.textureX, SlotOverlay.CHECK.textureY, 18, 18);
        }

    // Shift labels 9px to the right to align with moved switches
    renderText(LangUtils.localize("gui.on"), 26, 37.5F);
    renderText(LangUtils.localize("gui.on"), 38.75F, 37.5F);
    renderText(LangUtils.localize("gui.on"), 51.5F, 37.5F);
    renderText(LangUtils.localize("gui.off"), 26, 43.5F);
    renderText(LangUtils.localize("gui.off"), 38.75F, 43.5F);
    renderText(LangUtils.localize("gui.off"), 51.5F, 43.5F);

        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (autoEjectButtoninBounds(xAxis, yAxis)) {
            this.displayTooltip(LangUtils.localize("gui.autoEject") + ":" + LangUtils.transOnOff(tileEntity.doEject), xAxis, yAxis);
        } else if (autoPullButtoninBounds(xAxis, yAxis)) {
            this.displayTooltip(LangUtils.localize("gui.digitalMiner.autoPull") + ":" + LangUtils.transOnOff(tileEntity.doPull), xAxis, yAxis);
        } else if (silkTouchButtoninBounds(xAxis, yAxis)) {
            this.displayTooltip(LangUtils.localize("gui.digitalMiner.silkTouch") + ":" + LangUtils.transOnOff(tileEntity.silkTouch), xAxis, yAxis);
        } else if (xAxis >= 64 && xAxis <= 80 && yAxis >= 21 && yAxis <= 37) {
            if (!tileEntity.missingStack.isEmpty()) {
                this.displayTooltip(LangUtils.localize("gui.digitalMiner.missingBlock"), xAxis, yAxis);
            } else {
                this.displayTooltip(LangUtils.localize("gui.well"), xAxis, yAxis);
            }
        } else if (xAxis >= -21 && xAxis <= -3 && yAxis >= 116 && yAxis <= 134) {
            List<String> info = new ArrayList<>();
            boolean energy = tileEntity.getEnergy() < tileEntity.energyUsage || tileEntity.getEnergy() == 0;
            if (energy) {
                info.add(LangUtils.localize("gui.no_energy"));
            }
            if (energy) {
                this.displayTooltips(info, xAxis, yAxis);
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
    mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.SWITCH, "switch_icon.png"));
    // Shift switches 9px to the right (half a slot)
    drawTexturedModalRect(guiLeft + 28, guiTop + 56, 0, tileEntity.doEject ? 0 : 8, 15, 8);
    drawTexturedModalRect(guiLeft + 28, guiTop + 56 + 8 + 1, 0, !tileEntity.doEject ? 0 : 8, 15, 8);
    drawTexturedModalRect(guiLeft + 47, guiTop + 56, 0, tileEntity.doPull ? 0 : 8, 15, 8);
    drawTexturedModalRect(guiLeft + 47, guiTop + 56 + 8 + 1, 0, !tileEntity.doPull ? 0 : 8, 15, 8);
    drawTexturedModalRect(guiLeft + 66, guiTop + 56, 0, tileEntity.silkTouch ? 0 : 8, 15, 8);
    drawTexturedModalRect(guiLeft + 66, guiTop + 56 + 8 + 1, 0, !tileEntity.silkTouch ? 0 : 8, 15, 8);
    // Indicator dots shifted accordingly
    drawTexturedModalRect(guiLeft + 33, guiTop + 77, 17, 0, 5, 5);
    drawTexturedModalRect(guiLeft + 52, guiTop + 77, 24, 0, 5, 5);
    drawTexturedModalRect(guiLeft + 71, guiTop + 77, 31, 0, 5, 5);
        boolean energy = tileEntity.getEnergy() < tileEntity.energyUsage || tileEntity.getEnergy() == 0;
        for (int slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                mc.getTextureManager().bindTexture(MekanismUtils.getResource(ResourceType.SLOT, "Slot_Icon.png"));
                if (tileEntity.inventory.get(slotX + slotY * 9).getCount() == tileEntity.inventory.get(slotX + slotY * 9).getMaxStackSize()) {
                    drawTexturedModalRect(guiLeft + 7 + slotX * 18, guiTop + 91 + slotY * 18, 158, 0, 18, 18);
                }
            }
        }
        if (energy) {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.TAB, "Warning_Info.png"));
            drawTexturedModalRect(guiLeft - 26, guiTop + 112, 0, 0, 26, 26);
            addGuiElement(new GuiWarningInfo(this, getGuiLocation(), false));
        }
    }


    protected boolean silkTouchButtoninBounds(int xAxis, int yAxis) {
        // Shift hitbox 9px to the right
        return xAxis > 66 && xAxis < 66 + 15 && yAxis > 52 && yAxis < 52 + 28;
    }

    protected boolean autoEjectButtoninBounds(int xAxis, int yAxis) {
        return xAxis > 28 && xAxis < 28 + 15 && yAxis > 52 && yAxis < 52 + 28;
    }

    protected boolean autoPullButtoninBounds(int xAxis, int yAxis) {
        return xAxis > 47 && xAxis < 47 + 15 && yAxis > 52 && yAxis < 52 + 28;
    }

    @Override
    public void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        int xAxis = x - guiLeft;
        int yAxis = y - guiTop;
        if (silkTouchButtoninBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(9);
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(MekanismSounds.BEEP2);
        } else if (autoEjectButtoninBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(0);
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(MekanismSounds.BEEP2);
        } else if (autoPullButtoninBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(1);
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(MekanismSounds.BEEP2);
        }
    }

    private void renderText(String text, float x, float y) {
        int textWidth = fontRenderer.getStringWidth(text);
        float centerX = (x - (textWidth / 2F) * 0.5F);
        float yAdd = 4 - (0.5F * 8) / 2F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + yAdd, 0);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        fontRenderer.drawString(text, centerX, y, 0x101010, false);
        GlStateManager.popMatrix();
        MekanismRenderer.resetColor();
    }

}
