package mekanism.common.content.gear.mekafishrod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

public interface IMekaFishHook extends INBTSerializable<NBTTagCompound> {

    void setFish();

    boolean isMekaFishHook();
}
