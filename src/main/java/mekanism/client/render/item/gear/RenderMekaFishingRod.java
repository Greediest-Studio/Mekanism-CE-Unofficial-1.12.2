package mekanism.client.render.item.gear;

import mekanism.client.model.ModelMekafishingRod;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.MekanismModules;
import mekanism.common.item.ItemMekaFishingRod;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderMekaFishingRod extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;
    private static ModelMekafishingRod cube = new ModelMekafishingRod();


    @Override
    protected void renderBlockSpecific(@Nonnull ItemStack stack, TransformType transformType) {
    }

    @Override
    protected void renderItemSpecific(@Nonnull ItemStack stack, TransformType transformType) {
        boolean isBait = false;
        boolean isIntelligent = false;
        boolean isCollection = false;
        boolean isCatching = false;
        if (stack.getItem() instanceof ItemMekaFishingRod rod) {
            isBait = rod.isModuleEnabled(stack, MekanismModules.FISHING_SPEED_UNIT);
            isIntelligent = rod.isModuleEnabled(stack, MekanismModules.FISHING_INTELLIGENT_UNIT);
            isCollection = rod.isModuleEnabled(stack, MekanismModules.FISHING_COLLECTING_UNIT);
            isCatching = rod.isModuleEnabled(stack, MekanismModules.FISHING_MULTIPLE_UNIT);
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.4F, 1.4F, 1.4F);
        GlStateManager.rotate(180, 0, 0, 1);

        switch (transformType) {
            case THIRD_PERSON_RIGHT_HAND:
                GlStateManager.rotate(45, 0, 1, 0);
                GlStateManager.rotate(68, 1, 0, 0);
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                // Keep the original hand anchor, then apply json display delta.
                GlStateManager.translate(0F, -0.4F, 0.4F);
                applyDisplayTransform(0, -11F, -5F, 0F, 0F, 0F, 1F, 1F, 1F);
                break;
            case THIRD_PERSON_LEFT_HAND:
                GlStateManager.rotate(-90, 0, 1, 0);
                GlStateManager.rotate(45, 0, 1, 0);
                GlStateManager.rotate(68, 1, 0, 0);
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                // Keep the original hand anchor, then apply json display delta.
                GlStateManager.translate(0F, -0.4F, 0.4F);
                applyDisplayTransform(0, -11F, -5F, 0F, 0F, 0F, 1F, 1F, 1F);
                break;
            case FIRST_PERSON_RIGHT_HAND:
                GlStateManager.rotate(45, 0, 1, 0);
                GlStateManager.translate(0, -0.7F, 0);
                applyDisplayTransform(2.5F, -5.25F, 0F, 0F, 0F, 0F, 1F, 1F, 1F);
                break;
            case FIRST_PERSON_LEFT_HAND:
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.rotate(45, 0, 1, 0);
                GlStateManager.translate(0, -0.7F, 0);
                applyDisplayTransform(-2.5F, -5.25F, 0F, 0F, 0F, 0F, 1F, 1F, 1F);
                break;
            case GUI:
                applyDisplayTransform(-1.5F, -2.25F, 0F, 56F, -142F, 51F, 0.6F, 0.6F, 0.6F);
                break;
            case GROUND:
                applyDisplayTransform(0F, 5F, 0F, 0F, 0F, 0F, 1F, 1F, 1F);
                break;
            case HEAD:
                applyDisplayTransform(0F, 15.25F, 0F, 0F, 0F, 0F, 1F, 1F, 1F);
                break;
            case FIXED:
                applyDisplayTransform(0F, 0F, -3.5F, -90F, 45F, 90F, 1F, 1F, 1F);
                break;
            default:
                break;
        }

        MekanismRenderer.bindTexture(MekanismUtils.getResource(ResourceType.RENDER, "mekafishingrod.png"));
        boolean isleft = false;
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            if (player.getHeldItemOffhand() == stack) {
                isleft = true;
            }
        }
        cube.render(0.0625F, isBait, isIntelligent, isCollection, isCatching, isleft);
        GlStateManager.popMatrix();
    }

    private static void applyDisplayTransform(float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) {
        GlStateManager.translate(tx / 16F, ty / 16F, tz / 16F);
        GlStateManager.rotate(rx, 1, 0, 0);
        GlStateManager.rotate(ry, 0, 1, 0);
        GlStateManager.rotate(rz, 0, 0, 1);
        GlStateManager.scale(sx, sy, sz);
    }

    @Nonnull
    @Override
    protected TransformType getTransform(@Nonnull ItemStack stack) {
        return model.getTransform();
    }
}
