package mekanism.common.tile.interfaces;

import mekanism.api.MekanismAPI;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.math.MathUtils;
import mekanism.common.Mekanism;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldNameable;

public interface ITileRadioactive {

    static float calculateRadiationScale(GasTankInfo[] tanks, TileEntity tile, BlockPos pos) {
        if (MekanismAPI.getRadiationManager().isRadiationEnabled() && tile instanceof IGasHandler && tanks != IGasHandler.NONE) {
            float summedScale = 0;
            //不知道为什么还是能抛出null，推测应该是工厂
            try {
                if (tanks != null) {
                    for (GasTankInfo tank : tanks) {
                        if (tank != null && tank.getGas() != null && tank.getGas().getGas() != null && tank.getGas().getGas().isRadiation()) {
                            //TODO: Eventually we may want to debate doing this based on the radioactivity
                            // but for now this will work well
                            summedScale += tank.getStored() / (float) tank.getMaxGas();
                        }
                    }
                    return summedScale / tanks.length;
                }
            } catch (Exception e) {
                if (tile instanceof IWorldNameable worldNameable && worldNameable.getName() != null && !worldNameable.getName().isEmpty()) {
                    Mekanism.logger.error("Cannot add radiation from the machine,Machine Name :{}, Machine position : x={}, y={}, z={}", worldNameable.getName(), pos.getX(), pos.getY(), pos.getZ());
                } else {
                    Mekanism.logger.error("Cannot add radiation from the machine,Machine position :x={}, y={}, z={}", pos.getX(), pos.getY(), pos.getZ());
                }

            }
        }
        return 0;
    }

    float getRadiationScale();

    default int getRadiationParticleCount() {
        return MathUtils.clampToInt(10 * getRadiationScale());
    }
}
