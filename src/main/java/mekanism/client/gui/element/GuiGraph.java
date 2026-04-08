package mekanism.client.gui.element;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiGraph extends GuiElement {

    private final List<Integer> graphData = new ArrayList<>();
    private final GraphDataHandler dataHandler;
    private final int xPosition;
    private final int yPosition;
    private final int xSize;
    private final int ySize;

    private int currentScale = 10;
    private int minScale = 10;
    private boolean fixedScale = false;

    public GuiGraph(IGuiWrapper gui, ResourceLocation def, int x, int y, int sizeX, int sizeY, GraphDataHandler handler) {
        super(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiGraph.png"), gui, def);
        xPosition = x;
        yPosition = y;
        xSize = sizeX;
        ySize = sizeY;
        dataHandler = handler;
    }

    public void enableFixedScale(int scale) {
        fixedScale = true;
        currentScale = Math.max(1, scale);
    }

    public void setMinScale(int scale) {
        minScale = Math.max(1, scale);
        if (!fixedScale) {
            currentScale = Math.max(currentScale, minScale);
        }
    }

    public void addData(int data) {
        if (graphData.size() == xSize) {
            graphData.remove(0);
        }

        graphData.add(data);
        if (!fixedScale) {
            currentScale = minScale;
            for (int i : graphData) {
                if (i > currentScale) {
                    currentScale = i;
                }
            }
        }
    }

    @Override
    public Rectangle4i getBounds(int guiWidth, int guiHeight) {
        return new Rectangle4i(guiWidth + xPosition, guiHeight + yPosition, xSize, ySize);
    }

    @Override
    protected boolean inBounds(int xAxis, int yAxis) {
        return xAxis >= xPosition && xAxis <= xPosition + xSize && yAxis >= yPosition && yAxis <= yPosition + ySize;
    }

    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        drawBlack(guiWidth, guiHeight);
        drawGraph(guiWidth, guiHeight);
        mc.renderEngine.bindTexture(defaultLocation);
    }

    @Override
    public void renderForeground(int xAxis, int yAxis) {
        if (inBounds(xAxis, yAxis)) {
            int height = ySize - (yAxis - yPosition);
            int scaled = (int) (((double) height / (double) ySize) * currentScale);
            displayTooltip(dataHandler.getDataDisplay(scaled), xAxis, yAxis);
        }
    }

    @Override
    public void preMouseClicked(int xAxis, int yAxis, int button) {
    }

    @Override
    public void mouseClicked(int xAxis, int yAxis, int button) {
    }

    public void drawBlack(int guiWidth, int guiHeight) {
        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "Inner_Screen.png"));
        int halfWidthLeft = xSize / 2;
        int halfWidthRight = xSize % 2 == 0 ? halfWidthLeft : halfWidthLeft + 1;
        int halfHeightTop = (ySize + 1) / 2;
        int halfHeight = (ySize + 1) % 2 == 0 ? halfHeightTop : halfHeightTop + 1;
        MekanismRenderer.resetColor();
        guiObj.drawTexturedRect(guiWidth + xPosition, guiHeight + yPosition, 0, 0, halfWidthLeft, halfHeightTop);
        guiObj.drawTexturedRect(guiWidth + xPosition, guiHeight + yPosition + halfHeightTop, 0, 256 - halfHeight, halfWidthLeft, halfHeight);
        guiObj.drawTexturedRect(guiWidth + xPosition + halfWidthLeft, guiHeight + yPosition, 256 - halfWidthRight, 0, halfWidthRight, halfHeightTop);
        guiObj.drawTexturedRect(guiWidth + xPosition + halfWidthLeft, guiHeight + yPosition + halfHeightTop, 256 - halfWidthRight, 256 - halfHeight, halfWidthRight, halfHeight);
    }

    public void drawGraph(int guiWidth, int guiHeight) {
        mc.renderEngine.bindTexture(RESOURCE);
        for (int i = 0; i < graphData.size(); i++) {
            int data = Math.min(currentScale, graphData.get(i));
            int relativeHeight = (int) (((double) data / (double) currentScale) * ySize);
            guiObj.drawTexturedRect(guiWidth + xPosition + i, guiHeight + yPosition + (ySize - relativeHeight), 10, 0, 1, 1);

            int displays = (relativeHeight - 1) / 10 + ((relativeHeight - 1) % 10 > 0 ? 1 : 0);

            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            for (int iter = 0; iter < displays; iter++) {
                GlStateManager.color(1, 1, 1, 0.2F + (0.8F * ((float) i / (float) graphData.size())));
                int height = (relativeHeight - 1) % 10 > 0 && iter == displays - 1 ? (relativeHeight - 1) % 10 : 10;
                guiObj.drawTexturedRect(guiWidth + xPosition + i, guiHeight + yPosition + (ySize - (iter * 10)) - 10 + (10 - height), 11, 0, 1, height);
            }
            MekanismRenderer.resetColor();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }
    }

    public interface GraphDataHandler {

        String getDataDisplay(int data);
    }
}
