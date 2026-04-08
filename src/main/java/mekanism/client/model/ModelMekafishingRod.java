package mekanism.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelMekafishingRod extends ModelBase {

    ModelRenderer body;
    ModelRenderer bottom_connector_r1;
    ModelRenderer spool_r1;
    ModelRenderer connector_r1;
    ModelRenderer connector_r2;
    ModelRenderer wheel_r1;
    ModelRenderer bait_module;
    ModelRenderer body_r1;
    ModelRenderer intelligent_module;
    ModelRenderer collection_module;
    ModelRenderer body_r2;
    ModelRenderer catching_module;
    ModelRenderer body_r3;
    ModelRenderer body_r4;
    ModelRenderer body_r5;
    ModelRenderer collection_module2;
    ModelRenderer body_r6;
    ModelRenderer intelligent_module2;
    ModelRenderer a;
    ModelRenderer wheel_handle_r1;
    ModelRenderer b;
    ModelRenderer wheel_handle_r2;

    public ModelMekafishingRod() {
        textureWidth = 32;
        textureHeight = 32;

        body = new ModelRenderer(this);
        body.setRotationPoint(0.0F, 24.0F, 0.0F);
        body.cubeList.add(new ModelBox(body, 4, 16, -1.0F, -6.0F, -1.0F, 2, 1, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 4, 19, -0.5F, -5.0F, -0.5F, 1, 4, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 12, 5, -1.0F, -1.0F, -1.0F, 2, 2, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 8, 19, -0.5F, -4.0F, 0.5F, 1, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 18, 13, -0.5F, -8.0F, -1.0F, 1, 2, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 0, 0, -0.5F, -29.0F, -0.5F, 1, 21, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 20, 6, 0.0F, -11.5F, 0.5F, 0, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 20, 8, 0.0F, -13.5F, 0.5F, 0, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 20, 4, -0.2F, -5.0F, -0.7F, 1, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 20, 4, -0.8F, -5.0F, -0.7F, 1, 1, 1, 0.0F, true));

        bottom_connector_r1 = new ModelRenderer(this);
        bottom_connector_r1.setRotationPoint(0.0F, -10.0F, 2.5F);
        body.addChild(bottom_connector_r1);
        setRotationAngle(bottom_connector_r1, 0.0873F, 0.0F, 0.0F);
        bottom_connector_r1.cubeList.add(new ModelBox(bottom_connector_r1, 16, 18, -0.5F, -5.0F, -1.0F, 1, 5, 1, 0.0F, false));

        spool_r1 = new ModelRenderer(this);
        spool_r1.setRotationPoint(0.0F, -7.0F, 2.0F);
        body.addChild(spool_r1);
        setRotationAngle(spool_r1, 0.0F, -0.7854F, 0.0F);
        spool_r1.cubeList.add(new ModelBox(spool_r1, 4, 6, -1.0F, -3.0F, -1.0F, 2, 3, 2, 0.0F, false));

        connector_r1 = new ModelRenderer(this);
        connector_r1.setRotationPoint(0.0F, -4.5F, 0.0F);
        body.addChild(connector_r1);
        setRotationAngle(connector_r1, 0.2182F, 0.0F, 0.0F);
        connector_r1.cubeList.add(new ModelBox(connector_r1, 12, 13, -1.0F, -4.0F, -1.0F, 2, 4, 1, 0.0F, false));

        connector_r2 = new ModelRenderer(this);
        connector_r2.setRotationPoint(0.0F, -3.0F, 1.5F);
        body.addChild(connector_r2);
        setRotationAngle(connector_r2, -0.2618F, 0.0F, 0.0F);
        connector_r2.cubeList.add(new ModelBox(connector_r2, 12, 18, -0.5F, -5.0F, -1.0F, 1, 5, 1, 0.0F, false));

        wheel_r1 = new ModelRenderer(this);
        wheel_r1.setRotationPoint(0.0F, -7.5F, 0.5F);
        body.addChild(wheel_r1);
        setRotationAngle(wheel_r1, -0.7854F, 0.0F, 0.0F);
        wheel_r1.cubeList.add(new ModelBox(wheel_r1, 4, 0, 0.5F, -1.5F, -1.5F, 1, 3, 3, 0.0F, false));
        wheel_r1.cubeList.add(new ModelBox(wheel_r1, 4, 0, -1.5F, -1.5F, -1.5F, 1, 3, 3, 0.0F, false));

        bait_module = new ModelRenderer(this);
        bait_module.setRotationPoint(0.0F, 24.0F, 0.0F);


        body_r1 = new ModelRenderer(this);
        body_r1.setRotationPoint(0.0F, -25.0F, 0.0F);
        bait_module.addChild(body_r1);
        setRotationAngle(body_r1, 0.0F, -0.7854F, 0.0F);
        body_r1.cubeList.add(new ModelBox(body_r1, 4, 11, -1.0F, -3.0F, -1.0F, 2, 3, 2, 0.0F, false));

        intelligent_module = new ModelRenderer(this);
        intelligent_module.setRotationPoint(0.0F, 24.0F, -1.0F);
        intelligent_module.cubeList.add(new ModelBox(intelligent_module, 12, 0, 0.0F, -11.0F, -1.0F, 2, 3, 2, 0.0F, false));

        collection_module = new ModelRenderer(this);
        collection_module.setRotationPoint(0.0F, 24.0F, 0.0F);


        body_r2 = new ModelRenderer(this);
        body_r2.setRotationPoint(1.0F, -11.0F, -1.0F);
        collection_module.addChild(body_r2);
        setRotationAngle(body_r2, 0.0F, -0.7854F, 0.0F);
        body_r2.cubeList.add(new ModelBox(body_r2, 12, 9, -1.0F, -2.0F, -1.0F, 2, 2, 2, 0.0F, false));

        catching_module = new ModelRenderer(this);
        catching_module.setRotationPoint(0.0F, 24.0F, 0.0F);


        body_r3 = new ModelRenderer(this);
        body_r3.setRotationPoint(0.0F, -20.0F, 0.3F);
        catching_module.addChild(body_r3);
        setRotationAngle(body_r3, 0.7854F, 0.0F, 0.0F);
        body_r3.cubeList.add(new ModelBox(body_r3, 20, 2, -1.0F, -1.0F, -1.0F, 2, 1, 1, 0.0F, false));

        body_r4 = new ModelRenderer(this);
        body_r4.setRotationPoint(0.0F, -22.0F, 0.3F);
        catching_module.addChild(body_r4);
        setRotationAngle(body_r4, 0.7854F, 0.0F, 0.0F);
        body_r4.cubeList.add(new ModelBox(body_r4, 20, 2, -1.0F, -1.0F, -1.0F, 2, 1, 1, 0.0F, false));

        body_r5 = new ModelRenderer(this);
        body_r5.setRotationPoint(0.0F, -24.0F, 0.3F);
        catching_module.addChild(body_r5);
        setRotationAngle(body_r5, 0.7854F, 0.0F, 0.0F);
        body_r5.cubeList.add(new ModelBox(body_r5, 20, 2, -1.0F, -1.0F, -1.0F, 2, 1, 1, 0.0F, false));

        collection_module2 = new ModelRenderer(this);
        collection_module2.setRotationPoint(0.0F, 24.0F, 0.0F);


        body_r6 = new ModelRenderer(this);
        body_r6.setRotationPoint(-1.0F, -11.0F, -1.0F);
        collection_module2.addChild(body_r6);
        setRotationAngle(body_r6, 0.0F, -0.7854F, 0.0F);
        body_r6.cubeList.add(new ModelBox(body_r6, 12, 9, -1.0F, -2.0F, -1.0F, 2, 2, 2, 0.0F, false));

        intelligent_module2 = new ModelRenderer(this);
        intelligent_module2.setRotationPoint(0.0F, 24.0F, -1.0F);
        intelligent_module2.cubeList.add(new ModelBox(intelligent_module2, 12, 0, -2.0F, -11.0F, -1.0F, 2, 3, 2, 0.0F, false));

        a = new ModelRenderer(this);
        a.setRotationPoint(0.0F, 24.0F, 0.0F);


        wheel_handle_r1 = new ModelRenderer(this);
        wheel_handle_r1.setRotationPoint(0.0F, -7.5F, 0.5F);
        a.addChild(wheel_handle_r1);
        setRotationAngle(wheel_handle_r1, -0.7854F, 0.0F, 0.0F);
        wheel_handle_r1.cubeList.add(new ModelBox(wheel_handle_r1, 20, 0, -3.5F, -1.5F, 0.5F, 2, 1, 1, 0.0F, false));

        b = new ModelRenderer(this);
        b.setRotationPoint(0.0F, 24.0F, 0.0F);


        wheel_handle_r2 = new ModelRenderer(this);
        wheel_handle_r2.setRotationPoint(0.0F, -7.5F, 0.5F);
        b.addChild(wheel_handle_r2);
        setRotationAngle(wheel_handle_r2, -0.7854F, 0.0F, 0.0F);
        wheel_handle_r2.cubeList.add(new ModelBox(wheel_handle_r2, 20, 0, 1.5F, -1.5F, 0.5F, 2, 1, 1, 0.0F, false));
    }

    public void render(float size, boolean renderBait, boolean renderIntelligent, boolean renderCollection, boolean renderCatching, boolean isleft) {
        body.render(size);
        if (renderBait) {
            bait_module.render(size);
        }
        if (renderCatching) {
            catching_module.render(size);
        }
        if (isleft) {
            if (renderIntelligent) {
                intelligent_module.render(size);
            }
            if (renderCollection) {
                collection_module.render(size);
            }
            a.render(size);
        } else {
            if (renderIntelligent) {
                intelligent_module2.render(size);
            }
            if (renderCollection) {
                collection_module2.render(size);
            }
            b.render(size);
        }
    }


    private void setRotationAngle(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }
}
