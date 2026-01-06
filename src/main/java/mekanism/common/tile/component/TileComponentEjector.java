package mekanism.common.tile.component;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.SideData;
import mekanism.common.base.ILogisticalTransporter;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ITankManager;
import mekanism.common.base.ITileComponent;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.transporter.TransitRequest;
import mekanism.common.content.transporter.TransitRequest.TransitResponse;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.util.*;

public class TileComponentEjector implements ITileComponent {


    private TileEntityContainerBlock tileEntity;

    private boolean strictInput;
    private EnumColor outputColor;
    private EnumColor[] inputColors = new EnumColor[]{null, null, null, null, null, null};
    private int tickDelay = 0;
    private Map<TransmissionType, SideData> sideData = new EnumMap<>(TransmissionType.class);
    private Map<TransmissionType, SideData> sideData2 = new EnumMap<>(TransmissionType.class);
    private Map<TransmissionType, SideData> sideData3 = new EnumMap<>(TransmissionType.class);

    private final EjectSpeedController fluid = new EjectSpeedController();
    private final EjectSpeedController fluid2 = new EjectSpeedController();
    private final EjectSpeedController gas = new EjectSpeedController();
    private final EjectSpeedController gas2 = new EjectSpeedController();

    public TileComponentEjector(TileEntityContainerBlock tile) {
        tileEntity = tile;
        tile.components.add(this);
    }

    public TileComponentEjector setOutputData(TransmissionType type, SideData data) {
        sideData.put(type, data);
        return this;
    }

    public TileComponentEjector setInputOutputData(TransmissionType type, SideData data) {
        sideData2.put(type, data);
        return this;
    }


    public TileComponentEjector setInputExtraOutputData(TransmissionType type, SideData data) {
        sideData3.put(type, data);
        return this;
    }

    public TileComponentEjector removeOutputData(TransmissionType type) {
        sideData.remove(type);
        return this;
    }

    public TileComponentEjector removeInputOutputData(TransmissionType type) {
        sideData2.remove(type);
        return this;
    }


    public TileComponentEjector removeInputExtraOutputData(TransmissionType type) {
        sideData3.remove(type);
        return this;
    }

    public void readFrom(TileComponentEjector ejector) {
        strictInput = ejector.strictInput;
        outputColor = ejector.outputColor;
        inputColors = ejector.inputColors;
        tickDelay = ejector.tickDelay;
        sideData = ejector.sideData;
        sideData2 = ejector.sideData2;
        sideData3 = ejector.sideData3;
    }

    @Override
    public void tick() {
        if (tileEntity.getWorld().isRemote) {
            return;
        }

        if (tickDelay == 0 || MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
            outputItems();
            outputItems2(sideData2.get(TransmissionType.ITEM));
            outputItems2(sideData3.get(TransmissionType.ITEM));
            if (!MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
                tickDelay = MekanismConfig.current().mekce.ItemEjectionDelay.val();
            }
        } else {
            tickDelay--;
        }

        eject(TransmissionType.GAS);
        eject2(TransmissionType.GAS, sideData2.get(TransmissionType.GAS));
        eject2(TransmissionType.GAS, sideData3.get(TransmissionType.GAS));
        eject(TransmissionType.FLUID);
        eject2(TransmissionType.FLUID, sideData2.get(TransmissionType.FLUID));
        eject2(TransmissionType.FLUID, sideData3.get(TransmissionType.FLUID));
    }

    /**
     * Eject something.
     *
     * @param type Type
     */
    private void eject(TransmissionType type) {
        SideData data = sideData.get(type);
        if (data == null || !getEjecting(type)) {
            return;
        }
        ITankManager tankManager = (ITankManager) this.tileEntity;
        Set<EnumFacing> outputSides = getOutputSides(type, data);
        if (outputSides.isEmpty()) {
            return;
        }
        if (tankManager.getTanks() == null) {
            return;
        }

        if (type == TransmissionType.GAS && tankManager.getTanks()[data.availableSlots[0]] instanceof GasTank gasTank) {
            this.gas.ensureSize(1, () -> Collections.singletonList(new TankProvider.Gas(gasTank)));
            ejectGas(outputSides, gasTank, this.gas, 0);
        } else if (type == TransmissionType.FLUID && tankManager.getTanks()[data.availableSlots[0]] instanceof FluidTank fluidTank) {
            this.fluid.ensureSize(1, () -> Collections.singletonList(new TankProvider.Fluid(fluidTank)));
            ejectFluid(outputSides, fluidTank, this.fluid, 0);
        }

    }

