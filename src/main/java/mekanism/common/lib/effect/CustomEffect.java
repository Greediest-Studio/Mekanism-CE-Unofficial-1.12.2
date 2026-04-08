package mekanism.common.lib.effect;

import mekanism.common.lib.Color;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class CustomEffect {

    private final int gridSize;
    private final ResourceLocation texture;

    protected final Random rand = new Random();

    protected Vec3d pos = new Vec3d(0, 0, 0);
    protected Color color = Color.rgbai(255, 255, 255, 255);
    protected float scale = 1F;

    protected int ticker;

    public CustomEffect(ResourceLocation texture) {
        this(texture, 4);
    }

    public CustomEffect(ResourceLocation texture, int gridSize) {
        this.texture = texture;
        this.gridSize = gridSize;
    }

    protected Vec3d randVec() {
        return new Vec3d(rand.nextDouble() - 0.5D, rand.nextDouble() - 0.5D, rand.nextDouble() - 0.5D).normalize();
    }

    public boolean tick() {
        ticker++;
        return false;
    }

    public void setPos(Vec3d pos) {
        this.pos = pos;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public Vec3d getPos(float partialTick) {
        return pos;
    }

    public float getScale() {
        return scale;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getTextureGridSize() {
        return gridSize;
    }
}
