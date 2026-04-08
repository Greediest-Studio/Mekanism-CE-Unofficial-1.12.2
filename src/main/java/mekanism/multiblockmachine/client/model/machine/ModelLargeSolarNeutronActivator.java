package mekanism.multiblockmachine.client.model.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ModelLargeSolarNeutronActivator extends ModelBase {

    public static ResourceLocation LASER = MekanismUtils.getResource(MekanismMultiblockMachine.MODID, ResourceType.RENDER_MACHINE, "LargeSolarNeutronActivator/LASER.png");
    public static ResourceLocation LED = MekanismUtils.getResource(MekanismMultiblockMachine.MODID, ResourceType.RENDER_MACHINE, "LargeSolarNeutronActivator/LED.png");
    public static ResourceLocation SCREEN = MekanismUtils.getResource(MekanismMultiblockMachine.MODID, ResourceType.RENDER_MACHINE, "LargeSolarNeutronActivator/SCREEN.png");

    ModelRenderer top;
    ModelRenderer solar_panel;
    ModelRenderer screen;
    ModelRenderer ports;
    ModelRenderer port_back1;
    ModelRenderer port_back2;
    ModelRenderer port_west;
    ModelRenderer port_east;
    ModelRenderer port;
    ModelRenderer body;
    ModelRenderer cover_corner_r1;
    ModelRenderer keyboard_r1;
    ModelRenderer tubes;
    ModelRenderer tubes2;
    ModelRenderer top1_r1;
    ModelRenderer working_station;
    ModelRenderer top_r1;
    ModelRenderer top2_r1;
    ModelRenderer device1;
    ModelRenderer top_r2;
    ModelRenderer bottom_r1;
    ModelRenderer device2;
    ModelRenderer bottom_r2;
    ModelRenderer device3;
    ModelRenderer back;
    ModelRenderer body2;
    ModelRenderer body3;
    ModelRenderer tank;
    ModelRenderer top_side;
    ModelRenderer top_side2;
    ModelRenderer top_side3;
    ModelRenderer top_side4;
    ModelRenderer tank2;
    ModelRenderer top_side5;
    ModelRenderer top_side6;
    ModelRenderer top_side7;
    ModelRenderer top_side8;
    ModelRenderer laser;
    ModelRenderer laser_r1;
    ModelRenderer laser_r2;
    ModelRenderer glass;


    public ModelLargeSolarNeutronActivator() {
        textureWidth = 512;
        textureHeight = 512;

        top = new ModelRenderer(this);
        top.setRotationPoint(0.0F, 0.0F, 0.0F);
        top.cubeList.add(new ModelBox(top, 281, 104, 16.0F, -22.0F, 16.0F, 5, 3, 5, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 224, 80, 1.0F, -24.0F, -23.0F, 22, 2, 21, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 0, 198, 5.0F, -24.0F, -2.0F, 18, 2, 25, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 185, 0, -23.0F, -24.0F, -2.0F, 18, 2, 25, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 185, 28, -23.0F, -24.0F, -23.0F, 22, 2, 21, 0.0F, false));

        solar_panel = new ModelRenderer(this);
        solar_panel.setRotationPoint(23.0F, -22.0F, -23.0F);
        top.addChild(solar_panel);


        screen = new ModelRenderer(this);
        screen.setRotationPoint(0.0F, 0.0F, 0.0F);
        screen.cubeList.add(new ModelBox(screen, 0, 504, 9.0F, -15.0F, -23.0F, 12, 8, 0, 0.0F, false));
        screen.cubeList.add(new ModelBox(screen, 24, 504, 9.0F, -3.0F, -23.0F, 12, 8, 0, 0.0F, false));

        ports = new ModelRenderer(this);
        ports.setRotationPoint(8.0F, 8.0F, -8.0F);


        port_back1 = new ModelRenderer(this);
        port_back1.setRotationPoint(-5.0F, 0.0F, 30.0F);
        ports.addChild(port_back1);
        port_back1.cubeList.add(new ModelBox(port_back1, 282, 239, -23.0F, 4.0F, 1.0F, 8, 8, 1, 0.0F, false));
        port_back1.cubeList.add(new ModelBox(port_back1, 66, 280, 17.0F, 5.0F, -9.0F, 3, 6, 6, 0.0F, false));

        port_back2 = new ModelRenderer(this);
        port_back2.setRotationPoint(-5.0F, 0.0F, 30.0F);
        ports.addChild(port_back2);
        port_back2.cubeList.add(new ModelBox(port_back2, 103, 166, 20.0F, 4.0F, -10.0F, 1, 8, 8, 0.0F, false));
        port_back2.cubeList.add(new ModelBox(port_back2, 85, 280, -26.0F, 5.0F, -9.0F, 3, 6, 6, 0.0F, false));

        port_west = new ModelRenderer(this);
        port_west.setRotationPoint(-5.0F, 0.0F, 30.0F);
        ports.addChild(port_west);
        port_west.cubeList.add(new ModelBox(port_west, 242, 264, -27.0F, 4.0F, -10.0F, 1, 8, 8, 0.0F, false));
        port_west.cubeList.add(new ModelBox(port_west, 0, 0, -26.0F, 11.0F, -45.0F, 46, 5, 46, 0.0F, false));

        port_east = new ModelRenderer(this);
        port_east.setRotationPoint(-5.0F, 0.0F, 30.0F);
        ports.addChild(port_east);
        port_east.cubeList.add(new ModelBox(port_east, 0, 139, -26.0F, 0.0F, -43.0F, 46, 11, 15, 0.0F, false));
        port_east.cubeList.add(new ModelBox(port_east, 0, 100, -24.0F, 0.0F, -28.0F, 42, 11, 27, 0.0F, false));

        port = new ModelRenderer(this);
        port.setRotationPoint(0.0F, 0.0F, 0.0F);
        ports.addChild(port);
        port.cubeList.add(new ModelBox(port, 282, 229, 5.0F, 5.0F, 28.0F, 6, 6, 3, 0.0F, false));
        port.cubeList.add(new ModelBox(port, 282, 239, 4.0F, 4.0F, 31.0F, 8, 8, 1, 0.0F, false));
        port.cubeList.add(new ModelBox(port, 282, 229, -27.0F, 5.0F, 28.0F, 6, 6, 3, 0.0F, false));

        body = new ModelRenderer(this);
        body.setRotationPoint(8.0F, 8.0F, -8.0F);
        body.cubeList.add(new ModelBox(body, 139, 100, 0.0F, -24.0F, -13.0F, 15, 24, 27, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 103, 184, -30.0F, -24.0F, -15.0F, 4, 24, 29, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 170, 184, -4.0F, -24.0F, -15.0F, 4, 24, 29, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 123, 152, -26.0F, -2.0F, -15.0F, 22, 2, 29, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 0, 166, -26.0F, -24.0F, -15.0F, 22, 2, 29, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 0, 52, -31.0F, -27.0F, -15.0F, 46, 3, 44, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 272, 0, -31.0F, -27.0F, 29.0F, 18, 3, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 272, 6, -3.0F, -27.0F, 29.0F, 18, 3, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 272, 12, 0.0F, -24.0F, -15.0F, 14, 10, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 272, 25, 0.0F, -12.0F, -15.0F, 14, 10, 2, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 224, 104, -31.0F, -7.0F, -13.0F, 1, 7, 27, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 51, 226, -31.0F, -24.0F, -9.0F, 1, 17, 23, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 231, 264, -10.0F, -28.0F, 29.0F, 4, 32, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 272, 38, -10.0F, -28.0F, 20.0F, 4, 1, 9, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 87, 198, 13.0F, 0.0F, 5.0F, 1, 11, 3, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 226, 157, 13.0F, 0.0F, 10.0F, 1, 11, 3, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 286, 157, 13.0F, 0.0F, 15.0F, 1, 11, 3, 0.0F, false));

        cover_corner_r1 = new ModelRenderer(this);
        cover_corner_r1.setRotationPoint(-30.0F, -7.0F, -13.0F);
        body.addChild(cover_corner_r1);
        setRotationAngle(cover_corner_r1, 0.7854F, 0.0F, 0.0F);
        cover_corner_r1.cubeList.add(new ModelBox(cover_corner_r1, 123, 139, -0.999F, 0.0F, 0.0F, 1, 3, 6, 0.0F, false));

        keyboard_r1 = new ModelRenderer(this);
        keyboard_r1.setRotationPoint(12.0F, 0.0F, -13.0F);
        body.addChild(keyboard_r1);
        setRotationAngle(keyboard_r1, 0.3927F, 0.0F, 0.0F);
        keyboard_r1.cubeList.add(new ModelBox(keyboard_r1, 204, 238, -10.0F, 0.0F, -3.0F, 10, 1, 5, 0.0F, false));

        tubes = new ModelRenderer(this);
        tubes.setRotationPoint(0.0F, 0.0F, 0.0F);
        body.addChild(tubes);
        tubes.cubeList.add(new ModelBox(tubes, 286, 172, -30.0F, 0.0F, 15.0F, 1, 11, 3, 0.0F, false));
        tubes.cubeList.add(new ModelBox(tubes, 286, 187, -30.0F, 0.0F, 10.0F, 1, 11, 3, 0.0F, false));
        tubes.cubeList.add(new ModelBox(tubes, 286, 202, -30.0F, 0.0F, 5.0F, 1, 11, 3, 0.0F, false));

        tubes2 = new ModelRenderer(this);
        tubes2.setRotationPoint(0.0F, 0.0F, 0.0F);
        body.addChild(tubes2);
        tubes2.cubeList.add(new ModelBox(tubes2, 226, 139, -23.0F, -3.0F, -6.0F, 16, 1, 16, 0.0F, false));
        tubes2.cubeList.add(new ModelBox(tubes2, 181, 80, -20.0F, -8.0F, -3.0F, 10, 5, 10, 0.0F, false));

        top1_r1 = new ModelRenderer(this);
        top1_r1.setRotationPoint(-15.0F, -8.0F, 2.0F);
        tubes2.addChild(top1_r1);
        setRotationAngle(top1_r1, 0.0F, 0.0F, -0.7854F);
        top1_r1.cubeList.add(new ModelBox(top1_r1, 0, 266, 14.0F, -6.0F, -8.0F, 4, 8, 16, 0.0F, false));

        working_station = new ModelRenderer(this);
        working_station.setRotationPoint(0.0F, 0.0F, 0.0F);
        working_station.cubeList.add(new ModelBox(working_station, 181, 96, -8.0F, -8.0F, -7.0F, 2, 1, 2, 0.0F, false));
        working_station.cubeList.add(new ModelBox(working_station, 157, 283, -9.0F, -12.0F, -8.0F, 4, 4, 4, 0.0F, false));
        working_station.cubeList.add(new ModelBox(working_station, 278, 58, -10.0F, -14.0F, -9.0F, 6, 2, 6, 0.0F, false));

        top_r1 = new ModelRenderer(this);
        top_r1.setRotationPoint(-7.0F, 0.0F, -6.0F);
        working_station.addChild(top_r1);
        setRotationAngle(top_r1, 0.0F, 0.0F, 0.7854F);
        top_r1.cubeList.add(new ModelBox(top_r1, 190, 96, -1.0F, -8.0F, -1.0F, 2, 1, 2, 0.0F, false));

        top2_r1 = new ModelRenderer(this);
        top2_r1.setRotationPoint(-7.0F, 0.0F, -6.0F);
        working_station.addChild(top2_r1);
        setRotationAngle(top2_r1, 0.0F, 0.0F, -0.7854F);
        top2_r1.cubeList.add(new ModelBox(top2_r1, 145, 253, -2.0F, -18.0F, -8.0F, 8, 4, 16, 0.0F, false));

        device1 = new ModelRenderer(this);
        device1.setRotationPoint(-7.0F, 0.0F, -6.0F);
        working_station.addChild(device1);


        top_r2 = new ModelRenderer(this);
        top_r2.setRotationPoint(0.0F, 0.0F, 0.0F);
        device1.addChild(top_r2);
        setRotationAngle(top_r2, 0.0F, 0.0F, -0.7854F);
        top_r2.cubeList.add(new ModelBox(top_r2, 199, 96, -1.0F, -8.0F, -1.0F, 2, 1, 2, 0.0F, false));

        bottom_r1 = new ModelRenderer(this);
        bottom_r1.setRotationPoint(0.0F, 0.0F, 0.0F);
        device1.addChild(bottom_r1);
        setRotationAngle(bottom_r1, 0.0F, 0.0F, 0.7854F);
        bottom_r1.cubeList.add(new ModelBox(bottom_r1, 278, 67, -3.0F, -14.0F, -3.0F, 6, 2, 6, 0.0F, false));
        bottom_r1.cubeList.add(new ModelBox(bottom_r1, 174, 283, -2.0F, -12.0F, -2.0F, 4, 4, 4, 0.0F, false));

        device2 = new ModelRenderer(this);
        device2.setRotationPoint(-7.0F, 0.0F, -6.0F);
        working_station.addChild(device2);
        device2.cubeList.add(new ModelBox(device2, 0, 226, 2.0F, -16.0F, 12.0F, 10, 24, 15, 0.0F, false));

        bottom_r2 = new ModelRenderer(this);
        bottom_r2.setRotationPoint(0.0F, 0.0F, 0.0F);
        device2.addChild(bottom_r2);
        setRotationAngle(bottom_r2, 0.0F, 0.0F, -0.7854F);
        bottom_r2.cubeList.add(new ModelBox(bottom_r2, 41, 279, -3.0F, -14.0F, -3.0F, 6, 2, 6, 0.0F, false));
        bottom_r2.cubeList.add(new ModelBox(bottom_r2, 123, 285, -2.0F, -12.0F, -2.0F, 4, 4, 4, 0.0F, false));

        device3 = new ModelRenderer(this);
        device3.setRotationPoint(-7.0F, 0.0F, -6.0F);
        working_station.addChild(device3);
        device3.cubeList.add(new ModelBox(device3, 87, 213, -3.0F, 8.0F, 27.0F, 3, 11, 1, 0.0F, false));
        device3.cubeList.add(new ModelBox(device3, 41, 288, 14.0F, 8.0F, 27.0F, 3, 11, 1, 0.0F, false));
        device3.cubeList.add(new ModelBox(device3, 145, 238, 12.0F, 5.0F, 18.0F, 18, 3, 11, 0.0F, false));

        back = new ModelRenderer(this);
        back.setRotationPoint(21.0F, 8.0F, 7.0F);
        back.cubeList.add(new ModelBox(back, 263, 249, -16.0F, -24.0F, 5.0F, 7, 21, 11, 0.0F, false));
        back.cubeList.add(new ModelBox(back, 237, 157, -16.0F, -24.0F, -1.0F, 18, 24, 6, 0.0F, false));
        back.cubeList.add(new ModelBox(back, 204, 249, -44.0F, -3.0F, 5.0F, 18, 3, 11, 0.0F, false));

        body2 = new ModelRenderer(this);
        body2.setRotationPoint(0.0F, 0.0F, 0.0F);
        back.addChild(body2);
        body2.cubeList.add(new ModelBox(body2, 194, 264, -33.0F, -24.0F, 5.0F, 7, 21, 11, 0.0F, false));
        body2.cubeList.add(new ModelBox(body2, 237, 188, -44.0F, -24.0F, -1.0F, 18, 24, 6, 0.0F, false));
        body2.cubeList.add(new ModelBox(body2, 208, 96, -2.0F, -24.0F, 6.0F, 3, 2, 1, 0.0F, false));

        body3 = new ModelRenderer(this);
        body3.setRotationPoint(-42.0F, 0.0F, 0.0F);
        back.addChild(body3);
        body3.cubeList.add(new ModelBox(body3, 217, 96, 35.0F, -24.0F, 6.0F, 2, 2, 1, 0.0F, false));
        body3.cubeList.add(new ModelBox(body3, 123, 149, 37.0F, -23.0F, 6.0F, 3, 1, 1, 0.0F, false));
        body3.cubeList.add(new ModelBox(body3, 182, 274, 42.0F, -24.0F, 12.0F, 1, 2, 3, 0.0F, false));

        tank = new ModelRenderer(this);
        tank.setRotationPoint(-4.5F, -28.0F, -23.5F);
        back.addChild(tank);
        tank.cubeList.add(new ModelBox(tank, 182, 280, -35.5F, 5.0F, 29.5F, 3, 1, 1, 0.0F, false));
        tank.cubeList.add(new ModelBox(tank, 104, 290, -38.5F, 4.0F, 35.5F, 1, 2, 3, 0.0F, false));

        top_side = new ModelRenderer(this);
        top_side.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank.addChild(top_side);
        top_side.cubeList.add(new ModelBox(top_side, 96, 198, 4.5F, 4.0F, 30.5F, 1, 2, 2, 0.0F, false));
        top_side.cubeList.add(new ModelBox(top_side, 290, 131, 4.5F, 5.0F, 32.5F, 1, 1, 3, 0.0F, false));
        top_side.cubeList.add(new ModelBox(top_side, 204, 245, -3.5F, 4.0F, 37.5F, 3, 2, 1, 0.0F, false));

        top_side2 = new ModelRenderer(this);
        top_side2.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank.addChild(top_side2);
        top_side2.cubeList.add(new ModelBox(top_side2, 96, 218, 2.5F, 4.0F, 37.5F, 2, 2, 1, 0.0F, false));
        top_side2.cubeList.add(new ModelBox(top_side2, 278, 76, -0.5F, 5.0F, 37.5F, 3, 1, 1, 0.0F, false));
        top_side2.cubeList.add(new ModelBox(top_side2, 281, 131, -3.5F, 4.0F, 29.5F, 1, 2, 3, 0.0F, false));

        top_side3 = new ModelRenderer(this);
        top_side3.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank.addChild(top_side3);
        top_side3.cubeList.add(new ModelBox(top_side3, 96, 203, -3.5F, 4.0F, 35.5F, 1, 2, 2, 0.0F, false));
        top_side3.cubeList.add(new ModelBox(top_side3, 242, 290, -3.5F, 5.0F, 32.5F, 1, 1, 3, 0.0F, false));
        top_side3.cubeList.add(new ModelBox(top_side3, 237, 219, -4.5F, 6.0F, 28.5F, 11, 18, 11, 0.0F, false));

        top_side4 = new ModelRenderer(this);
        top_side4.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank.addChild(top_side4);
        top_side4.cubeList.add(new ModelBox(top_side4, 41, 267, -4.5F, 24.0F, 28.5F, 10, 1, 10, 0.0F, false));
        top_side4.cubeList.add(new ModelBox(top_side4, 213, 245, -38.5F, 4.0F, 29.5F, 3, 2, 1, 0.0F, false));
        top_side4.cubeList.add(new ModelBox(top_side4, 96, 222, -32.5F, 4.0F, 29.5F, 2, 2, 1, 0.0F, false));

        tank2 = new ModelRenderer(this);
        tank2.setRotationPoint(-4.5F, -28.0F, -23.5F);
        back.addChild(tank2);
        tank2.cubeList.add(new ModelBox(tank2, 281, 122, -37.5F, -2.0F, -4.5F, 5, 3, 5, 0.0F, false));
        tank2.cubeList.add(new ModelBox(tank2, 242, 281, -37.5F, -2.0F, 32.5F, 5, 3, 5, 0.0F, false));

        top_side5 = new ModelRenderer(this);
        top_side5.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank2.addChild(top_side5);
        top_side5.cubeList.add(new ModelBox(top_side5, 96, 208, -38.5F, 4.0F, 30.5F, 1, 2, 2, 0.0F, false));
        top_side5.cubeList.add(new ModelBox(top_side5, 251, 290, -38.5F, 5.0F, 32.5F, 1, 1, 3, 0.0F, false));
        top_side5.cubeList.add(new ModelBox(top_side5, 222, 245, -32.5F, 4.0F, 37.5F, 3, 2, 1, 0.0F, false));

        top_side6 = new ModelRenderer(this);
        top_side6.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank2.addChild(top_side6);
        top_side6.cubeList.add(new ModelBox(top_side6, 59, 288, -37.5F, 4.0F, 37.5F, 2, 2, 1, 0.0F, false));
        top_side6.cubeList.add(new ModelBox(top_side6, 287, 76, -35.5F, 5.0F, 37.5F, 3, 1, 1, 0.0F, false));
        top_side6.cubeList.add(new ModelBox(top_side6, 113, 290, -30.5F, 4.0F, 29.5F, 1, 2, 3, 0.0F, false));

        top_side7 = new ModelRenderer(this);
        top_side7.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank2.addChild(top_side7);
        top_side7.cubeList.add(new ModelBox(top_side7, 96, 213, -30.5F, 4.0F, 35.5F, 1, 2, 2, 0.0F, false));
        top_side7.cubeList.add(new ModelBox(top_side7, 0, 291, -30.5F, 5.0F, 32.5F, 1, 1, 3, 0.0F, false));
        top_side7.cubeList.add(new ModelBox(top_side7, 100, 238, -39.5F, 6.0F, 28.5F, 11, 18, 11, 0.0F, false));

        top_side8 = new ModelRenderer(this);
        top_side8.setRotationPoint(0.0F, 0.0F, 0.0F);
        tank2.addChild(top_side8);
        top_side8.cubeList.add(new ModelBox(top_side8, 82, 268, -38.5F, 24.0F, 28.5F, 10, 1, 10, 0.0F, false));
        top_side8.cubeList.add(new ModelBox(top_side8, 181, 52, -28.5F, -2.0F, 4.5F, 24, 3, 24, 0.0F, false));
        top_side8.cubeList.add(new ModelBox(top_side8, 281, 113, -0.5F, -2.0F, -4.5F, 5, 3, 5, 0.0F, false));

        laser = new ModelRenderer(this);
        laser.setRotationPoint(0.0F, 0.0F, 0.0F);
        laser.cubeList.add(new ModelBox(laser, 0, 0, -7.5F, -8.0F, -6.5F, 1, 8, 1, 0.0F, false));

        laser_r1 = new ModelRenderer(this);
        laser_r1.setRotationPoint(-7.0F, 0.0F, -6.0F);
        laser.addChild(laser_r1);
        setRotationAngle(laser_r1, 0.0F, 0.0F, -0.7854F);
        laser_r1.cubeList.add(new ModelBox(laser_r1, 0, 0, -0.5F, -8.0F, -0.5F, 1, 8, 1, 0.0F, false));

        laser_r2 = new ModelRenderer(this);
        laser_r2.setRotationPoint(-7.0F, 0.0F, -6.0F);
        laser.addChild(laser_r2);
        setRotationAngle(laser_r2, 0.0F, 0.0F, 0.7854F);
        laser_r2.cubeList.add(new ModelBox(laser_r2, 0, 0, -0.5F, -8.0F, -0.5F, 1, 8, 1, 0.0F, false));

        glass = new ModelRenderer(this);
        glass.setRotationPoint(0.0F, 24.0F, 0.0F);
        glass.cubeList.add(new ModelBox(glass, 313, 0, -18.0F, -38.0F, -22.0F, 22, 20, 1, 0.0F, false));
    }

    public void renderItem(float size, TextureManager manager) {
        render(size, false, manager, false);
        glass.render(size);
    }

    public void render(float size, boolean on, TextureManager manager, boolean isEnableGlow) {
        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        doRender(size);
        GlStateManager.popMatrix();

        if (isEnableGlow) {
            GlStateManager.pushMatrix();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            manager.bindTexture(LED);
            GlStateManager.scale(1.001F, 1.001F, 1.001F);
            GlStateManager.translate(-0.0011F, -0.0011F, -0.0011F);
            MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
            doRender(size); //渲染灯光
            if (on) {
                GlStateManager.translate(-0.0011F, -0.0011F, -0.0011F);
                manager.bindTexture(SCREEN);
                screen.render(size);//渲染工作屏幕
                manager.bindTexture(LASER);
                laser.render(size);//渲染激光
            }
            MekanismRenderer.disableGlow(glowInfo);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }

        if (isEnableGlow) {
            GlStateManager.pushMatrix();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            manager.bindTexture(MekanismUtils.getResource(MekanismMultiblockMachine.MODID, ResourceType.RENDER_MACHINE, "LargeSolarNeutronActivator/BASE.png"));
            glass.render(size);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }

    }

    public void renderBloom(float size, boolean on, TextureManager manager) {
        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        manager.bindTexture(LED);
        GlStateManager.scale(1.0011F, 1.0011F, 1.0011F);
        GlStateManager.translate(-0.0012F, -0.0012F, -0.0012F);
        MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
        doRender(size); //渲染灯光
        if (on) {
            GlStateManager.translate(-0.0011F, -0.0011F, -0.0011F);
            manager.bindTexture(SCREEN);
            screen.render(size);//渲染工作屏幕
            manager.bindTexture(LASER);
            laser.render(size);//渲染激光
        }
        MekanismRenderer.disableGlow(glowInfo);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }


    public void doRender(float size) {
        top.render(size);
        screen.render(size);
        ports.render(size);
        body.render(size);
        working_station.render(size);
        back.render(size);
    }


    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

}