    private void eject2(TransmissionType type, SideData data) {

        if (data == null || !getEjecting(type)) {
            return;
        }
        ITankManager tankManager = (ITankManager) this.tileEntity;
        Set<EnumFacing> outputSides = getOutputSides(type, data);
        if (outputSides.isEmpty()) {
            return;
        }
        if (tankManager.getTanks() == null) {
            return;
        }

        for (int index = 0; index < data.availableSlots.length; index++) {
            if (data.allowExtractionSlot[index]) {
                if (type == TransmissionType.GAS && tankManager.getTanks()[data.availableSlots[index]] instanceof GasTank gasTank) {
                    this.gas2.ensureSize(1, () -> Collections.singletonList(new TankProvider.Gas(gasTank)));
                    ejectGas(outputSides, gasTank, this.gas2, 0);
                } else if (type == TransmissionType.FLUID && tankManager.getTanks()[data.availableSlots[index]] instanceof FluidTank fluidTank) {
                    this.fluid2.ensureSize(1, () -> Collections.singletonList(new TankProvider.Fluid(fluidTank)));
                    ejectFluid(outputSides, fluidTank, this.fluid2, 0);
                }
            }
        }
    }

    /**
     * Eject gas.
     */
    private void ejectGas(Set<EnumFacing> outputSides, GasTank tank, EjectSpeedController speedController, int tankIdx) {
        speedController.record(tankIdx);

        if (tank.getGas() == null || tank.getStored() <= 0 || tank.getGas().getGas() == null) {
            return;
        }

        if (!speedController.canEject(tankIdx)) {
            return;
        }

        GasStack toEmit = tank.getGas().copy().withAmount(Math.min(tank.getMaxGas(), tank.getStored()));
        int emitted = GasUtils.emit(toEmit, tileEntity, outputSides);
        speedController.eject(tankIdx, emitted);
        if (emitted <= 0) {
            return;
        }

        tank.draw(emitted, true);
    }

    /**
     * Eject fluid.
     */
    private void ejectFluid(Set<EnumFacing> outputSides, FluidTank tank, EjectSpeedController speedController, int tankIdx) {
        speedController.record(tankIdx);

        if (tank.getFluid() == null || tank.getFluidAmount() <= 0) {
            return;
        }

        if (!speedController.canEject(tankIdx)) {
            return;
        }

        FluidStack toEmit = PipeUtils.copy(tank.getFluid(), Math.min(tank.getCapacity(), tank.getFluidAmount()));
        int emitted = PipeUtils.emit(outputSides, toEmit, tileEntity);
        speedController.eject(tankIdx, emitted);
        if (emitted <= 0) {
            return;
        }

        tank.drain(emitted, true);
    }

