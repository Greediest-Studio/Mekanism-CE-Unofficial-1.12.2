package mekanism.client.gui;

import mekanism.api.gas.GasTank;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiPlayerSlot;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.common.MekanismLang;
import mekanism.common.content.sps.SynchronizedSPSData;
import mekanism.common.inventory.container.ContainerSPS;
import mekanism.common.tile.multiblock.TileEntitySPSCasing;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GuiSPSMultiblock extends GuiMekanismTile<TileEntitySPSCasing> {

    private final GasTank emptyInput = new GasTank(SynchronizedSPSData.INPUT_CAPACITY);
    private final GasTank emptyOutput = new GasTank(SynchronizedSPSData.OUTPUT_CAPACITY);

    public GuiSPSMultiblock(InventoryPlayer inventory, TileEntitySPSCasing tile) {
        super(tile, new ContainerSPS(inventory, tile));
        ySize += 5;
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiInnerScreen(this, resource, 27, 17, 122, 60, () -> {
            List<String> list = new ArrayList<>();
            boolean active = tileEntity.structure != null && tileEntity.structure.lastProcessed > 0;
            list.add(MekanismLang.STATUS.getTranslationKey() + (active ? MekanismLang.ACTIVE.getTranslationKey() : MekanismLang.IDLE.getTranslationKey()));
            if (tileEntity.structure != null) {
                list.add(MekanismLang.SPS_ENERGY_INPUT.getTranslationKey() + MekanismUtils.getEnergyDisplay(tileEntity.structure.lastReceivedEnergy));
                list.add(MekanismLang.PROCESS_RATE_MB.getTranslationKey() + tileEntity.structure.getProcessRate() + "mB/t");
                list.add(LangUtils.localize("gui.formed") + ": true");
            } else {
                list.add(LangUtils.localize("gui.formed") + ": false");
            }
            return list;
        }));
        addGuiElement(new GuiGasGauge(() -> tileEntity.structure != null ? tileEntity.structure.inputTank : emptyInput,
              GuiGauge.Type.STANDARD, this, resource, 7, 17).withColor(GuiGauge.TypeColor.RED));
        addGuiElement(new GuiGasGauge(() -> tileEntity.structure != null ? tileEntity.structure.outputTank : emptyOutput,
              GuiGauge.Type.STANDARD, this, resource, 151, 17).withColor(GuiGauge.TypeColor.RED));
        addGuiElement(new GuiPlayerSlot(this, resource, 7, ySize - 83));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, ySize - 92, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }
}
