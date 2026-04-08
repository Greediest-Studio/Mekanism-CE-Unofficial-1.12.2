package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.item.ItemPortableTeleporter;
import mekanism.common.network.PacketPortableTeleporter.PortableTeleporterMessage;
import mekanism.common.network.PacketPortalFX.PortalFXMessage;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketPortableTeleporter implements IMessageHandler<PortableTeleporterMessage, IMessage> {

    @Override
    public IMessage onMessage(PortableTeleporterMessage message, MessageContext context) {
        EntityPlayer player = PacketHandler.getPlayer(context);
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            ItemStack itemstack = player.getHeldItem(message.currentHand);
            World world = player.world;
            if (!itemstack.isEmpty() && itemstack.getItem() instanceof ItemPortableTeleporter item) {
                if (!world.isRemote && !SecurityUtils.canAccess(player, itemstack)) {
                    return;
                }
                switch (message.packetType) {
                    case DATA_REQUEST ->
                            sendDataResponse(message.frequency, world, player, item, itemstack, message.currentHand);
                    case DATA_RESPONSE -> Mekanism.proxy.handleTeleporterUpdate(message);
                    case SET_FREQ -> {
                        if (message.frequency == null || message.frequency.name == null) {
                            break;
                        }
                        FrequencyManager manager1 = getManager(message.frequency.isPublic() ? null : player.getUniqueID(), world);
                        Frequency toUse = null;
                        for (Frequency freq : manager1.getFrequencies()) {
                            if (freq.name.equals(message.frequency.name)) {
                                toUse = freq;
                                break;
                            }
                        }
                        if (toUse == null) {
                            toUse = new Frequency(message.frequency.name, player.getPersistentID()).setPublic(message.frequency.isPublic());
                            manager1.addFrequency(toUse);
                        }
                        item.setFrequency(itemstack, toUse);
                        sendDataResponse(toUse, world, player, item, itemstack, message.currentHand);
                    }
                    case DEL_FREQ -> {
                        if (message.frequency == null || message.frequency.name == null) {
                            break;
                        }
                        FrequencyManager manager = getManager(message.frequency.isPublic() ? null : player.getUniqueID(), world);
                        manager.remove(message.frequency.name, player.getUniqueID());
                        item.setFrequency(itemstack, null);
                    }
                    case TELEPORT -> {
                        if (message.frequency == null || message.frequency.name == null) {
                            break;
                        }
                        FrequencyManager manager2 = getManager(message.frequency.isPublic() ? null : player.getUniqueID(), world);
                        Frequency found = null;
                        for (Frequency freq : manager2.getFrequencies()) {
                            if (message.frequency.name.equals(freq.name)) {
                                found = freq;
                                break;
                            }
                        }
                        if (found == null) {
                            break;
                        }
                        Coord4D coords = found.getClosestCoords(new Coord4D(player));
                        if (coords != null) {
                            World teleWorld = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(coords.dimensionId);
                            if (teleWorld != null && coords.getTileEntity(teleWorld) instanceof TileEntityTeleporter teleporter) {
                                try {
                                    if (!SecurityUtils.canAccess(player, teleporter)) {
                                        break;
                                    }
                                    double energyCost = ItemPortableTeleporter.calculateEnergyCost(player, coords);
                                    if (energyCost > item.getEnergy(itemstack)) {
                                        break;
                                    }
                                    teleporter.didTeleport.add(player.getPersistentID());
                                    teleporter.teleDelay = 5;
                                    item.setEnergy(itemstack, item.getEnergy(itemstack) - energyCost);
                                    if (player instanceof EntityPlayerMP mp) {
                                        mp.connection.floatingTickCount = 0;
                                    }
                                    player.closeScreen();
                                    Mekanism.packetHandler.sendToAllTracking(new PortalFXMessage(new Coord4D(player)), coords);
                                    if (player instanceof EntityPlayerMP mp) {
                                        TileEntityTeleporter.teleportPlayerTo(mp, coords, teleporter);
                                        TileEntityTeleporter.alignPlayer(mp, coords);
                                    }
                                    world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                    Mekanism.packetHandler.sendToAllTracking(new PortalFXMessage(coords), coords);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }, player);
        return null;
    }

    public void sendDataResponse(Frequency given, World world, EntityPlayer player, ItemPortableTeleporter item, ItemStack itemstack, EnumHand hand) {
        List<Frequency> publicFreqs = new ArrayList<>(getManager(null, world).getFrequencies());
        List<Frequency> privateFreqs = new ArrayList<>(getManager(player.getUniqueID(), world).getFrequencies());
        byte status = 3;
        if (given != null) {
            FrequencyManager manager = given.isPublic() ? getManager(null, world) : getManager(player.getUniqueID(), world);
            boolean found = false;
            for (Frequency iterFreq : manager.getFrequencies()) {
                if (given.equals(iterFreq)) {
                    given = iterFreq;
                    found = true;
                    break;
                }
            }
            if (!found) {
                given = null;
            }
        }

        if (given != null) {
            if (given.activeCoords.size() == 0) {
                status = 3;
            } else {
                Coord4D coords = given.getClosestCoords(new Coord4D(player));
                double energyNeeded = ItemPortableTeleporter.calculateEnergyCost(player, coords);
                if (energyNeeded > item.getEnergy(itemstack)) {
                    status = 4;
                } else {
                    status = 1;
                }
            }
        }
        Mekanism.packetHandler.sendTo(new PortableTeleporterMessage(hand, given, status, publicFreqs, privateFreqs), (EntityPlayerMP) player);
    }

    public FrequencyManager getManager(UUID owner, World world) {
        if (owner == null) {
            return Mekanism.publicTeleporters;
        } else if (!Mekanism.privateTeleporters.containsKey(owner)) {
            FrequencyManager manager = new FrequencyManager(Frequency.class, Frequency.TELEPORTER, owner);
            Mekanism.privateTeleporters.put(owner, manager);
            manager.createOrLoad(world);
        }
        return Mekanism.privateTeleporters.get(owner);
    }

    public enum PortableTeleporterPacketType {
        DATA_REQUEST,
        DATA_RESPONSE,
        SET_FREQ,
        DEL_FREQ,
        TELEPORT
    }

    public static class PortableTeleporterMessage implements IMessage {

        public PortableTeleporterPacketType packetType;

        public EnumHand currentHand;
        public Frequency frequency;
        public byte status;

        public List<Frequency> publicCache = new ArrayList<>();
        public List<Frequency> privateCache = new ArrayList<>();

        public PortableTeleporterMessage() {
        }

        public PortableTeleporterMessage(PortableTeleporterPacketType type, EnumHand hand, Frequency freq) {
            packetType = type;
            currentHand = hand;
            if (type == PortableTeleporterPacketType.DATA_REQUEST) {
                frequency = freq;
            } else if (type == PortableTeleporterPacketType.SET_FREQ) {
                frequency = freq;
            } else if (type == PortableTeleporterPacketType.DEL_FREQ) {
                frequency = freq;
            } else if (type == PortableTeleporterPacketType.TELEPORT) {
                frequency = freq;
            }
        }

        public PortableTeleporterMessage(EnumHand hand, Frequency freq, byte b, List<Frequency> publicFreqs, List<Frequency> privateFreqs) {
            packetType = PortableTeleporterPacketType.DATA_RESPONSE;

            currentHand = hand;
            frequency = freq;
            status = b;

            publicCache = publicFreqs;
            privateCache = privateFreqs;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(packetType.ordinal());

            if (packetType == PortableTeleporterPacketType.DATA_REQUEST) {
                buffer.writeInt(currentHand.ordinal());
                if (frequency != null) {
                    buffer.writeBoolean(true);
                    PacketHandler.writeString(buffer, frequency.name);
                    buffer.writeBoolean(frequency.publicFreq);
                } else {
                    buffer.writeBoolean(false);
                }
            } else if (packetType == PortableTeleporterPacketType.DATA_RESPONSE) {
                buffer.writeInt(currentHand.ordinal());

                if (frequency != null) {
                    buffer.writeBoolean(true);
                    PacketHandler.writeString(buffer, frequency.name);
                    buffer.writeBoolean(frequency.publicFreq);
                } else {
                    buffer.writeBoolean(false);
                }

                buffer.writeByte(status);

                TileNetworkList data = new TileNetworkList();
                data.add(publicCache.size());
                publicCache.forEach(freq -> freq.write(data));

                data.add(privateCache.size());
                privateCache.forEach(freq -> freq.write(data));

                PacketHandler.encode(data.toArray(), buffer);
            } else if (packetType == PortableTeleporterPacketType.SET_FREQ) {
                buffer.writeInt(currentHand.ordinal());
                PacketHandler.writeString(buffer, frequency.name);
                buffer.writeBoolean(frequency.publicFreq);
            } else if (packetType == PortableTeleporterPacketType.DEL_FREQ) {
                buffer.writeInt(currentHand.ordinal());
                PacketHandler.writeString(buffer, frequency.name);
                buffer.writeBoolean(frequency.publicFreq);
            } else if (packetType == PortableTeleporterPacketType.TELEPORT) {
                buffer.writeInt(currentHand.ordinal());
                PacketHandler.writeString(buffer, frequency.name);
                buffer.writeBoolean(frequency.publicFreq);
            }
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            packetType = MekanismUtils.getByIndex(PortableTeleporterPacketType.values(), buffer.readInt(), PortableTeleporterPacketType.DATA_REQUEST);
            if (packetType == PortableTeleporterPacketType.DATA_REQUEST) {
                currentHand = MekanismUtils.getByIndex(EnumHand.values(), buffer.readInt(), EnumHand.MAIN_HAND);
                if (buffer.readBoolean()) {
                    frequency = new Frequency(PacketHandler.readString(buffer), null).setPublic(buffer.readBoolean());
                }
            } else if (packetType == PortableTeleporterPacketType.DATA_RESPONSE) {
                currentHand = MekanismUtils.getByIndex(EnumHand.values(), buffer.readInt(), EnumHand.MAIN_HAND);
                if (buffer.readBoolean()) {
                    frequency = new Frequency(PacketHandler.readString(buffer), null).setPublic(buffer.readBoolean());
                }
                status = buffer.readByte();

                int amount = buffer.readInt();
                for (int i = 0; i < amount; i++) {
                    publicCache.add(new Frequency(buffer));
                }
                amount = buffer.readInt();
                for (int i = 0; i < amount; i++) {
                    privateCache.add(new Frequency(buffer));
                }
            } else if (packetType == PortableTeleporterPacketType.SET_FREQ) {
                currentHand = MekanismUtils.getByIndex(EnumHand.values(), buffer.readInt(), EnumHand.MAIN_HAND);
                frequency = new Frequency(PacketHandler.readString(buffer), null).setPublic(buffer.readBoolean());
            } else if (packetType == PortableTeleporterPacketType.DEL_FREQ) {
                currentHand = MekanismUtils.getByIndex(EnumHand.values(), buffer.readInt(), EnumHand.MAIN_HAND);
                frequency = new Frequency(PacketHandler.readString(buffer), null).setPublic(buffer.readBoolean());
            } else if (packetType == PortableTeleporterPacketType.TELEPORT) {
                currentHand = MekanismUtils.getByIndex(EnumHand.values(), buffer.readInt(), EnumHand.MAIN_HAND);
                frequency = new Frequency(PacketHandler.readString(buffer), null).setPublic(buffer.readBoolean());
            }
        }
    }
}
