package mekanism.client.sound;

import mekanism.common.config.MekanismConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

@SideOnly(Side.CLIENT)
public abstract class PlayerSound extends PositionedSound implements ITickableSound {

    @Nonnull
    private WeakReference<EntityPlayer> playerReference;
    private int subtitleFrequency;
    private float lastX;
    private float lastY;
    private float lastZ;

    private float fadeUpStep = 0.1f;
    private float fadeDownStep = 0.1f;

    private boolean donePlaying = false;
    private int consecutiveTicks;

    public PlayerSound(@Nonnull EntityPlayer player, @Nonnull ResourceLocation sound) {
        this(player, sound, 60);
    }

    public PlayerSound(@Nonnull EntityPlayer player, @Nonnull ResourceLocation sound, int subtitleFrequency) {
        super(sound, SoundCategory.PLAYERS);
        this.playerReference = new WeakReference<>(player);
        this.subtitleFrequency = subtitleFrequency;
        this.lastX = (float) player.posX;
        this.lastY = (float) player.posY;
        this.lastZ = (float) player.posZ;
        this.repeat = true;
        this.repeatDelay = 0;

        // N.B. the volume must be > 0 on first time it's processed by sound system or else it will not
        // get registered for tick events.
        this.volume = 0.1F;
    }

    @Nullable
    private EntityPlayer getPlayer() {
        return playerReference.get();
    }

    protected void setFade(float fadeUpStep, float fadeDownStep) {
        this.fadeUpStep = fadeUpStep;
        this.fadeDownStep = fadeDownStep;
    }

    @Override
    public float getXPosF() {
        //Gracefully handle the player becoming null if this object is kept around after update marks us as donePlaying
        EntityPlayer player = getPlayer();
        if (player != null) {
            this.lastX = (float) player.posX;
        }
        return this.lastX;
    }

    @Override
    public float getYPosF() {
        //Gracefully handle the player becoming null if this object is kept around after update marks us as donePlaying
        EntityPlayer player = getPlayer();
        if (player != null) {
            this.lastY = (float) player.posY;
        }
        return this.lastY;
    }

    @Override
    public float getZPosF() {
        //Gracefully handle the player becoming null if this object is kept around after update marks us as donePlaying
        EntityPlayer player = getPlayer();
        if (player != null) {
            this.lastZ = (float) player.posZ;
        }
        return this.lastZ;
    }

    @Override
    public void update() {
        EntityPlayer player = getPlayer();
        if (player == null || player.isDead) {
            this.donePlaying = true;
            this.volume = 0.0F;
            consecutiveTicks = 0;
            return;
        }

        if (shouldPlaySound(player)) {
            if (volume < 1.0F) {
                // If we weren't max volume, start fading up
                volume = Math.min(1.0F, volume + fadeUpStep);
            }
            if (consecutiveTicks % subtitleFrequency == 0) {
                SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
                for (ISoundEventListener soundEventListener : soundHandler.sndManager.listeners) {
                    SoundEventAccessor soundEventAccessor = createAccessor(soundHandler);
                    if (soundEventAccessor != null) {
                        soundEventListener.soundPlay(this, soundEventAccessor);
                    }
                }
                consecutiveTicks = 1;
            } else {
                consecutiveTicks++;
            }
        } else if (volume > 0.0F) {
            // Not yet fully muted, fade down
            volume = Math.max(0.0F, volume - fadeDownStep);
        }
    }

    @Override
    public boolean isDonePlaying() {
        return donePlaying;
    }

    public abstract boolean shouldPlaySound(@Nonnull EntityPlayer player);

    @Override
    public float getVolume() {
        return (float) (super.getVolume() * MekanismConfig.current().client.baseSoundVolume.val());
    }

    public enum SoundType {
        FLAMETHROWER,
        JETPACK,
        GAS_MASK,
        GRAVITATIONAL_MODULATOR
    }
}
