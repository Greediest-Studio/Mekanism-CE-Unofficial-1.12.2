package mekanism.multiblockmachine.common.tile.machine;

import ic2.api.energy.tile.IEnergyEmitter;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.IConfigCardAccess;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.util.*;
import mekanism.multiblockmachine.client.render.block.machine.bloom.BloomRenderLargeSolarNeutronActivator;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class TileEntityLargeSolarNeutronActivator extends TileEntityContainerBlock implements IUpgradeTile, IRedstoneControl, ISecurityTile, IElectricMachine<GasInput, GasOutput, SolarNeutronRecipe>, IComputerIntegration, IConfigCardAccess,
        IMachineSlotTip, IAdvancedBoundingBlock, IGasHandler, ISustainedData, ITankManager, Upgrade.IUpgradeInfoHandler, IComparatorSupport, IActiveState, ISpecialSelectionWireframeTile {

    public static final int MAX_GAS = 8192000;
    public GasTank inputTank = new GasTank(MAX_GAS);
    public GasTank outputTank = new GasTank(MAX_GAS);
    private SolarNeutronRecipe cachedRecipe;
    private int currentRedstoneLevel;
    private boolean isActive;
    private long lastActive = -1;
    private boolean needsRainCheck;

    public int operatingTicks;

    public int BASE_TICKS_REQUIRED;

    public int ticksRequired;
    private final int RECENT_THRESHOLD = 100;
    public TileComponentUpgrade upgradeComponent;
    public TileComponentSecurity securityComponent = new TileComponentSecurity(this);
    private RedstoneControl controlType = RedstoneControl.DISABLED;
    public int processes = MekanismConfig.current().multiblock.LargeSolarNeutronProcesses.val();
    public int numPowering;
    private final EjectSpeedController gasSpeedController = new EjectSpeedController();

    public TileEntityLargeSolarNeutronActivator() {
        this(1);
    }

    public TileEntityLargeSolarNeutronActivator(int baseTicksRequired) {
        super("LargeSolarNeutronActivator");
        ticksRequired = BASE_TICKS_REQUIRED = baseTicksRequired;
        upgradeComponent = new TileComponentUpgrade(this, 2);
        upgradeComponent.setSupported(Upgrade.ENERGY, false);
        upgradeComponent.setSupported(Upgrade.THREAD);
        inventory = NonNullListSynchronized.withSize(3, ItemStack.EMPTY);
    }

    @Override
    public void validate() {
        super.validate();
        // Cache the flag to know if rain matters where this block is placed
        needsRainCheck = world.provider.getBiomeForCoords(getPos()).canRain();
        if (isRemote()) {
            if (Mekanism.hooks.Bloom && MekanismConfig.current().client.enableBloom.val()) {
                new BloomRenderLargeSolarNeutronActivator(this);
            }
        }
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (!isActive && lastActive > 0) {
            long updateDiff = world.getTotalWorldTime() - lastActive;
            if (updateDiff > RECENT_THRESHOLD) {
                MekanismUtils.updateBlock(world, getPos());
                lastActive = -1;
            }
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        Mekanism.EXECUTE_MANAGER.addSyncTask(this::addTileSyncTask);
        ItemStack stack = inventory.get(0);
        if (!stack.isEmpty() && stack.getItem() instanceof IGasItem item && item.getGas(stack) != null && RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.containsRecipe(item.getGas(stack).getGas())) {
            TileUtils.receiveGasItem(inventory.get(0), inputTank);
        }
        TileUtils.drawGas(inventory.get(1), outputTank);
        SolarNeutronRecipe recipe = getRecipe();

        // TODO: Ideally the neutron activator should use the sky brightness to determine throughput; but
        // changing this would dramatically affect a lot of setups with Fusion reactors which can take
        // a long time to relight. I don't want to be chased by a mob right now, so just doing basic
        // rain checks.
        boolean seesSun = world.isDaytime() && world.canSeeSky(getPos().up(2)) && !world.provider.isNether();
        if (needsRainCheck) {
            seesSun &= !(world.isRaining() || world.isThundering());
        }

        if (seesSun && canOperate(recipe) && MekanismUtils.canFunction(this)) {
            setActive(true);
            MultipleActions(recipe);
        } else {
            setActive(false);
        }

        // Every 20 ticks (once a second), send update to client. Note that this is a 50% reduction in network
        // traffic from previous implementation that send the update every 10 ticks.
        if (world.getTotalWorldTime() % 20 == 0) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }

        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    public int getUpgradedUsage(SolarNeutronRecipe recipe) {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= processes;
        possibleProcess *= getThread();
        possibleProcess = Math.min(Math.min(inputTank.getStored(), outputTank.getNeeded()), possibleProcess);
        return Math.min(inputTank.getStored() / recipe.recipeInput.ingredient.amount, possibleProcess);
    }

    public int getThread() {
        int thread = 1;
        if (upgradeComponent.isUpgradeInstalled(Upgrade.THREAD)) {
            thread += upgradeComponent.getUpgrades(Upgrade.THREAD);
        }
        return thread;
    }


    public void addTileSyncTask() {
        this.gasSpeedController.ensureSize(2, () -> Arrays.asList(new TankProvider.Gas(outputTank), new TankProvider.Gas(outputTank)));
        handleTank(outputTank, getLeftTankside(), MekanismUtils.getLeft(facing), 0);
        handleTank(outputTank, getRightTankside(), MekanismUtils.getRight(facing), 1);
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            world.updateComparatorOutputLevel(pos, getBlockType());
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    private TileEntity getLeftTankside() {
        BlockPos left = getPos().offset(MekanismUtils.getBack(facing)).offset(MekanismUtils.getLeft(facing));
        if (world.getTileEntity(left) != null) {
            return world.getTileEntity(left);
        }
        return null;
    }

    private TileEntity getRightTankside() {
        BlockPos right = getPos().offset(MekanismUtils.getBack(facing)).offset(MekanismUtils.getRight(facing));
        if (world.getTileEntity(right) != null) {
            return world.getTileEntity(right);
        }
        return null;
    }


    private void handleTank(GasTank tank, TileEntity tile, EnumFacing side, int tankIdx) {
        if (tile != null) {
            ejectGas(Collections.singleton(side), tank, this.gasSpeedController, tankIdx, tile);
        }
    }

    private void ejectGas(Set<EnumFacing> outputSides, GasTank tank, EjectSpeedController speedController, int tankIdx, TileEntity tile) {
        speedController.record(tankIdx);
        if (tank.getGas() == null || tank.getStored() <= 0 || tank.getGas().getGas() == null) {
            return;
        }
        if (!speedController.canEject(tankIdx)) {
            return;
        }
        GasStack toEmit = tank.getGas().copy().withAmount(Math.min(tank.getMaxGas(), tank.getStored()));
        int emitted = GasUtils.emit(toEmit, tile, outputSides);
        speedController.eject(tankIdx, emitted);
        if (emitted <= 0) {
            return;
        }
        tank.draw(emitted, true);
    }

    public SolarNeutronRecipe getRecipe() {
        GasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getSolarNeutronRecipe(getInput());
        }
        return cachedRecipe;
    }

    public GasInput getInput() {
        return new GasInput(inputTank.getGas());
    }

    public boolean canOperate(SolarNeutronRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, outputTank);
    }

    public void operate(SolarNeutronRecipe recipe) {
        recipe.operate(inputTank, outputTank, getUpgradedUsage(recipe));
    }

    @Override
    public Map<GasInput, SolarNeutronRecipe> getRecipes() {
        return RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.get();
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            boolean newActive = dataStream.readBoolean();
            boolean stateChange = newActive != isActive;
            isActive = newActive;
            if (stateChange && !isActive) {
                // Switched off; note the time
                lastActive = world.getTotalWorldTime();
            } else if (stateChange && isActive) {
                // Switching on; if lastActive is not currently set, trigger a lighting update
                // and make sure lastActive is clear
                if (lastActive == -1) {
                    MekanismUtils.updateBlock(world, getPos());
                }
                lastActive = -1;
            }
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), RedstoneControl.DISABLED);
            operatingTicks = dataStream.readInt();
            ticksRequired = dataStream.readInt();
            TileUtils.readTankData(dataStream, inputTank);
            TileUtils.readTankData(dataStream, outputTank);
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(isActive);
        data.add(controlType.ordinal());
        data.add(operatingTicks);
        data.add(ticksRequired);
        TileUtils.addTankData(data, inputTank);
        TileUtils.addTankData(data, outputTank);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        isActive = nbtTags.getBoolean("isActive");
        controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), RedstoneControl.DISABLED);
        operatingTicks = nbtTags.getInteger("operatingTicks");
        inputTank.read(nbtTags.getCompoundTag("inputTank"));
        outputTank.read(nbtTags.getCompoundTag("outputTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setBoolean("isActive", isActive);
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setInteger("operatingTicks", operatingTicks);
        nbtTags.setTag("inputTank", inputTank.write(new NBTTagCompound()));
        nbtTags.setTag("outputTank", outputTank.write(new NBTTagCompound()));
    }

    @Override
    public void onPlace() {
        for (int y = 0; y <= 2; y++) {
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
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlockToAir(getPos().add(x, y, z));
                }
            }
        }
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.getGas() == null) {
            return 0;
        }
        if (canReceiveGas(side, stack.getGas())) {
            int recipeAmount = RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.get().get(new GasInput(stack)).recipeInput.ingredient.amount;
            int receivable = inputTank.receive(stack, false);
            int stored = inputTank.stored != null ? inputTank.stored.amount : 0;
            int newStored = stored + receivable;
            int amount = newStored - stored - newStored % recipeAmount;
            return inputTank.receive(stack.copy().withAmount(amount), doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return outputTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (side == MekanismUtils.getBack(facing)) {
            return inputTank.canReceive(type) && RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.containsRecipe(type);
        }
        return false;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        if (side == MekanismUtils.getLeft(facing) || side == MekanismUtils.getRight(facing)) {
            return outputTank.canDraw(type);
        }
        return false;
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
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        return capability == Capabilities.GAS_HANDLER_CAPABILITY;
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (inputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "inputTank", inputTank.getGas().write(new NBTTagCompound()));
        }
        if (outputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "outputTank", outputTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        inputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank")));
        outputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank")));
    }

    @Override
    public boolean canSetFacing(@Nonnull EnumFacing facing) {
        return facing != EnumFacing.DOWN && facing != EnumFacing.UP;
    }

    @Override
    public boolean renderUpdate() {
        return false;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{inputTank, outputTank};
    }


    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return stack.getItem() instanceof IGasItem;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return InventoryUtils.EMPTY;
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(inputTank.getStored(), inputTank.getMaxGas());
    }

    @Override
    public String[] getMethods() {
        return new String[0];
    }

    @Override
    public Object[] invoke(int method, Object[] args) throws NoSuchMethodException {
        return new Object[0];
    }

    @Override
    public boolean getEnergySlot() {
        return false;
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
    }

    public void MultipleActions(SolarNeutronRecipe recipe) {
        MultipleActions(recipe, ticksRequired);
    }

    @Override
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(RedstoneControl type) {
        controlType = Objects.requireNonNull(type);
        MekanismUtils.saveChunk(this);
    }

    @Override
    public boolean canPulse() {
        return false;
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }

    @Override
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }

    @Override
    public boolean getActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        boolean stateChange = isActive != active;
        if (stateChange) {
            isActive = active;
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }

    @Override
    public boolean wasActiveRecently() {
        // If the machine is currently active or it flipped off within our threshold,
        // we'll consider it recently active.
        return isActive || (lastActive > 0 && (world.getTotalWorldTime() - lastActive) < RECENT_THRESHOLD);
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 5;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MekanismMultiblockMachine.proxy;
    }

    @Override
    public boolean canBoundReceiveEnergy(BlockPos location, EnumFacing side) {
        return false;
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
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int extractEnergy(EnumFacing enumFacing, int i, boolean b) {
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int receiveEnergy(EnumFacing enumFacing, int i, boolean b) {
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int getEnergyStored(EnumFacing enumFacing) {
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int getMaxEnergyStored(EnumFacing enumFacing) {
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public boolean canConnectEnergy(EnumFacing enumFacing) {
        return false;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.IC2_MOD_ID)
    public double getDemandedEnergy() {
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.IC2_MOD_ID)
    public int getSinkTier() {
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.IC2_MOD_ID)
    public double injectEnergy(EnumFacing enumFacing, double v, double v1) {
        return 0;
    }

    @Optional.Method(modid = MekanismHooks.IC2_MOD_ID)
    public boolean acceptsEnergyFrom(IEnergyEmitter iEnergyEmitter, EnumFacing enumFacing) {
        return false;
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
    public double acceptEnergy(EnumFacing side, double amount, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canReceiveEnergy(EnumFacing side) {
        return false;
    }

    @Override
    public double pullEnergy(EnumFacing side, double amount, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canOutputEnergy(EnumFacing side) {
        return false;
    }

    @Override
    public double getEnergy() {
        return 0;
    }

    @Override
    public void setEnergy(double energy) {

    }

    @Override
    public double getMaxEnergy() {
        return 0;
    }

    @Override
    public boolean hasOffsetCapability(@NotNull Capability<?> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return false;
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return true;
        }
        return hasCapability(capability, side);
    }

    @Override
    public @Nullable <T> T getOffsetCapability(@NotNull Capability<T> capability, @Nullable EnumFacing side, @NotNull Vec3i offset) {
        if (isOffsetCapabilityDisabled(capability, side, offset)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
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
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != right && side != back;
                }
            } else if (facing == EnumFacing.SOUTH) {
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != right && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, -1))) {
                    return side != left && side != back;
                }
            } else if (facing == EnumFacing.WEST) {
                if (offset.equals(new Vec3i(1, 0, -1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != right && side != back;
                }
            } else if (facing == EnumFacing.NORTH) {
                if (offset.equals(new Vec3i(1, 0, 1))) {
                    return side != left && side != back;
                }
                if (offset.equals(new Vec3i(-1, 0, 1))) {
                    return side != right && side != back;
                }
            }
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.LargeSolarNeutronActivator.name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekanism.multiblockmachine.client.model.machine.ModelLargeSolarNeutronActivator.class;
    }

    @Override
    protected boolean shouldDumpRadiation() {
        return true;
    }
}
