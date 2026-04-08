package mekanism.common.tile.multiblock;

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
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.IFluidHandlerWrapper;
import mekanism.common.block.states.BlockStateBasic.BoilerValveModeProperty;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.boiler.*;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.util.*;
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

public class TileEntityBoilerValve extends TileEntityBoilerCasing implements IFluidHandlerWrapper, IComputerIntegration, IComparatorSupport, IGasHandler, IConfigurable {

    private static final String[] methods = new String[]{"isFormed", "getSteam", "getWater", "getBoilRate", "getMaxBoilRate", "getTemp"};
    public BoilerTank waterTank;
    public BoilerTank steamTank;
    public BoilerGasTank inputTank;
    public BoilerGasTank outputTank;
    private int currentRedstoneLevel;

    private PortMode mode = PortMode.INPUT;

    // Legacy field kept for compatibility with old saves and active-texture checks.
    public boolean Eject;

    public TileEntityBoilerValve() {
        super("BoilerValve");
        waterTank = new BoilerWaterTank(this);
        steamTank = new BoilerSteamTank(this);
        inputTank = new BoilerInputGasTank(this);
        outputTank = new BoilerOutputGasTank(this);
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        if (nbtTags.hasKey("boilerValveMode")) {
            mode = PortMode.byIndex(nbtTags.getInteger("boilerValveMode"));
        } else {
            //Old saves only had input/output eject state
            mode = nbtTags.getBoolean("Eject") ? PortMode.OUTPUT_STEAM : PortMode.INPUT;
        }
        updateEjectFlag();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("boilerValveMode", mode.ordinal());
        nbtTags.setBoolean("Eject", Eject);
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        if (structure != null) {
            if (mode == PortMode.OUTPUT_STEAM && structure.steamStored != null && structure.steamStored.amount > 0) {
                EmitUtils.forEachSide(getWorld(), getPos(), EnumSet.allOf(EnumFacing.class), (tile, side) -> {
                    if (!(tile instanceof TileEntityBoilerValve)) {
                        IFluidHandler handler = CapabilityUtils.getCapability(tile, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
                        if (handler != null && PipeUtils.canFill(handler, structure.steamStored)) {
                            structure.steamStored.amount -= handler.fill(structure.steamStored, true);
                            if (structure.steamStored.amount <= 0) {
                                structure.steamStored = null;
                            }
                        }
                    }
                });
            }
            if (mode == PortMode.OUTPUT_COOLANT && outputTank.getGas() != null && outputTank.getGas().getGas() != null) {
                GasStack toSend = outputTank.getGas().copy().withAmount(Math.min(outputTank.getMaxGas(), outputTank.getGasAmount()));
                outputTank.output(GasUtils.emit(toSend, this, EnumSet.allOf(EnumFacing.class)), true);
            }
            int newRedstoneLevel = getRedstoneLevel();
            if (newRedstoneLevel != currentRedstoneLevel) {
                updateComparatorOutputLevelSync();
                currentRedstoneLevel = newRedstoneLevel;
            }
        }
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if ((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) {
            if (mode == PortMode.INPUT) {
                return new FluidTankInfo[]{waterTank.getInfo()};
            } else if (mode == PortMode.OUTPUT_STEAM) {
                return new FluidTankInfo[]{steamTank.getInfo()};
            }
        }
        return PipeUtils.EMPTY;
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return getTankInfo(null);
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        if (mode != PortMode.INPUT || resource.getFluid() != FluidRegistry.WATER) {
            return 0;
        }
        return waterTank.fill(resource, doFill);
    }

    @Override
    @Nullable
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        if (mode != PortMode.OUTPUT_STEAM) {
            return null;
        }
        return steamTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        if (((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) && mode == PortMode.INPUT) {
            return fluid.getFluid() == FluidRegistry.WATER;
        }
        return false;
    }

    @Override
    public boolean canDrain(EnumFacing from, @Nullable FluidStack fluid) {
        if (((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) && mode == PortMode.OUTPUT_STEAM) {
            return FluidContainerUtils.canDrain(structure.steamStored, fluid);
        }
        return false;
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        if (method == 0) {
            return new Object[]{structure != null};
        } else {
            if (structure == null) {
                return new Object[]{"Unformed"};
            }
            switch (method) {
                case 1 -> {
                    return new Object[]{structure.steamStored != null ? structure.steamStored.amount : 0};
                }
                case 2 -> {
                    return new Object[]{structure.waterStored != null ? structure.waterStored.amount : 0};
                }
                case 3 -> {
                    return new Object[]{structure.lastBoilRate};
                }
                case 4 -> {
                    return new Object[]{structure.lastMaxBoil};
                }
                case 5 -> {
                    return new Object[]{structure.temperature};
                }
            }
        }
        throw new NoSuchMethodException();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if ((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) {
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIGURABLE_CAPABILITY) {
                return true;
            }
        }
        return super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if ((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) {
            if (capability == Capabilities.CONFIGURABLE_CAPABILITY || capability == Capabilities.GAS_HANDLER_CAPABILITY) {
                return (T) this;
            }
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(waterTank.getFluidAmount(), waterTank.getCapacity());
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.getGas() == null || !canReceiveGas(side, stack.getGas())) {
            return 0;
        }
        return inputTank.input(stack, doTransfer);
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (mode != PortMode.OUTPUT_COOLANT) {
            return null;
        }
        return outputTank.output(amount, doTransfer);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) && mode == PortMode.INPUT) {
            return type == MekanismFluids.SuperheatedSodium;
        }
        return false;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        if (((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) && mode == PortMode.OUTPUT_COOLANT) {
            return GasUtils.canDrain(structure.OutputGas, type);
        }
        return false;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        if ((!isRemote() && structure != null) || (isRemote() && clientHasStructure)) {
            if (mode == PortMode.INPUT) {
                return new GasTankInfo[]{inputTank.getInfo()};
            } else if (mode == PortMode.OUTPUT_COOLANT) {
                return new GasTankInfo[]{outputTank.getInfo()};
            }
        }
        return IGasHandler.NONE;
    }


    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            PortMode prevMode = mode;
            mode = PortMode.byIndex(dataStream.readInt());
            updateEjectFlag();
            if (prevMode != mode) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(mode.ordinal());
        return data;
    }

    @Override
    public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side) {
        if (!isRemote()) {
            mode = mode.next();
            updateEjectFlag();
            String modeText;
            switch (mode) {
                case INPUT:
                    modeText = LangUtils.localize("gui.input");
                    break;
                case OUTPUT_COOLANT:
                    modeText = LangUtils.localize("gui.output") + " " + LangUtils.localize("gui.coolant");
                    break;
                default:
                    modeText = LangUtils.localize("gui.output") + " " + LangUtils.localize("fluid.steam");
                    break;
            }
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
    protected boolean shouldDumpRadiation() {
        return true;
    }

    private void updateEjectFlag() {
        Eject = mode != PortMode.INPUT;
    }

    public PortMode getMode() {
        return mode;
    }

    public BoilerValveModeProperty getRenderMode() {
        return switch (mode) {
            case INPUT -> BoilerValveModeProperty.INPUT;
            case OUTPUT_STEAM -> BoilerValveModeProperty.OUTPUT_STEAM;
            case OUTPUT_COOLANT -> BoilerValveModeProperty.OUTPUT_COOLANT;
        };
    }

    public enum PortMode {
        INPUT,
        OUTPUT_STEAM,
        OUTPUT_COOLANT;

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
}
