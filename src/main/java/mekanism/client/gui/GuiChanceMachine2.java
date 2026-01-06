package mekanism.client.gui;

import mekanism.client.gui.element.*;
import mekanism.client.gui.element.GuiProgress.IProgressInfoHandler;
import mekanism.client.gui.element.GuiProgress.ProgressBar;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.slot.GuiInputSlot;
import mekanism.client.gui.element.slot.GuiOutputSlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.inventory.container.ContainerChanceMachine2;
import mekanism.common.recipe.machines.Chance2MachineRecipe;
import mekanism.common.tile.prefab.TileEntityChanceMachine2;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiChanceMachine2<RECIPE extends Chance2MachineRecipe<RECIPE>> extends GuiMekanismTile<TileEntityChanceMachine2<RECIPE>> implements IJeiNoShowRecipe {

    public GuiChanceMachine2(InventoryPlayer inventory, TileEntityChanceMachine2<RECIPE> tile) {
        super(tile, new ContainerChanceMachine2<>(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiTransporterConfigTab(this, tileEntity, resource));
        addGuiElement(new GuiPowerBar(this, tileEntity, resource, 164, 15));
        addGuiElement(new GuiEnergyInfo(tileEntity, this, resource));
        addGuiElement(new GuiInputSlot(this, resource, 55, 16, tileEntity));
        addGuiElement(new GuiEnergySlot(this, resource, 55, 52, tileEntity));
        addGuiElement(new GuiOutputSlot(SlotType.OUTPUT_LARGE, this, resource, 111, 30,tileEntity));
        addGuiElement(new GuiProgress(new IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return tileEntity.getScaledProgress();
            }
        }, ProgressBar.BAR, this, resource, 77, 37));
        addGuiElement(new GuiPlayerSlot(this, resource));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "Other_Icon.png"));
        drawTexturedModalRect(guiLeft + 60, guiTop + 38, 22, 0, 8, 10);
        boolean energy = tileEntity.getEnergy() < tileEntity.energyPerTick || tileEntity.getEnergy() == 0;
        boolean outslot = tileEntity.inventory.get(2).getCount() == tileEntity.inventory.get(2).getMaxStackSize();
        if (outslot) {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.SLOT, "Slot_Icon.png"));
            drawTexturedModalRect(guiLeft + 111, guiTop + 30, 202, 0, 42, 26);
        }
        if (outslot || energy) {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.TAB, "Warning_Info.png"));
            drawTexturedModalRect(guiLeft - 26, guiTop + 112, 0, 0, 26, 26);
            addGuiElement(new GuiWarningInfo(this, getGuiLocation(), false));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (xAxis >= -21 && xAxis <= -3 && yAxis >= 116 && yAxis <= 134) {
            List<String> info = new ArrayList<>();
            boolean energy = tileEntity.getEnergy() < tileEntity.energyPerTick || tileEntity.getEnergy() == 0;
            boolean outslot = tileEntity.inventory.get(2).getCount() == tileEntity.inventory.get(2).getMaxStackSize();
            if (energy) {
                info.add(LangUtils.localize("gui.no_energy"));
            }
            if (outslot) {
                info.add(LangUtils.localize("gui.item_no_space"));
            }
            if (energy || outslot) {
                this.displayTooltips(info, xAxis, yAxis);
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

}
