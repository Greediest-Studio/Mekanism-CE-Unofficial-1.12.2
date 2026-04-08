package mekanism.generators.common.tile.fission;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.base.FluidHandlerWrapper;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IFluidHandlerWrapper;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.*;
import mekanism.generators.common.block.states.BlockStateGenerator.FissionPortModeProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;

public class TileEntityFissionReactorPort extends TileEntityFissionReactorCasing implements IGasHandler, IFluidHandlerWrapper, IConfigurable, IActiveState {

    private PortMode mode = PortMode.INPUT;

    public TileEntityFissionReactorPort() {
        super("FissionReactorPort");
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        if (structure == null || mode == PortMode.INPUT) {
            return;
        }

        if (mode == PortMode.OUTPUT_WASTE && structure.wasteTank.getGas() != null && structure.wasteTank.getGas().amount > 0) {
            GasStack toSend = structure.wasteTank.getGas().copy();
            int sent = GasUtils.emit(toSend, this, EnumSet.allOf(EnumFacing.class));
            if (sent > 0) {
                structure.wasteTank.draw(sent, true);
            }
        }

        if (mode == PortMode.OUTPUT_COOLANT && structure.steamTank.getFluidAmount() > 0) {
            EmitUtils.forEachSide(getWorld(), getPos(), EnumSet.allOf(EnumFacing.class), (tile, side) -> {
                if (tile instanceof TileEntityFissionReactorPort) {
                    return;
                }
                IFluidHandler handler = CapabilityUtils.getCapability(tile, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
                FluidStack steam = structure.steamTank.getFluid();
                if (handler != null && steam != null && PipeUtils.canFill(handler, steam)) {
                    int filled = handler.fill(steam, true);
                    if (filled > 0) {
                        structure.steamTank.drain(filled, true);
                    }
                }
            });
        }
        if (mode == PortMode.OUTPUT_COOLANT && structure.heatedCoolantTank.getGas() != null && structure.heatedCoolantTank.getGas().amount > 0) {
            GasStack toSend = structure.heatedCoolantTank.getGas().copy();
            int sent = GasUtils.emit(toSend, this, EnumSet.allOf(EnumFacing.class));
            if (sent > 0) {
                structure.heatedCoolantTank.draw(sent, true);
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        mode = PortMode.byIndex(nbtTags.getInteger("fissionPortMode"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("fissionPortMode", mode.ordinal());
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(mode.ordinal());
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            PortMode previousMode = mode;
            mode = PortMode.byIndex(dataStream.readInt());
            if (previousMode != mode) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.getGas() == null || !canReceiveGas(side, stack.getGas()) || structure == null) {
            return 0;
        }
        if (stack.getGas() == MekanismFluids.FissileFuel) {
            return structure.fuelTank.receive(stack, doTransfer);
        } else if (stack.getGas() == MekanismFluids.Sodium) {
            return structure.gasCoolantTank.receive(stack, doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (structure == null || !canDrawGas(side, null)) {
            return null;
        }
        if (mode == PortMode.OUTPUT_WASTE) {
            return structure.wasteTank.draw(amount, doTransfer);
        }
        return structure.heatedCoolantTank.draw(amount, doTransfer);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (structure == null || mode != PortMode.INPUT) {
            return false;
        }
        if (type == null) {
            return structure.fuelTank.getNeeded() > 0 || (structure.coolantTank.getFluidAmount() == 0 && structure.gasCoolantTank.getNeeded() > 0);
        }
        if (type == MekanismFluids.FissileFuel) {
            return structure.fuelTank.canReceive(type);
        } else if (type == MekanismFluids.Sodium) {
            return structure.coolantTank.getFluidAmount() == 0 && structure.gasCoolantTank.canReceive(type);
        }
        return false;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        if (structure == null) {
            return false;
        }
        if (mode == PortMode.OUTPUT_WASTE) {
            return structure.wasteTank.canDraw(type);
        } else if (mode == PortMode.OUTPUT_COOLANT) {
            return structure.heatedCoolantTank.canDraw(type);
        }
        return false;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        if (structure == null) {
            return IGasHandler.NONE;
        }
        return switch (mode) {
            case INPUT -> new GasTankInfo[]{structure.fuelTank, structure.gasCoolantTank};
            case OUTPUT_COOLANT -> new GasTankInfo[]{structure.heatedCoolantTank};
            case OUTPUT_WASTE -> new GasTankInfo[]{structure.wasteTank};
        };
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        if (structure == null || mode != PortMode.INPUT) {
            return 0;
        }
        if (resource.getFluid() == FluidRegistry.WATER) {
            if (structure.gasCoolantTank.getStored() > 0) {
                return 0;
            }
            return structure.coolantTank.fill(resource, doFill);
        }
        if (resource.getFluid() == FluidRegistry.getFluid("liquidsodium")) {
            if (structure.coolantTank.getFluidAmount() > 0) {
                return 0;
            }
            return structure.gasCoolantTank.receive(new GasStack(MekanismFluids.Sodium, resource.amount), doFill);
        }
        return 0;
    }

    @Nullable
    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        if (structure == null || mode != PortMode.OUTPUT_COOLANT) {
            return null;
        }
        return structure.steamTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        if (structure == null || mode != PortMode.INPUT) {
            return false;
        }
        if (fluid.getFluid() == FluidRegistry.WATER) {
            return structure.gasCoolantTank.getStored() == 0;
        }
        if (fluid.getFluid() == FluidRegistry.getFluid("liquidsodium")) {
            return structure.coolantTank.getFluidAmount() == 0;
        }
        return false;
    }

    @Override
    public boolean canDrain(EnumFacing from, @Nullable FluidStack fluid) {
        return structure != null && mode == PortMode.OUTPUT_COOLANT && (fluid == null || FluidContainerUtils.canDrain(structure.steamTank.getFluid(), fluid));
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if (structure == null) {
            return PipeUtils.EMPTY;
        }
        return switch (mode) {
            case INPUT -> new FluidTankInfo[]{structure.coolantTank.getInfo()};
            case OUTPUT_COOLANT -> new FluidTankInfo[]{structure.steamTank.getInfo()};
            case OUTPUT_WASTE -> PipeUtils.EMPTY;
        };
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return getTankInfo(null);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIGURABLE_CAPABILITY) {
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && (structure != null || clientHasStructure)) {
            return true;
        }
        return super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIGURABLE_CAPABILITY) {
            return (T) this;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && (structure != null || clientHasStructure)) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side) {
        if (!isRemote()) {
            mode = mode.next();
            String modeText = switch (mode) {
                case INPUT -> LangUtils.localize("gui.input");
                case OUTPUT_COOLANT -> LangUtils.localize("fission.port.mode.output_coolant");
                case OUTPUT_WASTE -> LangUtils.localize("fission.port.mode.output_waste");
            };
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.GREY +
                  LangUtils.localize("tooltip.configurator.reactorPortEject") + " " + EnumColor.AQUA + modeText));
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
        return mode != PortMode.INPUT;
    }

    @Override
    public void setActive(boolean active) {
        mode = active ? PortMode.OUTPUT_WASTE : PortMode.INPUT;
    }

    @Override
    public boolean renderUpdate() {
        return true;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    public enum PortMode {
        INPUT,
        OUTPUT_COOLANT,
        OUTPUT_WASTE;

        private static final PortMode[] MODES = values();

        public PortMode next() {
            return MODES[(ordinal() + 1) % MODES.length];
        }

        public static PortMode byIndex(int index) {
            if (index < 0 || index >= MODES.length) {
                return INPUT;
            }
            return MODES[index];
        }
    }

    public FissionPortModeProperty getRenderMode() {
        return switch (mode) {
            case INPUT -> FissionPortModeProperty.INPUT;
            case OUTPUT_COOLANT -> FissionPortModeProperty.OUTPUT_COOLANT;
            case OUTPUT_WASTE -> FissionPortModeProperty.OUTPUT_WASTE;
        };
    }

    public PortMode getMode() {
        return mode;
    }
}
