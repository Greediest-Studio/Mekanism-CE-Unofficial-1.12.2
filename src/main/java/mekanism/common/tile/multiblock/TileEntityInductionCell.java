package mekanism.common.tile.multiblock;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tier.InductionCellTier;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;

public class TileEntityInductionCell extends TileEntityBasicBlock implements IStrictEnergyStorage {

    public InductionCellTier tier = InductionCellTier.BASIC;

    public double electricityStored;


    public String getName() {
        return LangUtils.localize(getBlockType().getTranslationKey() + ".InductionCell" + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            InductionCellTier prevTier = tier;
            tier = MekanismUtils.getByIndex(InductionCellTier.values(), dataStream.readInt(), tier);
            super.handlePacketData(dataStream);
            electricityStored = dataStream.readDouble();
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        data.add(tier.ordinal());
        super.getNetworkedData(data);
        data.add(electricityStored);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MekanismUtils.getByIndex(InductionCellTier.values(), nbtTags.getInteger("tier"), tier);
        electricityStored = nbtTags.getDouble("electricityStored");
    }


    @Override
   public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setDouble("electricityStored", electricityStored);
    }

    @Override
    public double getEnergy() {
        return electricityStored;
    }

    @Override
    public void setEnergy(double energy) {
        electricityStored = Math.min(energy, getMaxEnergy());
    }

    @Override
    public double getMaxEnergy() {
        return tier.getMaxEnergy();
    }

    @Override
    public boolean hasCapability(@Nonnull net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
        return capability == Capabilities.ENERGY_STORAGE_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        if (capability == Capabilities.ENERGY_STORAGE_CAPABILITY) {
            return Capabilities.ENERGY_STORAGE_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, facing);
    }
}
