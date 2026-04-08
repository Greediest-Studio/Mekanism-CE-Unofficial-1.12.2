package mekanism.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelFishRod extends ModelBase {
    ModelRenderer rod;
    ModelRenderer bb_main_r1;

    public ModelFishRod() {
        textureWidth = 16;
        textureHeight = 16;

        rod = new ModelRenderer(this);
        rod.setRotationPoint(0.0F, 24.0F, 0.0F);
        rod.cubeList.add(new ModelBox(rod, 0, 0, -1.5F, -3.0F, -1.5F, 3, 3, 3, 0.0F, false));
        rod.cubeList.add(new ModelBox(rod, 0, 6, -0.5F, -5.0F, -0.5F, 1, 6, 1, 0.0F, false));
        rod.cubeList.add(new ModelBox(rod, 4, 10, -1.0F, -3.5F, -1.0F, 2, 1, 2, 0.0F, false));

        bb_main_r1 = new ModelRenderer(this);
        bb_main_r1.setRotationPoint(0.0F, 0.6464F, 1.2678F);
        rod.addChild(bb_main_r1);
        setRotationAngle(bb_main_r1, 0.7854F, 0.0F, 0.0F);
        bb_main_r1.cubeList.add(new ModelBox(bb_main_r1, 4, 6, -1.0F, -1.0F, -1.9F, 1, 2, 2, 0.0F, false));
    }

    public void render(float size) {
        rod.render(size);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
