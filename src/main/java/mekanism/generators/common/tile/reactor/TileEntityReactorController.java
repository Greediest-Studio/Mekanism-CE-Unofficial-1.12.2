package mekanism.generators.common.tile.reactor;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.base.IActiveState;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.*;
import mekanism.generators.common.FusionReactor;
import mekanism.generators.common.item.ItemHohlraum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TileEntityReactorController extends TileEntityReactorBlock implements IActiveState {

    public FluidTank waterTank = new FluidTankSync(MekanismConfig.current().generators.FusionReactorsWaterTank.val());
    public FluidTank steamTank = new FluidTankSync(MekanismConfig.current().generators.FusionReactorsSteamTank.val());

    public GasTank deuteriumTank = new GasTank(MekanismConfig.current().generators.FusionReactorsDeuteriumTank.val());
    public GasTank tritiumTank = new GasTank(MekanismConfig.current().generators.FusionReactorsTritiumTank.val());

    public GasTank fuelTank = new GasTank(MekanismConfig.current().generators.FusionReactorsFuelTank.val());

    public AxisAlignedBB box;
    public double clientTemp = 0;
    public boolean clientBurning = false;
    private SoundEvent soundEvent = new SoundEvent(new ResourceLocation(Mekanism.MODID, "tile.machine.fusionreactor"));
    @SideOnly(Side.CLIENT)
    private ISound activeSound;
    private int playSoundCooldown = 0;
    @SideOnly(Side.CLIENT)
    private static final int REACTOR_WINDOW_RAY_MAX_STEPS = 24;
    @SideOnly(Side.CLIENT)
    private static final double REACTOR_CORE_PROBE_RADIUS = 1.25D;
    @SideOnly(Side.CLIENT)
    private static final double REACTOR_CORE_PROBE_VERTICAL_RADIUS = 1.05D;

    public TileEntityReactorController() {
        super("ReactorController", MekanismConfig.current().generators.reactorGeneratorStorage.val());
        inventory = NonNullListSynchronized.withSize(1, ItemStack.EMPTY);
    }

    @Override
    public boolean isFrame() {
        return false;
    }

    public void radiateNeutrons(int neutrons) {
        //future impl
    }

    public void formMultiblock(boolean keepBurning) {
        if (getReactor() == null) {
            setReactor(new FusionReactor(this));
        }
        getReactor().formMultiblock(keepBurning);
    }

    public double getPlasmaTemp() {
        if (getReactor() == null || !getReactor().isFormed()) {
            return 0;
        }
        return getReactor().getPlasmaTemp();
    }

    public double getCaseTemp() {
        if (getReactor() == null || !getReactor().isFormed()) {
            return 0;
        }
        return getReactor().getCaseTemp();
    }

    public boolean getactivelyCooled() {
        return steamTank.getFluidAmount() < steamTank.getCapacity() && steamTank.getFluidAmount() != steamTank.getCapacity();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (isFormed()) {
            getReactor().simulate();
            if (!isRemote() && (getReactor().isBurning() != clientBurning || Math.abs(getReactor().getPlasmaTemp() - clientTemp) > 1000000)) {
                Mekanism.packetHandler.sendUpdatePacket(this);
                clientBurning = getReactor().isBurning();
                clientTemp = getReactor().getPlasmaTemp();
            }
        }
    }

    @Override
    public void onUpdateClient(){
        super.onUpdateClient();
        updateSound();
    }

    @SideOnly(Side.CLIENT)
    private void updateSound() {
        // If machine sounds are disabled, noop
        if (!MekanismConfig.current().client.enableMachineSounds.val()) {
            return;
        }
        if (isBurning() && !isInvalid()) {
            // If sounds are being muted, we can attempt to start them on every tick, only to have them
            // denied by the event bus, so use a cooldown period that ensures we're only trying once every
            // second or so to start a sound.
            if (--playSoundCooldown > 0) {
                return;
            }
            if (activeSound == null || !Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(activeSound)) {
                activeSound = SoundHandler.startTileSound(soundEvent.getSoundName(), 1.0f, getPos());
                playSoundCooldown = 20;
            }
        } else if (activeSound != null) {
            SoundHandler.stopTileSound(getPos());
            activeSound = null;
            playSoundCooldown = 0;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (isRemote()) {
            updateSound();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        formMultiblock(true);
    }

    @Override
    public void onAdded() {
        super.onAdded();
        formMultiblock(false);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setBoolean("formed", isFormed());
        if (isFormed()) {
            tag.setDouble("plasmaTemp", getReactor().getPlasmaTemp());
            tag.setDouble("caseTemp", getReactor().getCaseTemp());
            tag.setInteger("injectionRate", getReactor().getInjectionRate());
            tag.setBoolean("burning", getReactor().isBurning());
        } else {
            tag.setDouble("plasmaTemp", 0);
            tag.setDouble("caseTemp", 0);
            tag.setInteger("injectionRate", 0);
            tag.setBoolean("burning", false);
        }
        tag.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
        tag.setTag("deuteriumTank", deuteriumTank.write(new NBTTagCompound()));
        tag.setTag("tritiumTank", tritiumTank.write(new NBTTagCompound()));
        tag.setTag("waterTank", waterTank.writeToNBT(new NBTTagCompound()));
        tag.setTag("steamTank", steamTank.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        boolean formed = tag.getBoolean("formed");
        if (formed) {
            setReactor(new FusionReactor(this));
            getReactor().setPlasmaTemp(tag.getDouble("plasmaTemp"));
            getReactor().setCaseTemp(tag.getDouble("caseTemp"));
            getReactor().setInjectionRate(tag.getInteger("injectionRate"));
            getReactor().setBurning(tag.getBoolean("burning"));
            getReactor().updateTemperatures();
        }
        fuelTank.read(tag.getCompoundTag("fuelTank"));
        deuteriumTank.read(tag.getCompoundTag("deuteriumTank"));
        tritiumTank.read(tag.getCompoundTag("tritiumTank"));
        waterTank.readFromNBT(tag.getCompoundTag("waterTank"));
        steamTank.readFromNBT(tag.getCompoundTag("steamTank"));
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(getReactor() != null && getReactor().isFormed());
        if (getReactor() != null) {
            data.add(getReactor().getPlasmaTemp());
            data.add(getReactor().getCaseTemp());
            data.add(getReactor().getInjectionRate());
            data.add(getReactor().isBurning());

            data.add(fuelTank.getStored());
            data.add(deuteriumTank.getStored());
            data.add(tritiumTank.getStored());
            /*
            TileUtils.addTankData(data,fuelTank);
            TileUtils.addTankData(data,deuteriumTank);
            TileUtils.addTankData(data,tritiumTank);
            */
            TileUtils.addTankData(data, waterTank);
            TileUtils.addTankData(data, steamTank);
        }
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                if (getReactor() != null) {
                    getReactor().setInjectionRate(dataStream.readInt());
                }
            }
            return;
        }

        super.handlePacketData(dataStream);

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            boolean formed = dataStream.readBoolean();
            if (formed) {
                if (getReactor() == null || !getReactor().formed) {
                    BlockPos corner = getPos().subtract(new Vec3i(2, 4, 2));
                    Mekanism.proxy.doMultiblockSparkle(this, corner, 5, 5, 6, tile -> tile instanceof TileEntityReactorBlock);
                }
                if (getReactor() == null) {
                    setReactor(new FusionReactor(this));
                    MekanismUtils.updateBlock(world, getPos());
                }

                getReactor().formed = true;
                getReactor().setPlasmaTemp(dataStream.readDouble());
                getReactor().setCaseTemp(dataStream.readDouble());
                getReactor().setInjectionRate(dataStream.readInt());
                getReactor().setBurning(dataStream.readBoolean());

                fuelTank.setGas(new GasStack(MekanismFluids.FusionFuel, dataStream.readInt()));
                deuteriumTank.setGas(new GasStack(MekanismFluids.Deuterium, dataStream.readInt()));
                tritiumTank.setGas(new GasStack(MekanismFluids.Tritium, dataStream.readInt()));
               /*
                TileUtils.readTankData(dataStream,fuelTank);
                TileUtils.readTankData(dataStream,deuteriumTank);
                TileUtils.readTankData(dataStream,tritiumTank);
                */
                TileUtils.readTankData(dataStream, waterTank);
                TileUtils.readTankData(dataStream, steamTank);
            } else if (getReactor() != null) {
                setReactor(null);
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    public boolean isFormed() {
        return getReactor() != null && getReactor().isFormed();
    }

    public boolean isBurning() {
        return getActive() && getReactor().isBurning();
    }

    @Override
    public boolean getActive() {
        return isFormed();
    }

    @Override
    public void setActive(boolean active) {
        if (active == (getReactor() == null)) {
            setReactor(active ? new FusionReactor(this) : null);
        }
    }

    @Override
    public boolean renderUpdate() {
        return true;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldCullForOcclusion() {
        if (MekanismConfig.current().client.GazeCullingTracking.val() && shouldRenderPlasmaCore()) {
            return false;
        }
        return super.shouldCullForOcclusion();
    }

    @SideOnly(Side.CLIENT)
    public boolean shouldRenderPlasmaCore() {
        if (!isBurning() || !isFormed() || world == null) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return false;
        }
        if (mc.gameSettings.thirdPersonView != 0) {
            return true;
        }
        Entity renderView = mc.getRenderViewEntity();
        if (renderView == null) {
            return false;
        }
        Vec3d eyePos = renderView.getPositionEyes(1.0F);
        List<Vec3d> probePoints = buildCoreProbePoints();
        for (Vec3d probePoint : probePoints) {
            if (canSeePointThroughReactorWindow(eyePos, probePoint)) {
                return true;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    private List<Vec3d> buildCoreProbePoints() {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() - 1.5D;
        double centerZ = pos.getZ() + 0.5D;
        double[] xs = new double[]{centerX - REACTOR_CORE_PROBE_RADIUS, centerX, centerX + REACTOR_CORE_PROBE_RADIUS};
        double[] ys = new double[]{centerY - REACTOR_CORE_PROBE_VERTICAL_RADIUS, centerY, centerY + REACTOR_CORE_PROBE_VERTICAL_RADIUS};
        double[] zs = new double[]{centerZ - REACTOR_CORE_PROBE_RADIUS, centerZ, centerZ + REACTOR_CORE_PROBE_RADIUS};
        List<Vec3d> probes = new ArrayList<>(27);
        for (double y : ys) {
            for (double x : xs) {
                for (double z : zs) {
                    probes.add(new Vec3d(x, y, z));
                }
            }
        }
        return probes;
    }

    @SideOnly(Side.CLIENT)
    private boolean canSeePointThroughReactorWindow(Vec3d eyePos, Vec3d target) {
        Vec3d start = eyePos;
        Vec3d direction = target.subtract(eyePos);
        double distanceSq = direction.lengthSquared();
        if (distanceSq <= 1.0E-8D) {
            return true;
        }
        Vec3d directionNorm = direction.scale(1.0D / Math.sqrt(distanceSq));
        boolean passedWindow = false;
        for (int i = 0; i < REACTOR_WINDOW_RAY_MAX_STEPS; i++) {
            RayTraceResult trace = world.rayTraceBlocks(start, target, false, true, false);
            if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK) {
                return passedWindow;
            }
            BlockPos hitPos = trace.getBlockPos();
            if (isReactorWindow(hitPos)) {
                passedWindow = true;
            }
            IBlockState hitState = world.getBlockState(hitPos);
            if (!isTransparentForReactorRay(hitState, hitPos)) {
                return false;
            }
            if (trace.hitVec == null) {
                return false;
            }
            start = trace.hitVec.add(directionNorm.scale(0.01D));
            if (start.squareDistanceTo(target) < 1.0E-6D) {
                return passedWindow;
            }
        }
        return passedWindow;
    }

    @SideOnly(Side.CLIENT)
    private boolean isReactorWindow(BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityReactorGlass || tile instanceof TileEntityReactorLaserFocusMatrix;
    }

    @SideOnly(Side.CLIENT)
    private boolean isTransparentForReactorRay(IBlockState state, BlockPos pos) {
        if (state == null) {
            return false;
        }
        if (isReactorWindow(pos) || state.getMaterial().isLiquid()) {
            return true;
        }
        if (!state.isFullCube()) {
            return true;
        }
        if (!state.isOpaqueCube()) {
            return true;
        }
        return state.getBlock().isTranslucent(state);
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        if (box == null) {
            box = new AxisAlignedBB(getPos().getX() - 1, getPos().getY() - 3, getPos().getZ() - 1, getPos().getX() + 2, getPos().getY(), getPos().getZ() + 2);
        }
        return box;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return isFormed() ? new int[]{0} : InventoryUtils.EMPTY;
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return stack.getItem() instanceof ItemHohlraum;
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            //Allow inserting
            return false;
        }
        return super.isCapabilityDisabled(capability, side);
    }
}
