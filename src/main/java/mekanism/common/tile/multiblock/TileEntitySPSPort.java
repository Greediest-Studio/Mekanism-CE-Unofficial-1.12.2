package mekanism.common.tile.multiblock;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.TileNetworkList;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.base.IActiveState;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.CapabilityWrapperManager;
import mekanism.common.content.sps.SynchronizedSPSData;
import mekanism.common.integration.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.util.GasUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;

public class TileEntitySPSPort extends TileEntitySPSCasing implements IGasHandler, IConfigurable, IActiveState, IStrictEnergyStorage, IStrictEnergyAcceptor {

    private static final double MAX_PORT_ENERGY = SynchronizedSPSData.ENERGY_PER_INPUT * SynchronizedSPSData.INPUT_CAPACITY;

    private boolean outputMode;
    private double energy;
    private final CapabilityWrapperManager<TileEntitySPSPort, SPSPortForgeEnergyStorage> forgeEnergyManager = new CapabilityWrapperManager<>(TileEntitySPSPort.class, SPSPortForgeEnergyStorage.class);

    public TileEntitySPSPort() {
        super("SpsPort");
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        if (structure != null && outputMode && structure.outputTank.getGas() != null && structure.outputTank.getGas().amount > 0) {
            GasStack toSend = structure.outputTank.getGas().copy();
            toSend.amount = Math.min(toSend.amount, 16);
            int sent = GasUtils.emit(toSend, this, java.util.EnumSet.allOf(EnumFacing.class));
            if (sent > 0) {
                structure.outputTank.draw(sent, true);
            }
        }
        if (structure != null && energy > 0) {
            Coord4D portPos = Coord4D.get(this);
            if (structure.canSupplyPortEnergy(portPos)) {
                double toSupply = energy;
                structure.addEnergy(portPos, toSupply);
                setEnergy(energy - toSupply);
            }
        }
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double energy) {
        double clamped = Math.max(0, Math.min(energy, getMaxEnergy()));
        if (this.energy != clamped) {
            this.energy = clamped;
            markNoUpdateSync();
        }
    }

    @Override
    public double getMaxEnergy() {
        return MAX_PORT_ENERGY;
    }

    @Override
    public double acceptEnergy(EnumFacing side, double amount, boolean simulate) {
        if (amount <= 0 || (side != null && !canReceiveEnergy(side))) {
            return 0;
        }
        double accepted = Math.min(getMaxEnergy() - energy, amount);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            setEnergy(energy + accepted);
        }
        return accepted;
    }

    @Override
    public boolean canReceiveEnergy(EnumFacing side) {
        return true;
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.getGas() == null || !canReceiveGas(side, stack.getGas()) || structure == null) {
            return 0;
        }
        return structure.inputTank.receive(stack, doTransfer);
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (!canDrawGas(side, null) || structure == null) {
            return null;
        }
        return structure.outputTank.draw(amount, doTransfer);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return structure != null && !outputMode && type == MekanismFluids.Polonium && structure.inputTank.canReceive(type);
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return structure != null && outputMode && structure.outputTank.canDraw(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        if (structure == null) {
            return IGasHandler.NONE;
        }
        return new GasTankInfo[]{structure.inputTank, structure.outputTank};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIGURABLE_CAPABILITY
                || capability == Capabilities.ENERGY_STORAGE_CAPABILITY || capability == Capabilities.ENERGY_ACCEPTOR_CAPABILITY
                || capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIGURABLE_CAPABILITY
                || capability == Capabilities.ENERGY_STORAGE_CAPABILITY || capability == Capabilities.ENERGY_ACCEPTOR_CAPABILITY) {
            return (T) this;
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(forgeEnergyManager.getWrapper(this, side));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        outputMode = nbtTags.getBoolean("spsOutputMode");
        energy = nbtTags.getDouble("spsPortEnergy");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setBoolean("spsOutputMode", outputMode);
        nbtTags.setDouble("spsPortEnergy", energy);
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(outputMode);
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            boolean prevMode = outputMode;
            outputMode = dataStream.readBoolean();
            if (prevMode != outputMode) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side) {
        if (!isRemote()) {
            boolean oldMode = outputMode;
            setActive(!oldMode);
            String mode = outputMode ? LangUtils.localize("gui.output") : LangUtils.localize("gui.input");
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.GREY +
                  LangUtils.localize("tooltip.configurator.reactorPortEject") + " " + EnumColor.AQUA + mode));
            Mekanism.packetHandler.sendUpdatePacket(this);
            markNoUpdateSync();
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public EnumActionResult onRightClick(EntityPlayer player, EnumFacing side) {
        return EnumActionResult.PASS;
    }

    @Override
    public boolean getActive() {
        return outputMode;
    }

    @Override
    public void setActive(boolean active) {
        outputMode = active;
    }

    @Override
    public boolean renderUpdate() {
        return true;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    public static class SPSPortForgeEnergyStorage implements IEnergyStorage {

        private final TileEntitySPSPort tile;
        private final EnumFacing side;

        public SPSPortForgeEnergyStorage(TileEntitySPSPort tile, EnumFacing side) {
            this.tile = tile;
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return ForgeEnergyIntegration.toForge(tile.acceptEnergy(side, ForgeEnergyIntegration.fromForge(maxReceive), simulate));
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return ForgeEnergyIntegration.toForge(tile.getEnergy());
        }

        @Override
        public int getMaxEnergyStored() {
            return ForgeEnergyIntegration.toForge(tile.getMaxEnergy());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return tile.canReceiveEnergy(side);
        }
    }
}
