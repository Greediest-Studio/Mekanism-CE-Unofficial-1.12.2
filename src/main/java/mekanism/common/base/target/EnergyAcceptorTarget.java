package mekanism.common.base.target;

import mekanism.common.base.EnergyAcceptorWrapper;
import mekanism.common.base.SplitInfo;
import net.minecraft.util.EnumFacing;

public class EnergyAcceptorTarget extends Target<EnergyAcceptorWrapper, Double, Double> {

    @Override
    protected void acceptAmount(EnumFacing side, SplitInfo<Double> splitInfo, Double amount) {
        EnergyAcceptorWrapper wrapper = handlers.get(side);
        if (wrapper != null) {
            splitInfo.send(wrapper.acceptEnergy(side, amount, false));
        }
    }

    @Override
    protected Double simulate(EnergyAcceptorWrapper wrapper, EnumFacing side, Double energyToSend) {
        return wrapper.acceptEnergy(side, energyToSend, true);
    }
}