package mekanism.common.integration.lookingat;

import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.common.MekanismLang;
import mekanism.common.base.FluidHandlerWrapper;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tile.TileEntityAdvancedBoundingBlock;
import mekanism.common.tile.base.TileEntitySynchronized;
import mekanism.common.util.CapabilityUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;

public class LookingAtUtils {

    private LookingAtUtils() {
    }


    private static void displayEnergy(LookingAtHelper info, IStrictEnergyStorage energyHandler) {
        info.addEnergyElement(energyHandler.getEnergy(), energyHandler.getMaxEnergy());
    }

    public static void addInfo(LookingAtHelper info, @Nonnull TileEntity tile, boolean displayTanks, boolean displayFluidTanks) {
        IStrictEnergyStorage energyCapability = CapabilityUtils.getCapability(tile, Capabilities.ENERGY_STORAGE_CAPABILITY, null);
        if (energyCapability != null && energyCapability.getMaxEnergy() > 0) {
            displayEnergy(info, energyCapability);
        } else if (tile instanceof TileEntityAdvancedBoundingBlock block && block.getInv() != null && block.getInv().getMaxEnergy() > 0) {
            displayEnergy(info, block.getInv());
        } else if (tile instanceof IStrictEnergyStorage strictEnergyStorage && strictEnergyStorage.getMaxEnergy() > 0) {
            displayEnergy(info, strictEnergyStorage);
        }
        if (displayTanks) {
            if (displayFluidTanks && tile instanceof TileEntitySynchronized) {
                IFluidHandler fluidCapability = CapabilityUtils.getCapability(tile, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                if (fluidCapability != null) {
                    displayFluid(info, fluidCapability);
                } else if (tile instanceof IFluidHandler handler) {
                    displayFluid(info, handler);
                }
            }
            IGasHandler gasCapability = CapabilityUtils.getCapability(tile, Capabilities.GAS_HANDLER_CAPABILITY, null);
            if (gasCapability != null) {
                displayGas(info, gasCapability);
            } else if (tile instanceof IGasHandler handler) {
                displayGas(info, handler);
            }
        }
    }


    private static void displayFluid(LookingAtHelper info, IFluidHandler fluidHandler) {
        if (fluidHandler instanceof FluidHandlerWrapper mekFluidHandler) {
            for (IFluidTankProperties fluidTank : mekFluidHandler.getTankProperties()) {
                addFluidInfo(info, fluidTank.getContents(), fluidTank.getCapacity());
            }
        }
    }

    private static void displayGas(LookingAtHelper info, IGasHandler handler) {
        for (GasTankInfo tank : handler.getTankInfo()) {
            addGasInfo(info, tank.getGas(), tank.getMaxGas());
        }
    }

    private static void addFluidInfo(LookingAtHelper info, FluidStack fluidInTank, int capacity) {
        if (fluidInTank != null) {
            info.addText(MekanismLang.LIQUID.getTranslationKey() + fluidInTank.getLocalizedName());
        }
        info.addFluidElement(fluidInTank, capacity);
    }

    private static void addGasInfo(LookingAtHelper info, GasStack gasInTank, int capacity) {
        if (gasInTank != null) {
            info.addText(MekanismLang.GAS.getTranslationKey() + gasInTank.getGas().getLocalizedName());
        }
        info.addChemicalElement(gasInTank, capacity);
    }


}
