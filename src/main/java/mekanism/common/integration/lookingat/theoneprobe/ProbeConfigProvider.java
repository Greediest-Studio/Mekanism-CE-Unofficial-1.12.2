package mekanism.common.integration.lookingat.theoneprobe;

import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tile.TileEntityAdvancedBoundingBlock;
import mekanism.common.tile.base.TileEntitySynchronized;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ProbeConfigProvider implements IProbeConfigProvider {

    public static final ProbeConfigProvider INSTANCE = new ProbeConfigProvider();

    @Override
    public void getProbeConfig(IProbeConfig iProbeConfig, EntityPlayer entityPlayer, World world, Entity entity, IProbeHitEntityData iProbeHitEntityData) {

    }

    @Override
    public void getProbeConfig(IProbeConfig config, EntityPlayer entityPlayer, World world, IBlockState iBlockState, IProbeHitData data) {
        TileEntity tile = WorldUtils.getTileEntity(world, data.getPos());
        if (hasEnergyDisplay(tile)) {
            config.setRFMode(0);
        }
        if (tile instanceof TileEntitySynchronized) {
            //Disable the default fluid view for our own tiles
            config.setTankMode(0);
        }
    }

    private static boolean hasEnergyDisplay(TileEntity tile) {
        IStrictEnergyStorage energyCapability = CapabilityUtils.getCapability(tile, Capabilities.ENERGY_STORAGE_CAPABILITY, null);
        if (energyCapability != null && energyCapability.getMaxEnergy() > 0) {
            return true;
        }
        if (tile instanceof TileEntityAdvancedBoundingBlock block && block.getInv() != null && block.getInv().getMaxEnergy() > 0) {
            return true;
        }
        return tile instanceof IStrictEnergyStorage strictEnergyStorage && strictEnergyStorage.getMaxEnergy() > 0;
    }
}
