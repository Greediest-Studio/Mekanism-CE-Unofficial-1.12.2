package mekanism.client.gui.element.gauge;

import mekanism.api.EnumColor;
import mekanism.api.transmitters.TransmissionType;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiUtils;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.SideData;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class GuiGauge<T> extends GuiElement {

    protected final int xLocation;
    protected final int yLocation;
    protected final int texX;
    protected final int texY;
    protected final int width;
    protected final int height;
    protected final boolean vertical;
    protected EnumColor color;
    protected boolean dummy;

    protected T dummyType;
    private TypeColor typecolor = TypeColor.NORMAL;

    public GuiGauge(Type type, IGuiWrapper gui, ResourceLocation def, int x, int y) {
        super(MekanismUtils.getResource(MekanismUtils.ResourceType.GAUGE, "Gauge_Icon.png"), gui, def);
        xLocation = x;
        yLocation = y;
        width = type.width;
        height = type.height;
        texX = type.texX;
        texY = type.texY;
        vertical = type.vertical;
    }

    public abstract int getScaledLevel();

    public abstract TextureAtlasSprite getIcon();

    public abstract String getTooltipText();

    public List<String> getTooltipTexts() {
        return new ArrayList<>();
    }


    protected void applyRenderColor() {
    }


    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        drawBlack(guiWidth, guiHeight);
        mc.renderEngine.bindTexture(RESOURCE);
        if (!dummy) {
            renderScale(guiWidth, guiHeight);
        }
        mc.renderEngine.bindTexture(defaultLocation);
    }

    public void drawBlack(int guiWidth, int guiHeight) {
        mc.renderEngine.bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.GAUGE, typecolor.textureLocation));
        int halfWidthLeft = width / 2;
        int halfWidthRight = width % 2 == 0 ? halfWidthLeft : halfWidthLeft + 1;
        int halfHeightTop = height / 2;
        int halfHeight = height % 2 == 0 ? halfHeightTop : halfHeightTop + 1;
        MekanismRenderer.resetColor();
        guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation, 0, 0, halfWidthLeft, halfHeightTop);
        guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation + halfHeightTop, 0, 256 - halfHeight, halfWidthLeft, halfHeight);
        guiObj.drawTexturedRect(guiWidth + xLocation + halfWidthLeft, guiHeight + yLocation, 256 - halfWidthRight, 0, halfWidthRight, halfHeightTop);
        guiObj.drawTexturedRect(guiWidth + xLocation + halfWidthLeft, guiHeight + yLocation + halfHeightTop, 256 - halfWidthRight, 256 - halfHeight, halfWidthRight, halfHeight);
    }


    public void renderScale(int guiWidth, int guiHeight) {
        if (getScaledLevel() == 0 || getIcon() == null) {
            guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation, texX, texY, width, height);
            return;
        }
        applyRenderColor();
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GuiUtils.drawBarSprite(guiWidth + xLocation, guiHeight + yLocation, width, height, getScaledLevel(), getIcon(), vertical);
        MekanismRenderer.resetColor();
        mc.renderEngine.bindTexture(RESOURCE);
        guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation, texX, texY, width, height);
    }

    @Override
    public void renderForeground(int xAxis, int yAxis) {
        if (xAxis >= xLocation + 1 && xAxis <= xLocation + width - 1 && yAxis >= yLocation + 1 && yAxis <= yLocation + height - 1) {
            ItemStack stack = mc.player.inventory.getItemStack();
            if (!stack.isEmpty() && stack.getItem() instanceof ItemConfigurator && color != null) {
                if (guiObj instanceof GuiMekanismTile<?> guiMekanismTile) {
                    TileEntity tile = guiMekanismTile.getTileEntity();
                    if (tile instanceof ISideConfiguration side && getTransmission() != null) {
                        SideData data = null;
                        for (SideData iterData : side.getConfig().getOutputs(getTransmission())) {
                            if (iterData.color == color) {
                                data = iterData;
                                break;
                            }
                        }
                        String localized = data == null ? "" : data.localize();
                        guiObj.displayTooltip(color + localized + " (" + color.getColoredName() + ")", xAxis, yAxis);
                    }
                }
            } else {
                if (getTooltipText() != null && !getTooltipText().isEmpty()) {
                    guiObj.displayTooltip(getTooltipText(), xAxis, yAxis);
                }
                if (getTooltipTexts() != null && !getTooltipTexts().isEmpty()) {
                    guiObj.displayTooltips(getTooltipTexts(), xAxis, yAxis);
                }
            }
        }
    }


    public boolean isMouseOver(int xAxis, int yAxis) {
        return xAxis >= xLocation + 1 && xAxis <= xLocation + width - 1 && yAxis >= yLocation + 1 && yAxis <= yLocation + height - 1;
    }

    public GuiGauge<T> withColor(TypeColor color) {
        this.typecolor = color;
        return this;
    }

    @Override
    public void preMouseClicked(int xAxis, int yAxis, int button) {
    }

    @Override
    public void mouseClicked(int xAxis, int yAxis, int button) {
    }

    public abstract TransmissionType getTransmission();

    public void setDummyType(T type) {
        dummyType = type;
    }

    @Override
    public Rectangle4i getBounds(int guiWidth, int guiHeight) {
        return new Rectangle4i(guiWidth + xLocation, guiHeight + yLocation, width, height);
    }

    public enum TypeColor {
        AQUA("Aqua.png"),
        BLUE("Blue.png"),
        NORMAL("Normal.png"),
        ORANGE("Orange.png"),
        RED("Red.png"),
        YELLOW("Yellow.png");
        public final String textureLocation;

        TypeColor(String color) {
            textureLocation = color;
        }
    }

    public enum Type {
        MEDIUM(34, 60, 0, 0),
        SMALL(18, 30, 72, 0),
        SMALL_MED(18, 48, 53, 0),
        STANDARD(18, 60, 34, 0),
        WIDE(66, 50, 91, 0),
        //这是工厂使用的
        SLOT(18, 18, 158, 0),
        SLOT_BASIC(94, 18, 0, 60, false),
        SLOT_ADVANCED(122, 18, 0, 60, false),
        SLOT_ELITE(132, 18, 0, 60, false),
        SLOT_ULTIMATE(170, 18, 0, 60, false),
        SLOT_CREATIVE(208, 18, 0, 60, false),
        ;

        public final int width;
        public final int height;
        public final int texX;
        public final int texY;
        public final boolean vertical;

        Type(int w, int h, int tx, int ty) {
            this(w, h, tx, ty, true);
        }

        Type(int w, int h, int tx, int ty, boolean vertical) {
            width = w;
            height = h;
            texX = tx;
            texY = ty;
            this.vertical = vertical;
        }

    }
}
