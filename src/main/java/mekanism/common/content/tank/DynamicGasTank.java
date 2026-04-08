package mekanism.common.content.tank;

import mekanism.api.Coord4D;
import mekanism.api.gas.GasStack;
import mekanism.common.base.MultiblockGasTank;
import mekanism.common.content.tank.SynchronizedTankData.ValveData;
import mekanism.common.tile.multiblock.TileEntityDynamicTank;

import javax.annotation.Nullable;

public class DynamicGasTank extends MultiblockGasTank<TileEntityDynamicTank> {

    public DynamicGasTank(TileEntityDynamicTank tileEntity) {
        super(tileEntity);
    }

    @Override
    @Nullable
    public GasStack getGas() {
        return multiblock.structure != null ? multiblock.structure.gasstored : null;
    }

    @Override
    public int input(@Nullable GasStack resource, boolean doFill) {
        if (multiblock.structure != null && multiblock.structure.hasFluid()) {
            return 0;
        }
        return super.input(resource, doFill);
    }

    @Override
    public void setGas(GasStack stack) {
        if (multiblock.structure != null) {
            multiblock.structure.gasstored = stack;
        }
    }

    @Override
    public int getMaxGas() {
        return multiblock.structure != null ? multiblock.structure.volume * TankUpdateProtocol.FLUID_PER_TANK : 0;
    }

    @Override
    protected void updateValveData() {
        if (multiblock.structure != null) {
            Coord4D coord4D = Coord4D.get(multiblock);
            for (ValveData data : multiblock.structure.valves) {
                if (coord4D.equals(data.location)) {
                    data.onTransfer();
                }
            }
        }
    }
}
