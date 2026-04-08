package mekanism.common.tile;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.client.render.bloom.BloomRenderSecurityDesk;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.config.MekanismConfig;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.frequency.IFrequencyHandler;
import mekanism.common.network.PacketSecurityUpdate.SecurityPacket;
import mekanism.common.network.PacketSecurityUpdate.SecurityUpdateMessage;
import mekanism.common.security.IOwnerItem;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile.SecurityMode;
import mekanism.common.security.SecurityData;
import mekanism.common.security.SecurityFrequency;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NonNullListSynchronized;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import java.util.UUID;

public class TileEntitySecurityDesk extends TileEntityContainerBlock implements IBoundingBlock, IFrequencyHandler, ISpecialSelectionWireframeTile {

    private static final int[] SLOTS = {0, 1};
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] SELECTION_ROTATE_SOUTH = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateY(180.0D, 0.5D, 0.5D, 0.5D)
    };
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] SELECTION_ROTATE_WEST = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateY(90.0D, 0.5D, 0.5D, 0.5D)
    };
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] SELECTION_ROTATE_EAST = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateY(270.0D, 0.5D, 0.5D, 0.5D)
    };

    public UUID ownerUUID;
    public String clientOwner;
    public SecurityFrequency frequency;

    public TileEntitySecurityDesk() {
        super("SecurityDesk");
        inventory = NonNullListSynchronized.withSize(SLOTS.length, ItemStack.EMPTY);
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        if (ownerUUID != null && frequency != null) {
            if (!inventory.get(0).isEmpty() && inventory.get(0).getItem() instanceof IOwnerItem ownerItem) {
                if (ownerItem.hasOwner(inventory.get(0)) && ownerItem.getOwnerUUID(inventory.get(0)) != null) {
                    if (ownerItem.getOwnerUUID(inventory.get(0)).equals(ownerUUID)) {
                        ownerItem.setOwnerUUID(inventory.get(0), null);
                        if (ownerItem instanceof ISecurityItem iSecurityItem && iSecurityItem.hasSecurity(inventory.get(0))) {
                            iSecurityItem.setSecurity(inventory.get(0), SecurityMode.PUBLIC);
                        }
                    }
                }
            }
            if (!inventory.get(1).isEmpty() && inventory.get(1).getItem() instanceof IOwnerItem item) {
                if (item.hasOwner(inventory.get(1))) {
                    if (item.getOwnerUUID(inventory.get(1)) == null) {
                        item.setOwnerUUID(inventory.get(1), ownerUUID);
                    }
                    if (item.getOwnerUUID(inventory.get(1)).equals(ownerUUID)) {
                        if (item instanceof ISecurityItem securityItem && securityItem.hasSecurity(inventory.get(1))) {
                            securityItem.setSecurity(inventory.get(1), frequency.securityMode);
                        }
                    }
                }
            }
        }
        if (frequency == null && ownerUUID != null) {
            setFrequency(ownerUUID);
        }
        FrequencyManager manager = getManager(frequency);
        if (manager != null) {
            if (frequency != null && !frequency.valid) {
                frequency = (SecurityFrequency) manager.validateFrequency(ownerUUID, Coord4D.get(this), frequency);
            }
            if (frequency != null) {
                frequency = (SecurityFrequency) manager.update(Coord4D.get(this), frequency);
            }
        } else {
            frequency = null;
        }
    }

    @Override
    public boolean supportsAsync() {
        return false;
    }

    public FrequencyManager getManager(Frequency freq) {
        if (ownerUUID == null || freq == null) {
            return null;
        }
        return Mekanism.securityFrequencies;
    }

    public void setFrequency(UUID owner) {
        FrequencyManager manager = Mekanism.securityFrequencies;
        manager.deactivate(Coord4D.get(this));
        for (Frequency freq : manager.getFrequencies()) {
            if (freq.ownerUUID.equals(owner)) {
                frequency = (SecurityFrequency) freq;
                frequency.activeCoords.add(Coord4D.get(this));
                return;
            }
        }

        Frequency freq = new SecurityFrequency(owner).setPublic(true);
        freq.activeCoords.add(Coord4D.get(this));
        manager.addFrequency(freq);
        frequency = (SecurityFrequency) freq;
//        MekanismUtils.saveChunk(this);
        markNoUpdateSync();
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                if (frequency != null) {
                    frequency.trusted.add(PacketHandler.readString(dataStream));
                }
            } else if (type == 1) {
                if (frequency != null) {
                    frequency.trusted.remove(PacketHandler.readString(dataStream));
                }
            } else if (type == 2) {
                if (frequency != null) {
                    frequency.override = !frequency.override;
                    Mekanism.packetHandler.sendToAll(new SecurityUpdateMessage(SecurityPacket.UPDATE, ownerUUID, new SecurityData(frequency)));
                }
            } else if (type == 3) {
                if (frequency != null) {
                    frequency.securityMode = MekanismUtils.getByIndex(SecurityMode.values(), dataStream.readInt(), frequency.securityMode);
                    Mekanism.packetHandler.sendToAll(new SecurityUpdateMessage(SecurityPacket.UPDATE, ownerUUID, new SecurityData(frequency)));
                }
            }
            MekanismUtils.saveChunk(this);
            return;
        }

        super.handlePacketData(dataStream);

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            if (dataStream.readBoolean()) {
                clientOwner = PacketHandler.readString(dataStream);
                ownerUUID = PacketHandler.readUUID(dataStream);
            } else {
                clientOwner = null;
                ownerUUID = null;
            }
            if (dataStream.readBoolean()) {
                frequency = new SecurityFrequency(dataStream);
            } else {
                frequency = null;
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        if (nbtTags.hasKey("ownerUUID")) {
            ownerUUID = MekanismUtils.parseUUID(nbtTags.getString("ownerUUID"));
        }
        if (nbtTags.hasKey("frequency")) {
            frequency = new SecurityFrequency(nbtTags.getCompoundTag("frequency"));
            frequency.valid = false;
        }
    }


    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        if (ownerUUID != null) {
            nbtTags.setString("ownerUUID", ownerUUID.toString());
        }
        if (frequency != null) {
            NBTTagCompound frequencyTag = new NBTTagCompound();
            frequency.write(frequencyTag);
            nbtTags.setTag("frequency", frequencyTag);
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        if (ownerUUID != null) {
            data.add(true);
            data.add(MekanismUtils.getLastKnownUsername(ownerUUID));
            data.add(ownerUUID.getMostSignificantBits());
            data.add(ownerUUID.getLeastSignificantBits());
        } else {
            data.add(false);
        }
        if (frequency != null) {
            data.add(true);
            frequency.write(data);
        } else {
            data.add(false);
        }
        return data;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!isRemote()) {
            if (frequency != null) {
                FrequencyManager manager = getManager(frequency);
                if (manager != null) {
                    manager.deactivate(Coord4D.get(this));
                }
            }
        }
    }

    @Override
    public void onPlace() {
        MekanismUtils.makeBoundingBlock(world, getPos().up(), Coord4D.get(this));
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().up());
        world.setBlockToAir(getPos());
    }

    @Override
    public Frequency getFrequency(FrequencyManager manager) {
        if (manager == Mekanism.securityFrequencies) {
            return frequency;
        }
        return null;
    }


    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        //Even though there are inventory slots make this return none as
        // accessible by automation, as then people could lock items to other
        // people unintentionally
        return InventoryUtils.EMPTY;
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            //For the same reason as the getSlotsForFace does not give any slots, don't expose this here
            return true;
        }
        return super.isCapabilityDisabled(capability, side);
    }

    @Override
    public void validate() {
        super.validate();
        if (isRemote()) {
            if (Mekanism.hooks.Bloom && MekanismConfig.current().client.enableBloom.val()) {
                new BloomRenderSecurityDesk(this);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekanism.client.model.ModelSecurityDesk.class;
    }

    @Override
    public boolean shouldApplyDefaultSelectionWireframeFacingRotation(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public ISpecialSelectionWireframeTile.SelectionTransform[] getSelectionWireframeTransforms(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing currentFacing = facing == null ? EnumFacing.NORTH : facing;
        return switch (currentFacing) {
            case SOUTH -> SELECTION_ROTATE_SOUTH;
            case WEST -> SELECTION_ROTATE_WEST;
            case EAST -> SELECTION_ROTATE_EAST;
            default -> SelectionTransform.EMPTY;
        };
    }
}
