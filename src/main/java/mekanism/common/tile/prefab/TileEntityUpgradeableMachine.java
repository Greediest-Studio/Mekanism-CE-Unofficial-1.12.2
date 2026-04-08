package mekanism.common.tile.prefab;

import mekanism.api.gas.GasTank;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.Upgrade;
import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.recipe.inputs.MachineInput;
import mekanism.common.recipe.machines.MachineRecipe;
import mekanism.common.recipe.outputs.MachineOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidTank;

/**
 * 可用升级的机器类型 ，一般用于工厂
 */

public abstract class TileEntityUpgradeableMachine<INPUT extends MachineInput<INPUT>, OUTPUT extends MachineOutput<OUTPUT>, RECIPE extends MachineRecipe<INPUT, OUTPUT, RECIPE>> extends
        TileEntityBasicMachine<INPUT, OUTPUT, RECIPE> implements ITierUpgradeable {


    /**
     * The foundation of all machines - a simple tile entity with a facing, active state, initialized state, sound effect, and animated texture.
     *
     * @param soundPath         - location of the sound effect
     * @param type              - the type of this machine
     * @param baseTicksRequired - how many ticks it takes to run a cycle
     */
    public TileEntityUpgradeableMachine(String soundPath, MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    public boolean isUpgrade = true;

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier != BaseTier.BASIC) {
            return false;
        }
        RecipeType type = RecipeType.getFromMachine(getBlockType(), getBlockMetadata());
        isUpgrade = false;
        world.setBlockToAir(getPos());
        world.setBlockState(getPos(), MekanismBlocks.MachineBlock.getStateFromMeta(5), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityFactory factory) {
            //Basic
            factory.facing = facing;
            factory.clientFacing = clientFacing;
            factory.ticker = ticker;
            factory.redstone = redstone;
            factory.redstoneLastTick = redstoneLastTick;
            factory.doAutoSync = doAutoSync;

            //Electric
            factory.electricityStored.set(electricityStored.get());

            //Machine
            factory.progress[0] = operatingTicks;
            factory.isActive = isActive;
            factory.setControlType(getControlType());
            factory.prevEnergy = prevEnergy;
            factory.upgradeComponent.readFrom(upgradeComponent);
            factory.upgradeComponent.setUpgradeSlot(0);
            factory.ejectorComponent.readFrom(ejectorComponent);

            factory.ejectorComponent.setOutputData(TransmissionType.ITEM, factory.configComponent.getOutputs(TransmissionType.ITEM).get(2));
            factory.ejectorComponent.setInputOutputData(TransmissionType.ITEM, factory.configComponent.getOutputs(TransmissionType.ITEM).get(6));
            factory.ejectorComponent.setInputExtraOutputData(TransmissionType.ITEM, factory.configComponent.getOutputs(TransmissionType.ITEM).get(11));

            factory.ejectorComponent.setOutputData(TransmissionType.GAS, factory.configComponent.getOutputs(TransmissionType.GAS).get(2));
            factory.ejectorComponent.setInputOutputData(TransmissionType.GAS, factory.configComponent.getOutputs(TransmissionType.GAS).get(3));

            factory.setRecipeType(type);
            factory.upgradeComponent.setSupported(Upgrade.GAS, type.fuelEnergyUpgrades());
            factory.securityComponent.readFrom(securityComponent);
            configComponent.getTransmissions().forEach(transmission -> {
                factory.configComponent.setConfig(transmission, configComponent.getConfig(transmission).asByteArray());
                factory.configComponent.setEjecting(transmission, configComponent.isEjecting(transmission));
            });

            upgradeInventory(factory);

            factory.upgradeComponent.getSupportedTypes().forEach(factory::recalculateUpgradables);

            factory.upgraded = true;
            factory.isUpgrade = true;
            factory.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(factory);
            return true;
        }

        return false;
    }

    protected abstract void upgradeInventory(TileEntityFactory factory);

    protected void setUpgradeSlot(TileEntityFactory factory, ItemStack stack) {
        factory.inventory.set(0, stack);
    }

    protected void setEnergySlotItem(TileEntityFactory factory, ItemStack stack) {
        factory.inventory.set(1, stack);
    }

    protected void setExtraSlotItem(TileEntityFactory factory, ItemStack stack) {
        factory.inventory.set(4, stack);
    }

    protected void setInputSlotItem(TileEntityFactory factory, ItemStack stack) {
        factory.inventory.set(TileEntityFactory.getSlotsWithTier(factory.tier)[0], stack);
    }

    protected void setOutputSlotItem(TileEntityFactory factory, ItemStack stack) {
        factory.inventory.set(TileEntityFactory.getOutputSlotsWithTier(factory.tier)[0], stack);
    }

    protected void setSecondaryOutputSlotItem(TileEntityFactory factory, ItemStack stack) {
        factory.inventory.set(TileEntityFactory.getSecondaryOutputSlotsWithTier(factory.tier)[0], stack);
    }

    protected void setInputGasTank(TileEntityFactory factory, GasTank gasTank) {
        factory.gasTank.setGas(gasTank.getGas());
    }

    protected void setInputFluidTank(TileEntityFactory factory, FluidTank fluidTank) {
        factory.fluidTank.setFluid(fluidTank.getFluid());
    }

    protected void setOutputGasTank(TileEntityFactory factory, GasTank gasTank) {
        factory.gasOutTank.setGas(gasTank.getGas());
    }

    @Override
    protected boolean shouldDumpRadiation() {
        return isUpgrade;
    }

}
