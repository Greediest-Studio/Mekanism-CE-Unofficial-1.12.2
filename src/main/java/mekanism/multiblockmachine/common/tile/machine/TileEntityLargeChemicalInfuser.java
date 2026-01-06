package mekanism.multiblockmachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.Upgrade.IUpgradeInfoHandler;
import mekanism.common.base.IAdvancedBoundingBlock;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.ChemicalPairInput;
import mekanism.common.recipe.machines.ChemicalInfuserRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekanism.multiblockmachine.client.render.block.machine.bloom.BloomRenderLargeChemicalInfuser;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class TileEntityLargeChemicalInfuser extends TileEntityBasicMachine<ChemicalPairInput, GasOutput, ChemicalInfuserRecipe> implements IGasHandler, ISustainedData, IUpgradeInfoHandler,
        ITankManager, IAdvancedBoundingBlock {

    public static final int MAX_GAS = 8192000;
    public GasTank leftTank = new GasTank(MAX_GAS);
    public GasTank rightTank = new GasTank(MAX_GAS);
    public GasTank centerTank = new GasTank(MAX_GAS);
    private final EjectSpeedController gasSpeedController = new EjectSpeedController();
    public ChemicalInfuserRecipe cachedRecipe;

    public double clientEnergyUsed;
    private int currentRedstoneLevel;
    public int processes = MekanismConfig.current().multiblock.LargeChemicalInfuserProcesses.val();
    public int numPowering;
    public int updateDelay;
    public boolean needsPacket;

    public TileEntityLargeChemicalInfuser() {
        super("cheminfuser", "LargeChemicalInfuser", 0, MachineType.CHEMICAL_INFUSER.getUsage(), 4, 1);
        inventory = NonNullListSynchronized.withSize(5, ItemStack.EMPTY);
        upgradeComponent.setSupported(Upgrade.THREAD);
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (updateDelay > 0) {
            updateDelay--;
            if (updateDelay == 0) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        if (updateDelay > 0) {
            updateDelay--;
            if (updateDelay == 0) {
                needsPacket = true;
            }
        }
        ChargeUtils.discharge(3, this);
        TileUtils.receiveGasItem(inventory.get(0), leftTank);
        TileUtils.receiveGasItem(inventory.get(1), rightTank);
        TileUtils.drawGas(inventory.get(2), centerTank);
        ChemicalInfuserRecipe recipe = getRecipe();
        double energy = recipe != null ? energyPerTick * getUpgradedUsage(recipe) : energyPerTick;
        getProcess(recipe, true, energy, true, false);
        prevEnergy = getEnergy();
        if (needsPacket) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            needsPacket = false;
        }

    }

    @Override
    protected void setUpOtherActions() {
        double prev = getEnergy();
        if (getRecipe() != null) {
            setEnergy(getEnergy() - energyPerTick * getUpgradedUsage(getRecipe()));
        }
        clientEnergyUsed = prev - getEnergy();
    }

    public int getUpgradedUsage(ChemicalInfuserRecipe recipe) {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= processes;
        possibleProcess *= getThread();
        if (leftTank.getGasType() == recipe.recipeInput.leftGas.getGas()) {
            possibleProcess = Math.min(leftTank.getStored() / recipe.recipeInput.leftGas.amount, possibleProcess);
            possibleProcess = Math.min(rightTank.getStored() / recipe.recipeInput.rightGas.amount, possibleProcess);
        } else {
            possibleProcess = Math.min(leftTank.getStored() / recipe.recipeInput.rightGas.amount, possibleProcess);
            possibleProcess = Math.min(rightTank.getStored() / recipe.recipeInput.leftGas.amount, possibleProcess);
        }
        possibleProcess = Math.min(centerTank.getNeeded() / recipe.recipeOutput.output.amount, possibleProcess);
        possibleProcess = Math.min((int) (getEnergy() / energyPerTick), possibleProcess);
        //不能为0
        possibleProcess = Math.max(possibleProcess, 1);
        return possibleProcess;
    }


    public int getThread() {
        int thread = 1;
        if (upgradeComponent.isUpgradeInstalled(Upgrade.THREAD)) {
            thread += upgradeComponent.getUpgrades(Upgrade.THREAD);
        }
        return thread;
    }


    @Override
    public void addTileSyncTask() {
        this.gasSpeedController.ensureSize(1, () -> Collections.singletonList(new TankProvider.Gas(centerTank)));
        handleTank(centerTank, getTankside());
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            world.updateComparatorOutputLevel(pos, getBlockType());
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    private TileEntity getTankside() {
        BlockPos left = getPos().offset(facing).offset(MekanismUtils.getLeft(facing));
        BlockPos right = getPos().offset(facing).offset(MekanismUtils.getRight(facing));
        if (world.getTileEntity(left) != null) {
            return world.getTileEntity(left);
        } else if (world.getTileEntity(right) != null) {
            return world.getTileEntity(right);
        }
        return null;
    }

    private void handleTank(GasTank tank, TileEntity tile) {
        if (tile != null) {
            ejectGas(EnumSet.of(facing), tank, this.gasSpeedController, tile);
        }
    }

    private void ejectGas(Set<EnumFacing> outputSides, GasTank tank, EjectSpeedController speedController, TileEntity tile) {
        speedController.record(0);
        if (tank.getGas() == null || tank.getStored() <= 0 || tank.getGas().getGas() == null) {
            return;
        }
        if (!speedController.canEject(0)) {
            return;
        }
        GasStack toEmit = tank.getGas().copy().withAmount(Math.min(tank.getMaxGas(), tank.getStored()));
        int emitted = GasUtils.emit(toEmit, tile, outputSides);
        speedController.eject(0, emitted);
        if (emitted <= 0) {
            return;
        }
        tank.draw(emitted, true);
    }

    @Override
    public ChemicalPairInput getInput() {
        return new ChemicalPairInput(leftTank.getGas(), rightTank.getGas());
    }

    @Override
    public ChemicalInfuserRecipe getRecipe() {
        ChemicalPairInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getChemicalInfuserRecipe(getInput());
        }
        return cachedRecipe;
    }

    @Override
    public boolean canOperate(ChemicalInfuserRecipe recipe) {
        return recipe != null && recipe.canOperate(leftTank, rightTank, centerTank);
    }

    @Override
    public void operate(ChemicalInfuserRecipe recipe) {
        int operations = getUpgradedUsage(recipe);
        recipe.operate(leftTank, rightTank, centerTank, operations);
        markNoUpdateSync();
    }

    @Override
    public Map<ChemicalPairInput, ChemicalInfuserRecipe> getRecipes() {
        return RecipeHandler.Recipe.CHEMICAL_INFUSER.get();
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, leftTank);
            TileUtils.readTankData(dataStream, rightTank);
            TileUtils.readTankData(dataStream, centerTank);
            numPowering = dataStream.readInt();
            if (updateDelay == 0) {
                updateDelay = MekanismConfig.current().general.UPDATE_DELAY.val();
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(clientEnergyUsed);
        TileUtils.addTankData(data, leftTank);
        TileUtils.addTankData(data, rightTank);
        TileUtils.addTankData(data, centerTank);
        data.add(numPowering);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        leftTank.read(nbtTags.getCompoundTag("leftTank"));
        rightTank.read(nbtTags.getCompoundTag("rightTank"));
        centerTank.read(nbtTags.getCompoundTag("centerTank"));
        numPowering = nbtTags.getInteger("numPowering");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("leftTank", leftTank.write(new NBTTagCompound()));
        nbtTags.setTag("rightTank", rightTank.write(new NBTTagCompound()));
        nbtTags.setTag("centerTank", centerTank.write(new NBTTagCompound()));
        nbtTags.setInteger("numPowering", numPowering);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{leftTank, centerTank, rightTank};
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (side == MekanismUtils.getBack(facing) || side == MekanismUtils.getLeft(facing) || side == MekanismUtils.getRight(facing)) {
            return leftTank.canReceive(type) || rightTank.canReceive(type);
        }
        return false;
    }


    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack != null ? stack.getGas() : null)) {
            if (stack != null && (side == MekanismUtils.getBack(facing) || side == MekanismUtils.getLeft(facing) || side == MekanismUtils.getRight(facing))) {
                if (leftTank.canReceive(stack.getGas()) && rightTank.getGasType() != stack.getGas()) {
                    return leftTank.receive(stack, doTransfer);
                }
                if (rightTank.canReceive(stack.getGas()) && leftTank.getGasType() != stack.getGas()) {
                    return rightTank.receive(stack, doTransfer);
                }
            }
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return centerTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return centerTank.canDraw(type) && side == facing;
    }


    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        return slotID == 3 && ChargeUtils.canBeDischarged(itemstack);
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return InventoryUtils.EMPTY;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 0 || slotID == 2) {
            return !itemstack.isEmpty() && itemstack.getItem() instanceof IGasItem item && item.canReceiveGas(itemstack, null);
        } else if (slotID == 1) {
            return !itemstack.isEmpty() && itemstack.getItem() instanceof IGasItem item && item.canProvideGas(itemstack, null);
        } else if (slotID == 3) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return false;
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (leftTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "leftTank", leftTank.getGas().write(new NBTTagCompound()));
        }
        if (rightTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "rightTank", rightTank.getGas().write(new NBTTagCompound()));
        }
        if (centerTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "centerTank", centerTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        leftTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "leftTank")));
        rightTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "rightTank")));
        centerTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "centerTank")));
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{leftTank, rightTank, centerTank};
    }

    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }

    public double getTierEnergy() {
        return MachineType.CHEMICAL_INFUSER.getStorage() * processes * getThread();
    }

    @Override
    public String[] getMethods() {
        return new String[0];
    }

    @Override
    public Object[] invoke(int method, Object[] args) throws NoSuchMethodException {
        return new Object[0];
    }


    public double getScaledLeftTankGasLevel() {
        return Math.max(Math.min((double) leftTank.getStored() / leftTank.getMaxGas(), 1.0D), 0.0D);
    }

    public double getScaledRightTankGasLevel() {
        return Math.max(Math.min((double) rightTank.getStored() / rightTank.getMaxGas(), 1.0D), 0.0D);
    }

    public double getScaledGasTankLevel() {
        return Math.max(Math.min((double) centerTank.getStored() / centerTank.getMaxGas(), 1.0D), 0.0D);
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(centerTank.getStored(), centerTank.getMaxGas());
    }

    @Override
    public boolean isPowered() {
        return redstone || numPowering > 0;
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(3).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 1;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MekanismMultiblockMachine.proxy;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.LargeChemicalInfuser.name");
    }

    @Override
    public boolean canBoundReceiveEnergy(BlockPos coord, EnumFacing side) {
        EnumFacing back = MekanismUtils.getBack(facing);
        if (coord.equals(getPos().offset(back))) {
            return side == back;
        }
        return false;
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return side == MekanismUtils.getBack(this.facing);
    }

    @Override
    public boolean canBoundOutPutEnergy(BlockPos location, EnumFacing side) {
        return false;
    }

    @Override
    public void onPower() {
        numPowering++;
    }

    @Override
    public void onNoPower() {
        numPowering--;
    }

    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {

    }

    @Override
    public String getDataType() {
        return getName();
    }

    @Override
    public void onPlace() {
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos pos1 = getPos().add(x, y, z);
                    if (y == 0) {
                        MekanismUtils.makeAdvancedBoundingBlock(world, pos1, Coord4D.get(this));
                    } else {
                        MekanismUtils.makeBoundingBlock(world, pos1, Coord4D.get(this));
                    }
                    world.notifyNeighborsOfStateChange(pos1, getBlockType(), true);
                }
            }
        }
    }

    @Override
    public void onBreak() {
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlockToAir(getPos().add(x, y, z));
                }
            }
        }
    }

    @Override
    public boolean hasOffsetCapability(@NotNull Capability<?> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return false;
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return hasCapability(capability, side);
    }


    @Nullable
    @Override
    public <T> T getOffsetCapability(@NotNull Capability<T> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (isStrictEnergy(capability)) {
            return (T) this;
        } else if (isTesla(capability, side)) {
            return (T) getTeslaEnergyWrapper(side);
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(getForgeEnergyWrapper(side));
        }
        return getCapability(capability, side);
    }

    @Override
    public boolean isOffsetCapabilityDisabled(@NotNull Capability<?> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        EnumFacing back = facing.getOpposite();
        EnumFacing left = MekanismUtils.getLeft(facing);
        EnumFacing right = MekanismUtils.getRight(facing);
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.EAST) {
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != right && side != back;
                }
            } else if (facing == EnumFacing.SOUTH) {
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != right && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != left && side != back;
                }
            } else if (facing == EnumFacing.WEST) {
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != right && side != back;
                }
            } else if (facing == EnumFacing.NORTH) {
                if (offset.equals(new Vec3i(-1, 0, -1)) || offset.equals(new Vec3i(1, 0, -1))) {
                    return side != facing;
                }
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != right && side != back;
                }
            }
            return true;
        }

        if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            if (offset.equals(new Vec3i(back.getXOffset(), 0, back.getZOffset()))) {
                return side != back;
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return true;
        } else if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || isTesla(capability, side)) {
            return true;
        }
        return false;
    }

    @Override
    public void validate() {
        super.validate();
        if (isRemote()) {
            if (Mekanism.hooks.Bloom && MekanismConfig.current().client.enableBloom.val()) {
                new BloomRenderLargeChemicalInfuser(this);
            }
        }
    }


    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (updateDelay == 0) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            updateDelay = 10;
        }
    }
}
