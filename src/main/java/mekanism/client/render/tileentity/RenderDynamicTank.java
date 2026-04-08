package mekanism.client.render.tileentity;

import mekanism.api.Coord4D;
import mekanism.api.gas.GasStack;
import mekanism.client.render.FluidRenderer;
import mekanism.client.render.FluidRenderer.RenderData;
import mekanism.client.render.FluidRenderer.ValveRenderData;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.GlowInfo;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.content.tank.SynchronizedTankData.ValveData;
import mekanism.common.tile.multiblock.TileEntityDynamicTank;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderDynamicTank extends TileEntitySpecialRenderer<TileEntityDynamicTank> {

    @Override
    public void render(TileEntityDynamicTank tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        boolean glChanged = false;
        if (tileEntity.clientHasStructure && tileEntity.isRendering && tileEntity.structure != null) {
            GasStack gasStack = tileEntity.structure.gasstored;
            FluidStack renderStack = tileEntity.structure.fluidStored;
            boolean renderingGas = false;
            boolean gasHasFluid = false;
            if ((renderStack == null || renderStack.amount <= 0) && gasStack != null && gasStack.amount > 0 && gasStack.getGas() != null) {
                renderingGas = true;
                gasHasFluid = gasStack.getGas().hasFluid();
                if (gasHasFluid) {
                    renderStack = new FluidStack(gasStack.getGas().getFluid(), gasStack.amount);
                }
            }

            if (renderingGas && !gasHasFluid) {
                glChanged = renderGasWithoutFluid(tileEntity, gasStack);
            } else if (renderStack != null && renderStack.amount > 0) {
                RenderData data = new RenderData();
                data.location = tileEntity.structure.renderLocation;
                data.height = tileEntity.structure.volHeight - 2;
                data.length = tileEntity.structure.volLength;
                data.width = tileEntity.structure.volWidth;
                data.fluidType = renderStack;

                if (data.location != null && data.height >= 1) {
                    GlStateManager.pushMatrix();
                    glChanged = enableGL();
                    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                    FluidRenderer.translateToOrigin(data.location);
                    GlowInfo glowInfo = MekanismRenderer.enableGlow(data.fluidType);
                    float scale = tileEntity.clientCapacity <= 0 ? 0 : (float) data.fluidType.amount / (float) tileEntity.clientCapacity;
                    if (renderingGas) {
                        MekanismRenderer.color(gasStack.getGas(), Math.min(1, scale + 0.2F));
                    } else {
                        MekanismRenderer.color(data.fluidType, scale);
                    }
                    if (data.fluidType.getFluid().isGaseous(data.fluidType)) {
                        FluidRenderer.getTankDisplay(data).render();
                    } else {
                        FluidRenderer.getTankDisplay(data, tileEntity.prevScale).render();
                    }

                    MekanismRenderer.resetColor();
                    MekanismRenderer.disableGlow(glowInfo);
                    GlStateManager.popMatrix();

                    for (ValveData valveData : tileEntity.valveViewing) {
                        GlStateManager.pushMatrix();
                        glChanged = enableGL();
                        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                        FluidRenderer.translateToOrigin(valveData.location);
                        GlowInfo valveGlowInfo = MekanismRenderer.enableGlow(data.fluidType);
                        if (renderingGas) {
                            MekanismRenderer.color(gasStack);
                        } else {
                            MekanismRenderer.color(data.fluidType);
                        }
                        FluidRenderer.getValveDisplay(ValveRenderData.get(data, valveData)).render();
                        MekanismRenderer.resetColor();
                        MekanismRenderer.disableGlow(valveGlowInfo);
                        GlStateManager.popMatrix();
                    }
                }
            }

            if (glChanged) {
                setLightmapDisabled(false);
                GlStateManager.disableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.enableLighting();
                GlStateManager.disableCull();
            }
        }
    }

    private boolean renderGasWithoutFluid(TileEntityDynamicTank tileEntity, GasStack gasStack) {
        if (gasStack == null || gasStack.getGas() == null || tileEntity.structure == null || tileEntity.structure.renderLocation == null) {
            return false;
        }
        int innerHeight = tileEntity.structure.volHeight - 2;
        if (innerHeight < 1) {
            return false;
        }
        float scale = tileEntity.clientCapacity <= 0 ? 0 : (float) gasStack.amount / (float) tileEntity.clientCapacity;
        Coord4D renderLocation = tileEntity.structure.renderLocation;

        GlStateManager.pushMatrix();
        enableGL();
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        FluidRenderer.translateToOrigin(renderLocation);
        GlowInfo glowInfo = MekanismRenderer.enableGlow();
        MekanismRenderer.color(gasStack.getGas(), Math.min(1, scale + 0.2F));
        Model3D gasModel = new Model3D();
        gasModel.baseBlock = Blocks.WATER;
        gasModel.setTexture(MekanismRenderer.heatIcon);
        gasModel.minX = 0.01;
        gasModel.minY = 0.01;
        gasModel.minZ = 0.01;
        gasModel.maxX = tileEntity.structure.volLength - 0.01;
        gasModel.maxY = innerHeight - 0.01;
        gasModel.maxZ = tileEntity.structure.volWidth - 0.01;
        MekanismRenderer.renderObject(gasModel);
        MekanismRenderer.resetColor();
        MekanismRenderer.disableGlow(glowInfo);
        GlStateManager.popMatrix();

        for (ValveData valveData : tileEntity.valveViewing) {
            Model3D valveModel = getGasValveDisplay(renderLocation, innerHeight, valveData);
            if (valveModel != null) {
                GlStateManager.pushMatrix();
                enableGL();
                bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                FluidRenderer.translateToOrigin(valveData.location);
                GlowInfo valveGlowInfo = MekanismRenderer.enableGlow();
                MekanismRenderer.color(gasStack);
                MekanismRenderer.renderObject(valveModel);
                MekanismRenderer.resetColor();
                MekanismRenderer.disableGlow(valveGlowInfo);
                GlStateManager.popMatrix();
            }
        }
        return true;
    }

    private Model3D getGasValveDisplay(Coord4D renderLocation, int innerHeight, ValveData valveData) {
        if (renderLocation == null || valveData == null || valveData.location == null || valveData.side == null) {
            return null;
        }
        int valveFluidHeight = valveData.location.y - renderLocation.y;
        Model3D model = new Model3D();
        model.baseBlock = Blocks.WATER;
        model.setTexture(MekanismRenderer.heatIcon);
        switch (valveData.side) {
            case DOWN:
                model.minX = 0.3;
                model.minY = 1.01;
                model.minZ = 0.3;
                model.maxX = 0.7;
                model.maxY = 1.5;
                model.maxZ = 0.7;
                break;
            case UP:
                model.minX = 0.3;
                model.minY = -innerHeight - 0.01;
                model.minZ = 0.3;
                model.maxX = 0.7;
                model.maxY = -0.01;
                model.maxZ = 0.7;
                break;
            case NORTH:
                model.minX = 0.3;
                model.minY = -valveFluidHeight + 0.01;
                model.minZ = 1.02;
                model.maxX = 0.7;
                model.maxY = 0.7;
                model.maxZ = 1.4;
                break;
            case SOUTH:
                model.minX = 0.3;
                model.minY = -valveFluidHeight + 0.01;
                model.minZ = -0.4;
                model.maxX = 0.7;
                model.maxY = 0.7;
                model.maxZ = -0.02;
                break;
            case WEST:
                model.minX = 1.02;
                model.minY = -valveFluidHeight + 0.01;
                model.minZ = 0.3;
                model.maxX = 1.4;
                model.maxY = 0.7;
                model.maxZ = 0.7;
                break;
            case EAST:
                model.minX = -0.4;
                model.minY = -valveFluidHeight + 0.01;
                model.minZ = 0.3;
                model.maxX = -0.02;
                model.maxY = 0.7;
                model.maxZ = 0.7;
                break;
            default:
                return null;
        }
        return model;
    }

    private boolean enableGL() {
        GlStateManager.enableCull();
        GlStateManager.disableLighting();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        setLightmapDisabled(true);
        return true;
    }
}
