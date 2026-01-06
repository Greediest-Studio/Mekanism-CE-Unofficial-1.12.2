package mekanism.client.gui.element.gauge;

import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiEnergyGauge extends GuiGauge<Void> {

    private final IEnergyInfoHandler infoHandler;

    public GuiEnergyGauge(IEnergyInfoHandler handler, Type type, IGuiWrapper gui, ResourceLocation def, int x, int y) {
        super(type, gui, def, x, y);
        infoHandler = handler;
    }

    public static GuiEnergyGauge getDummy(Type type, IGuiWrapper gui, ResourceLocation def, int x, int y) {
        GuiEnergyGauge gauge = new GuiEnergyGauge(null, type, gui, def, x, y);
        gauge.dummy = true;
        return gauge;
    }

    @Override
    public Rectangle4i getBounds(int guiWidth, int guiHeight) {
        return new Rectangle4i(guiWidth - 26, guiHeight + 6, 26, 26);
    }

    @Override
    public TransmissionType getTransmission() {
        return TransmissionType.ENERGY;
    }

    @Override
    public int getScaledLevel() {
        if (dummy || infoHandler.getEnergyStorage().getEnergy() == Double.MAX_VALUE) {
            return height - 2;
        }
        double scale =  Math.max(Math.min(infoHandler.getEnergyStorage().getEnergy() / infoHandler.getEnergyStorage().getMaxEnergy(), 1.0D), 0.0D);
        if (vertical) {
            return MathUtils.clampToInt(Math.round(scale * (height - 2)));
        } else {
            return MathUtils.clampToInt(Math.round(scale * (width - 2)));
        }

    }

    @Override
    public TextureAtlasSprite getIcon() {
        return MekanismRenderer.energyIcon;
    }

    @Override
    public String getTooltipText() {
        return infoHandler.getEnergyStorage().getEnergy() > 0 ? MekanismUtils.getEnergyDisplay(infoHandler.getEnergyStorage().getEnergy(),
                infoHandler.getEnergyStorage().getMaxEnergy()) : LangUtils.localize("gui.empty");
    }

    public interface IEnergyInfoHandler {

        IStrictEnergyStorage getEnergyStorage();
    }
}
