package mekanism.client.gui;

import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.GuiPlayerSlot;
import mekanism.client.gui.element.GuiPowerBar;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.gui.element.tab.GuiVisualsTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.ContainerDimensionalStabilizer;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiDimensionalStabilizer extends GuiMekanismTile<TileEntityDimensionalStabilizer> {

    private static final int GRID_START_X = 63;
    private static final int GRID_START_Y = 19;
    private static final int GRID_BUTTON_SIZE = 10;
    private static final int GRID_BUTTON_ID = 200;

    private final List<ChunkButton> chunkButtons = new ArrayList<>();

    public GuiDimensionalStabilizer(InventoryPlayer inventory, TileEntityDimensionalStabilizer tile) {
        super(tile, new ContainerDimensionalStabilizer(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiVisualsTab(this, tileEntity, resource));
        addGuiElement(new GuiEnergyInfo(() -> {
            String usage = MekanismUtils.getEnergyDisplay(tileEntity.getEnergyUsage());
            return Arrays.asList(
                    LangUtils.localize("gui.using") + ": " + usage + "/t",
                    LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())
            );
        }, this, resource));
        addGuiElement(new GuiPowerBar(this, tileEntity, resource, 164, 15));
        addGuiElement(new GuiEnergySlot(this, resource, 142, 34, tileEntity));
        addGuiElement(new GuiPlayerSlot(this, resource, 7, 88));
        ySize += 5;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        chunkButtons.clear();
        for (int x = 0; x < TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER; x++) {
            for (int z = 0; z < TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER; z++) {
                int id = GRID_BUTTON_ID + x * TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER + z;
                ChunkButton button = new ChunkButton(id, guiLeft + GRID_START_X + x * GRID_BUTTON_SIZE, guiTop + GRID_START_Y + z * GRID_BUTTON_SIZE, x, z);
                chunkButtons.add(button);
                buttonList.add(button);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        super.actionPerformed(guibutton);
        if (guibutton.id >= GRID_BUTTON_ID && guibutton.id < GRID_BUTTON_ID + TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER * TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER) {
            int index = guibutton.id - GRID_BUTTON_ID;
            int x = index / TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER;
            int z = index % TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER;
            boolean handled;
            if (x == TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS && z == TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS) {
                handled = handleCenterEnableButton();
            } else {
                sendPacket(TileEntityDimensionalStabilizer.PACKET_TOGGLE_CHUNK, x, z);
                handled = true;
            }
            if (handled) {
                SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 1) {
            for (ChunkButton chunkButton : chunkButtons) {
                if (chunkButton.isMouseOverButton(mouseX, mouseY)) {
                    boolean handled;
                    if (chunkButton.isCenter()) {
                        handled = handleCenterDisableButton();
                    } else {
                        sendPacket(TileEntityDimensionalStabilizer.PACKET_TOGGLE_CHUNK, chunkButton.chunkGridX, chunkButton.chunkGridZ);
                        handled = true;
                    }
                    if (handled) {
                        SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                    }
                    break;
                }
            }
        }
    }

    private boolean handleCenterEnableButton() {
        for (int i = 1; i <= TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; i++) {
            if (hasAtRadius(i, false)) {
                sendPacket(TileEntityDimensionalStabilizer.PACKET_ENABLE_RADIUS, i);
                return true;
            }
        }
        return false;
    }

    private boolean handleCenterDisableButton() {
        for (int i = TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; i > 0; i--) {
            if (hasAtRadius(i, true)) {
                sendPacket(TileEntityDimensionalStabilizer.PACKET_DISABLE_RADIUS, i);
                return true;
            }
        }
        return false;
    }

    private boolean hasAtRadius(int radius, boolean state) {
        for (int x = -radius; x <= radius; x++) {
            boolean skipInner = x > -radius && x < radius;
            int actualX = x + TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS;
            for (int z = -radius; z <= radius; z += skipInner ? 2 * radius : 1) {
                if (tileEntity.isChunkLoadingAt(actualX, z + TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS) == state) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendPacket(int type, int value) {
        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(type, value)));
    }

    private void sendPacket(int type, int x, int z) {
        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(type, x, z)));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, ySize - 94, 0x404040);
        drawNorthArrow(52, 28, 0xFF404040);
        String north = LangUtils.localize("direction.north.short");
        int northTextX = 49 + Math.max(0, (15 - fontRenderer.getStringWidth(north)) / 2);
        renderScaledText(north, northTextX, 41, 0x404040, 15);

        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        for (ChunkButton button : chunkButtons) {
            if (button.isMouseOverButton(mouseX, mouseY)) {
                displayTooltips(button.getTooltips(), xAxis, yAxis);
                break;
            }
        }

        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    private void drawNorthArrow(int x, int y, int color) {
        drawRect(x + 5, y, x + 6, y + 1, color);
        drawRect(x + 4, y + 1, x + 7, y + 2, color);
        drawRect(x + 3, y + 2, x + 8, y + 3, color);
        drawRect(x + 2, y + 3, x + 9, y + 4, color);
        drawRect(x + 4, y + 4, x + 7, y + 11, color);
    }

    private class ChunkButton extends GuiButton {

        private final ResourceLocation buttonTexture = MekanismUtils.getResource(MekanismUtils.ResourceType.BUTTON, "Button.png");
        private final int chunkGridX;
        private final int chunkGridZ;

        private ChunkButton(int buttonId, int x, int y, int chunkGridX, int chunkGridZ) {
            super(buttonId, x, y, GRID_BUTTON_SIZE, GRID_BUTTON_SIZE, "");
            this.chunkGridX = chunkGridX;
            this.chunkGridZ = chunkGridZ;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            MekanismRenderer.resetColor();
            hovered = isMouseOverButton(mouseX, mouseY);
            int halfWidthLeft = width / 2;
            int halfWidthRight = width % 2 == 0 ? halfWidthLeft : halfWidthLeft + 1;
            int halfHeightTop = height / 2;
            int halfHeightBottom = height % 2 == 0 ? halfHeightTop : halfHeightTop + 1;
            mc.getTextureManager().bindTexture(buttonTexture);
            EnumColor color = getColor();
            boolean doColor = color != null;
            if (doColor) {
                MekanismRenderer.color(color);
            }
            int stateYOffset = hovered ? 40 : 20;
            drawTexturedModalRect(x, y, 0, stateYOffset, halfWidthLeft, halfHeightTop);
            drawTexturedModalRect(x, y + halfHeightTop, 0, stateYOffset + 20 - halfHeightBottom, halfWidthLeft, halfHeightBottom);
            drawTexturedModalRect(x + halfWidthLeft, y, 200 - halfWidthRight, stateYOffset, halfWidthRight, halfHeightTop);
            drawTexturedModalRect(x + halfWidthLeft, y + halfHeightTop, 200 - halfWidthRight, stateYOffset + 20 - halfHeightBottom, halfWidthRight, halfHeightBottom);
            if (doColor) {
                MekanismRenderer.resetColor();
            }

        }

        public EnumColor getColor() {
            boolean loading = tileEntity.isChunkLoadingAt(chunkGridX, chunkGridZ);
            if (loading) {
                return EnumColor.INDIGO;
            } else {
                return null;
            }
        }


        private boolean isCenter() {
            return chunkGridX == TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS && chunkGridZ == TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS;
        }

        private boolean isMouseOverButton(int mouseX, int mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }

        private List<String> getTooltips() {
            int tileChunkX = tileEntity.getPos().getX() >> 4;
            int tileChunkZ = tileEntity.getPos().getZ() >> 4;
            int chunkX = tileChunkX + chunkGridX - TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS;
            int chunkZ = tileChunkZ + chunkGridZ - TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS;
            String coloredCoords = getColoredChunkCoords(chunkX, chunkZ);
            List<String> tooltips = new ArrayList<>();
            if (isCenter()) {
                tooltips.add(LangUtils.localizeWithFormat("gui.mekanism.stabilizer.center", coloredCoords));
                for (int i = 1; i <= TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; i++) {
                    if (hasAtRadius(i, false)) {
                        tooltips.add(" ");
                        tooltips.add(LangUtils.localizeWithFormat("gui.mekanism.stabilizer.radius.enable", coloredCoords, EnumColor.INDIGO + Integer.toString(i) + EnumColor.WHITE));
                        break;
                    }
                }
                for (int i = TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; i > 0; i--) {
                    if (hasAtRadius(i, true)) {
                        tooltips.add(" ");
                        tooltips.add(LangUtils.localizeWithFormat("gui.mekanism.stabilizer.radius.disable", coloredCoords, EnumColor.INDIGO + Integer.toString(i) + EnumColor.WHITE));
                        break;
                    }
                }
            } else {
                boolean loading = tileEntity.isChunkLoadingAt(chunkGridX, chunkGridZ);
                EnumColor stateColor = loading ? EnumColor.BRIGHT_GREEN : EnumColor.RED;
                String state = stateColor + LangUtils.transOnOff(loading) + EnumColor.WHITE;
                tooltips.add(LangUtils.localizeWithFormat("gui.mekanism.stabilizer.toggle_loading", coloredCoords, state));
            }
            return tooltips;
        }

        private String getColoredChunkCoords(int chunkX, int chunkZ) {
            return EnumColor.INDIGO + Integer.toString(chunkX) + EnumColor.WHITE + ", " + EnumColor.INDIGO + chunkZ + EnumColor.WHITE;
        }
    }


}
