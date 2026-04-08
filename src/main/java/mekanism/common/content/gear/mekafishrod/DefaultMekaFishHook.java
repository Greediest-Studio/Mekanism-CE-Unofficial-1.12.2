package mekanism.common.content.gear.mekafishrod;

import mekanism.common.Mekanism;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultMekaFishHook implements IMekaFishHook {

    private boolean isfish;

    @Override
    public void setFish() {
        isfish = true;
    }


    @Override
    public boolean isMekaFishHook() {
        return isfish;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound ret = new NBTTagCompound();
        ret.setBoolean("isfish", isfish);
        return ret;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        isfish = nbt.getBoolean("isfish");
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(IMekaFishHook.class, new Capability.IStorage<>() {
            @Override
            public @Nullable NBTBase writeNBT(Capability<IMekaFishHook> capability, IMekaFishHook instance, EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(Capability<IMekaFishHook> capability, IMekaFishHook instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound tag) {
                    instance.deserializeNBT(tag);
                }
            }
        }, DefaultMekaFishHook::new);
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {

        public static final ResourceLocation NAME = Mekanism.rl("fish");
        private final IMekaFishHook defaultImpl = new DefaultMekaFishHook();


        @Override
        public boolean hasCapability(@NotNull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == Capabilities.MEKA_FISH_HOOK;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing side) {
            if (capability == Capabilities.MEKA_FISH_HOOK) {
                return Capabilities.MEKA_FISH_HOOK.cast(defaultImpl);
            }
            return null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return defaultImpl.serializeNBT();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            defaultImpl.deserializeNBT(nbt);
        }
    }
}
