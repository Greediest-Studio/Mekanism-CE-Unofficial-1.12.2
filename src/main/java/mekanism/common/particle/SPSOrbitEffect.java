package mekanism.common.particle;

import mekanism.common.content.sps.SynchronizedSPSData;
import mekanism.common.lib.Color;
import mekanism.common.lib.effect.CustomEffect;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class SPSOrbitEffect extends CustomEffect {

    private static final ResourceLocation TEXTURE = MekanismUtils.getResource(ResourceType.RENDER, "sps_orbit_effect.png");

    private final Vec3d center;
    private final Vec3d start;
    private final Vec3d axis;

    private float speed = 0.5F;

    private SynchronizedSPSData multiblock;

    public SPSOrbitEffect(SynchronizedSPSData multiblock, Vec3d center) {
        super(TEXTURE, 1);
        this.multiblock = multiblock;
        this.center = center;
        double radius = 1 + rand.nextDouble();
        start = randVec().scale(radius);
        pos = center.add(start);
        Vec3d randomAxis = randVec();
        if (randomAxis.lengthSquared() < 1.0E-6D) {
            randomAxis = new Vec3d(0, 1, 0);
        }
        axis = randomAxis.normalize();
        scale = 0.01F + rand.nextFloat() * 0.04F;
        color = Color.rgbai(102, 215, 237, 240);
    }

    public void updateMultiblock(SynchronizedSPSData multiblock) {
        this.multiblock = multiblock;
    }

    @Override
    public boolean tick() {
        if (super.tick()) {
            return true;
        }
        if (multiblock != null && multiblock.lastReceivedEnergy > 0) {
            speed = (float) Math.max(0.1D, Math.log10(multiblock.lastReceivedEnergy));
        } else {
            speed = 0.1F;
        }
        return false;
    }

    @Override
    public Vec3d getPos(float partialTick) {
        double angleRadians = Math.toRadians((ticker + partialTick) * speed);
        return center.add(rotateAroundAxis(start, axis, angleRadians));
    }

    private static Vec3d rotateAroundAxis(Vec3d vector, Vec3d axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3d term1 = vector.scale(cos);
        Vec3d term2 = axis.crossProduct(vector).scale(sin);
        Vec3d term3 = axis.scale(axis.dotProduct(vector) * (1 - cos));
        return term1.add(term2).add(term3);
    }
}
