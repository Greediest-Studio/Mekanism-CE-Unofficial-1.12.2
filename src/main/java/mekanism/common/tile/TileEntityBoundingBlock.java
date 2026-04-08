package mekanism.common.tile;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITileNetwork;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.network.PacketDataRequest.DataRequestMessage;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;

/**
 * Multi-block used by wind turbines, solar panels, and other machines
 */
public class TileEntityBoundingBlock extends TileEntity implements ITileNetwork, ITickable {

    private static final int VALIDATION_RATE = 20;
    private static final int NO_MAIN_COORDS_GRACE_TICKS = 100;

    public boolean receivedCoords;
    public int prevPower;
    public int ticker;
    private BlockPos mainPos = BlockPos.ORIGIN;

    public void setMainLocation(BlockPos pos) {
        receivedCoords = pos != null;
        mainPos = pos == null ? BlockPos.ORIGIN : pos;
        if (!world.isRemote) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }

    public BlockPos getMainPos() {
        if (mainPos == null) {
            mainPos = BlockPos.ORIGIN;
        }
        return mainPos;
    }


    @Override
    public void validate() {
        super.validate();
        if (world.isRemote) {
            Mekanism.packetHandler.sendToServer(new DataRequestMessage(Coord4D.get(this)));
        }
    }

    public TileEntity getMainTile() {
        if (receivedCoords && world.isBlockLoaded(getMainPos())) {
            TileEntity tile = world.getTileEntity(getMainPos());
            if (!world.isRemote && !(tile instanceof IBoundingBlock)) {
                world.setBlockToAir(getPos());
                return null;
            }
            return tile;
        }
        return null;
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        ticker++;
        if (ticker % VALIDATION_RATE != 0) {
            return;
        }
        if (!receivedCoords) {
            if (ticker >= NO_MAIN_COORDS_GRACE_TICKS) {
                world.setBlockToAir(getPos());
            }
            return;
        }
        getMainTile();
    }

    public void onNeighborChange(Block block) {
        final TileEntity tile = getMainTile();
        if (tile instanceof final TileEntityBasicBlock tileEntity) {
            int power = world.getRedstonePowerFromNeighbors(getPos());
            if (prevPower != power) {
                if (power > 0) {
                    onPower();
                } else {
                    onNoPower();
                }
                prevPower = power;
                Mekanism.packetHandler.sendToAllTracking(new TileEntityMessage(tileEntity), this);
            }
        }
    }

    public void onPower() {
    }

    public void onNoPower() {
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (world.isRemote) {
            mainPos = new BlockPos(dataStream.readInt(), dataStream.readInt(), dataStream.readInt());
            prevPower = dataStream.readInt();
            receivedCoords = dataStream.readBoolean();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTags) {
        super.readFromNBT(nbtTags);
        mainPos = new BlockPos(nbtTags.getInteger("mainX"), nbtTags.getInteger("mainY"), nbtTags.getInteger("mainZ"));
        prevPower = nbtTags.getInteger("prevPower");
        receivedCoords = nbtTags.getBoolean("receivedCoords");
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTags) {
        super.writeToNBT(nbtTags);
        nbtTags.setInteger("mainX", getMainPos().getX());
        nbtTags.setInteger("mainY", getMainPos().getY());
        nbtTags.setInteger("mainZ", getMainPos().getZ());
        nbtTags.setInteger("prevPower", prevPower);
        nbtTags.setBoolean("receivedCoords", receivedCoords);
        return nbtTags;
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        data.add(getMainPos().getX());
        data.add(getMainPos().getY());
        data.add(getMainPos().getZ());
        data.add(prevPower);
        data.add(receivedCoords);
        return data;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == Capabilities.TILE_NETWORK_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        if (capability == Capabilities.TILE_NETWORK_CAPABILITY) {
            return Capabilities.TILE_NETWORK_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, facing);
    }

}
