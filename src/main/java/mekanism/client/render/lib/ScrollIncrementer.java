package mekanism.client.render.lib;

import net.minecraft.client.Minecraft;


public class ScrollIncrementer {

    private final boolean discrete;
    private long lastScrollTime = -1;
    private double scrollDelta;

    public ScrollIncrementer(boolean discrete) {
        this.discrete = discrete;
    }


    public int scroll(double delta) {
        long time = Minecraft.getMinecraft().ingameGUI.getUpdateCounter();
        if (time - lastScrollTime > 20) {
            scrollDelta = 0;
        }
        lastScrollTime = time;
        scrollDelta += delta;
        int shift = (int) scrollDelta;
        scrollDelta %= 1;
        if (discrete) {
            shift = clamp(shift, -1, 1);
        }
        return shift;
    }

    public int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}