    public Set<EnumFacing> getOutputSides(TransmissionType type, SideData data) {
        Set<EnumFacing> outputSides = EnumSet.noneOf(EnumFacing.class);
        TileComponentConfig config = ((ISideConfiguration) tileEntity).getConfig();
        SideConfig sideConfig = config.getConfig(type);
        List<SideData> outputs = config.getOutputs(type);
        EnumFacing[] facings = MekanismUtils.getBaseOrientations(tileEntity.facing);
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            EnumFacing side = facings[i];
            if (sideConfig.get(side) == outputs.indexOf(data)) {
                outputSides.add(EnumFacing.VALUES[i]);
            }
        }
        return outputSides;
    }

    private void outputItems() {
        SideData data = sideData.get(TransmissionType.ITEM);
        if (data == null || !getEjecting(TransmissionType.ITEM)) {
            return;
        }
        Set<EnumFacing> outputs = getOutputSides(TransmissionType.ITEM, data);
        if (outputs.isEmpty()) {
            return;
        }
        TransitRequest ejectMap = null;
        TileEntityContainerBlock self = tileEntity;
        for (EnumFacing side : outputs) {
            if (ejectMap == null) {
                ejectMap = getEjectItemMap(data);
                if (ejectMap.isEmpty()) {
                    break;
                }
            }
            TileEntity tile = MekanismUtils.getTileEntity(self.getWorld(), self.getPos().offset(side));
            if (tile == null) {
                //If the spot is not loaded just skip trying to eject to it
                continue;
            }
            ILogisticalTransporter capability = CapabilityUtils.getCapability(tile, Capabilities.LOGISTICAL_TRANSPORTER_CAPABILITY, side.getOpposite());
            TransitResponse response;
            if (capability == null) {
                response = InventoryUtils.putStackInInventory(tile, ejectMap, side, false);
            } else {
                response = TransporterUtils.insert(self, capability, ejectMap, outputColor, true, 0);
            }
            if (!response.isEmpty()) {
                response.getInvStack(self, side).use();
                //Set map to null so next loop recalculates the eject map so that all sides get a chance to be ejected to
                // assuming that there is still any left
                //TODO: Eventually make some way to just directly update the TransitRequest with remaining parts
                ejectMap = null;
            }
        }
    }

    private void outputItems2(SideData data) {
        if (data == null || !getEjecting(TransmissionType.ITEM)) {
            return;
        }
        Set<EnumFacing> outputs = getOutputSides(TransmissionType.ITEM, data);
        TransitRequest ejectMap = null;
        TileEntityContainerBlock self = tileEntity;
        for (EnumFacing side : outputs) {
            if (ejectMap == null) {
                ejectMap = getEjectItemMap2(data);
                if (ejectMap.isEmpty()) {
                    break;
                }
            }
            TileEntity tile = MekanismUtils.getTileEntity(self.getWorld(), self.getPos().offset(side));
            if (tile == null) {
                //If the spot is not loaded just skip trying to eject to it
                continue;
            }
            ILogisticalTransporter capability = CapabilityUtils.getCapability(tile, Capabilities.LOGISTICAL_TRANSPORTER_CAPABILITY, side.getOpposite());
            TransitResponse response;
            if (capability == null) {
                response = InventoryUtils.putStackInInventory(tile, ejectMap, side, false);
            } else {
                response = TransporterUtils.insert(self, capability, ejectMap, outputColor, true, 0);
            }
            if (!response.isEmpty()) {
                response.getInvStack(self, side).use();
                //Set map to null so next loop recalculates the eject map so that all sides get a chance to be ejected to
                // assuming that there is still any left
                //TODO: Eventually make some way to just directly update the TransitRequest with remaining parts
                ejectMap = null;
            }
        }
    }

    private TransitRequest getEjectItemMap(SideData data) {
        TransitRequest request = new TransitRequest();
        for (int index = 0; index < data.availableSlots.length; index++) {
            int slotID = data.availableSlots[index];
            ItemStack stack = tileEntity.getStackInSlot(slotID);
            if (!stack.isEmpty()) {
                request.addItem(stack, index);
            }
        }
        return request;
    }

    private TransitRequest getEjectItemMap2(SideData data) {
        TransitRequest request = new TransitRequest();
        for (int index = 0; index < data.availableSlots.length; index++) {
            int slotID = data.availableSlots[index];
            if (data.allowExtractionSlot[index]) {
                ItemStack stack = tileEntity.getStackInSlot(slotID);
                if (!stack.isEmpty()) {
                    request.addItem(stack, index);
                }
            }

        }
        return request;
    }

    public boolean hasStrictInput() {
        return strictInput;
    }

    public void setStrictInput(boolean strict) {
        strictInput = strict;
        MekanismUtils.saveChunk(tileEntity);
    }

    public EnumColor getOutputColor() {
        return outputColor;
    }

    public void setOutputColor(EnumColor color) {
        outputColor = color;
        MekanismUtils.saveChunk(tileEntity);
    }

    public void setInputColor(EnumFacing side, EnumColor color) {
        inputColors[side.ordinal()] = color;
        MekanismUtils.saveChunk(tileEntity);
    }

    public EnumColor getInputColor(EnumFacing side) {
        return inputColors[side.ordinal()];
    }

    @Override
    public void read(NBTTagCompound nbtTags) {
        strictInput = nbtTags.getBoolean("strictInput");
        if (nbtTags.hasKey("ejectColor")) {
            outputColor = readColor(nbtTags.getInteger("ejectColor"));
        }
        for (int i = 0; i < 6; i++) {
            if (nbtTags.hasKey("inputColors" + i)) {
                inputColors[i] = readColor(nbtTags.getInteger("inputColors" + i));
            }
        }
    }

    @Override
    public void read(ByteBuf dataStream) {
        strictInput = dataStream.readBoolean();
        outputColor = readColor(dataStream.readInt());
        for (int i = 0; i < 6; i++) {
            inputColors[i] = readColor(dataStream.readInt());
        }
    }

    @Override
    public void write(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("strictInput", strictInput);
        if (outputColor != null) {
            nbtTags.setInteger("ejectColor", getColorIndex(outputColor));
        }
        for (int i = 0; i < 6; i++) {
            nbtTags.setInteger("inputColors" + i, getColorIndex(inputColors[i]));
        }
    }

    @Override
    public void write(TileNetworkList data) {
        data.add(strictInput);
        data.add(getColorIndex(outputColor));
        for (int i = 0; i < 6; i++) {
            data.add(getColorIndex(inputColors[i]));
        }
    }

    private EnumColor readColor(int inputColor) {
        if (inputColor == -1) {
            return null;
        }
        return TransporterUtils.colors.get(inputColor);
    }

    private int getColorIndex(EnumColor color) {
        if (color == null) {
            return -1;
        }
        return TransporterUtils.colors.indexOf(color);
    }

    @Override
    public void invalidate() {
    }

    private boolean getEjecting(TransmissionType type) {
        if (tileEntity instanceof ISideConfiguration configuration) {
            return configuration.getConfig() != null && configuration.getConfig().isEjecting(type);
        } else {
            return false;
        }
    }
}
