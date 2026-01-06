package mekanism.client.gui.robit;

import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.GuiSlot;
import mekanism.client.gui.element.GuiSlot.ISlotInfoHandler;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.slot.GuiExtraSlot;
import mekanism.client.gui.element.slot.GuiInputSlot;
import mekanism.client.gui.element.slot.GuiOutputSlot;
import mekanism.common.entity.EntityRobit;
import mekanism.common.inventory.container.robit.ContainerRobitSmelting;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiRobitSmelting extends GuiRobit {

    public GuiRobitSmelting(InventoryPlayer inventory, EntityRobit entity) {
        super(entity, new ContainerRobitSmelting(inventory, entity));
        addGuiElement(new GuiInputSlot(this, getGuiLocation(), 55, 16, new ISlotInfoHandler() {
            @Override
            public boolean getSlotCanTip() {
                return robit.inventory.get(28).isEmpty();
            }
        }));
        addGuiElement(new GuiExtraSlot(this, getGuiLocation(), 55, 52, new ISlotInfoHandler() {
            @Override
            public boolean getSlotCanTip() {
                return robit.inventory.get(29).isEmpty();
            }
        }));
        addGuiElement(new GuiOutputSlot(SlotType.OUTPUT_LARGE, this, getGuiLocation(), 111, 30, new GuiSlot.ISlotInfoHandler() {
            @Override
            public boolean getSlotCanTip() {
                return robit.inventory.get(30).isEmpty();
            }
        }));
        addGuiElement(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return Math.max(Math.min((double) robit.furnaceCookTime / 200, 1.0D), 0.0D);
            }
        }, GuiProgress.ProgressBar.TALL_RIGHT, this, getGuiLocation(), 78, 34, true, false));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(LangUtils.localize("gui.robit.smelting"), 8, 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, ySize - 93, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.PROGRESS, "Progress_Icon.png"));
        drawTexturedModalRect(guiLeft + 57, guiTop + 37, 1, 14, 14, 14);
        if (robit.furnaceBurnTime > 0) {
            int displayInt = robit.furnaceBurnTime * 13 / robit.currentItemBurnTime;
            drawTexturedModalRect(guiLeft + 56, guiTop + 37 + 12 - displayInt, 18, 26 - displayInt, 14, displayInt + 1);
        }
    }

    @Override
    protected boolean shouldOpenGui(int id) {
        return id != 3;
    }

}
