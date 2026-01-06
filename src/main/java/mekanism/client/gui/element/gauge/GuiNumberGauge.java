package mekanism.client.gui.element.gauge;

import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.client.gui.IGuiWrapper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiNumberGauge extends GuiGauge<Void> {

    private final INumberInfoHandler infoHandler;

    public GuiNumberGauge(INumberInfoHandler handler, Type type, IGuiWrapper gui, ResourceLocation def, int x, int y) {
        super(type, gui, def, x, y);
        infoHandler = handler;
    }

    @Override
    public TransmissionType getTransmission() {
        return null;
    }

    @Override
    public int getScaledLevel() {
        double scale = Math.max(Math.min(infoHandler.getLevel() / infoHandler.getMaxLevel(), 1.0D), 0.0D);
        if (vertical) {
            return MathUtils.clampToInt(Math.round(scale * (height - 2)));
        } else {
            return MathUtils.clampToInt(Math.round(scale * (width - 2)));
        }
    }

    @Override
    public TextureAtlasSprite getIcon() {
        return infoHandler.getIcon();
    }

    @Override
    public String getTooltipText() {
        return infoHandler.getText(infoHandler.getLevel());
    }


    public interface INumberInfoHandler {

        TextureAtlasSprite getIcon();

        double getLevel();

        double getMaxLevel();

        String getText(double level);
    }
}
