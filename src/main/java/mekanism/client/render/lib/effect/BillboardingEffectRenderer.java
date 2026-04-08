package mekanism.client.render.lib.effect;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.RenderState;
import mekanism.common.lib.effect.CustomEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class BillboardingEffectRenderer {

    private BillboardingEffectRenderer() {
    }

    private static final Minecraft minecraft = Minecraft.getMinecraft();

    public static void render(CustomEffect effect, BlockPos renderPos, double x, double y, double z, long time, float partialTick) {
        int gridSize = effect.getTextureGridSize();
        if (gridSize <= 0) {
            return;
        }

        int tick = (int) (time % (gridSize * gridSize));
        int xIndex = tick % gridSize;
        int yIndex = tick / gridSize;
        float spriteSize = 1F / gridSize;

        Vec3d localPos = effect.getPos(partialTick).subtract(new Vec3d(renderPos)).add(x, y, z);
        int[] color = effect.getColor().rgbaArray();
        float minU = xIndex * spriteSize;
        float maxU = minU + spriteSize;
        float minV = yIndex * spriteSize;
        float maxV = minV + spriteSize;

        GlStateManager.pushMatrix();
        GlStateManager.translate(localPos.x, localPos.y, localPos.z);

        RenderManager renderManager = minecraft.getRenderManager();
        GlStateManager.rotate(-renderManager.playerViewY, 0, 1, 0);
        float pitchRotation = minecraft.gameSettings.thirdPersonView == 2 ? -renderManager.playerViewX : renderManager.playerViewX;
        GlStateManager.rotate(pitchRotation, 1, 0, 0);

        float scale = effect.getScale();
        GlStateManager.scale(scale, scale, scale);

        minecraft.getTextureManager().bindTexture(effect.getTexture());
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        RenderState previousState = MekanismRenderer.pauseRenderer(tessellator);
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        buffer.pos(-1.0D, 1.0D, 0.0D).tex(minU, maxV).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(1.0D, 1.0D, 0.0D).tex(maxU, maxV).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(1.0D, -1.0D, 0.0D).tex(maxU, minV).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(-1.0D, -1.0D, 0.0D).tex(minU, minV).color(color[0], color[1], color[2], color[3]).endVertex();

        tessellator.draw();
        MekanismRenderer.resumeRenderer(tessellator, previousState);

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
