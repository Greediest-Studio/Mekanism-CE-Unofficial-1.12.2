package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.content.miner.MinerFilter;
import mekanism.common.content.transporter.TransporterFilter;
import mekanism.common.network.PacketNewFilter.NewFilterMessage;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.tile.machine.TileEntityOredictionificator;
import mekanism.common.tile.machine.TileEntityOredictionificator.OredictionificatorFilter;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketNewFilter implements IMessageHandler<NewFilterMessage, IMessage> {

    @Override
    public IMessage onMessage(NewFilterMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().player;
        if (player == null) {
            return null;
        }
        WorldServer worldServer = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(message.coord4D.dimensionId);
        if (worldServer == null) {
            return null;
        }

        worldServer.addScheduledTask(() -> {
            if (message.type == 0 && message.coord4D.getTileEntity(worldServer) instanceof TileEntityLogisticalSorter sorter) {
                if (!PacketHandler.canAccessTile(player, sorter) || message.tFilter == null) {
                    return;
                }
                sorter.filters.add(message.tFilter);
                sorter.markDirty();
                TileNetworkList filterPacket = sorter.getFilterPacket(new TileNetworkList());
                sorter.playersUsing.forEach(iterPlayer -> Mekanism.packetHandler.sendTo(new TileEntityMessage(sorter, filterPacket), (EntityPlayerMP) iterPlayer));
                if (!sorter.playersUsing.contains(player)) {
                    Mekanism.packetHandler.sendTo(new TileEntityMessage(sorter, filterPacket), player);
                }
            } else if (message.type == 1 && message.coord4D.getTileEntity(worldServer) instanceof TileEntityDigitalMiner miner) {
                if (!PacketHandler.canAccessTile(player, miner) || message.mFilter == null) {
                    return;
                }
                miner.filters.add(message.mFilter);
                miner.markDirty();
                TileNetworkList filterPacket = miner.getFilterPacket(new TileNetworkList());
                miner.playersUsing.forEach(iterPlayer -> Mekanism.packetHandler.sendTo(new TileEntityMessage(miner, filterPacket), (EntityPlayerMP) iterPlayer));
                if (!miner.playersUsing.contains(player)) {
                    Mekanism.packetHandler.sendTo(new TileEntityMessage(miner, filterPacket), player);
                }
            } else if (message.type == 2 && message.coord4D.getTileEntity(worldServer) instanceof TileEntityOredictionificator oredictionificator) {
                if (!PacketHandler.canAccessTile(player, oredictionificator) || message.oFilter == null) {
                    return;
                }
                oredictionificator.filters.add(message.oFilter);
                oredictionificator.markDirty();
                TileNetworkList filterPacket = oredictionificator.getFilterPacket(new TileNetworkList());
                oredictionificator.playersUsing.forEach(iterPlayer -> Mekanism.packetHandler.sendTo(new TileEntityMessage(oredictionificator, filterPacket), (EntityPlayerMP) iterPlayer));
                if (!oredictionificator.playersUsing.contains(player)) {
                    Mekanism.packetHandler.sendTo(new TileEntityMessage(oredictionificator, filterPacket), player);
                }
            }
        });
        return null;
    }

    public static class NewFilterMessage implements IMessage {

        public Coord4D coord4D;

        public TransporterFilter tFilter;

        public MinerFilter mFilter;

        public OredictionificatorFilter oFilter;

        public byte type = -1;

        public NewFilterMessage() {
        }

        public NewFilterMessage(Coord4D coord, Object filter) {
            coord4D = coord;

            if (filter instanceof TransporterFilter transporterFilter) {
                tFilter = transporterFilter;
                type = 0;
            } else if (filter instanceof MinerFilter minerFilter) {
                mFilter = minerFilter;
                type = 1;
            } else if (filter instanceof OredictionificatorFilter oredictionificatorFilter) {
                oFilter = oredictionificatorFilter;
                type = 2;
            }
        }

        @Override
        public void toBytes(ByteBuf dataStream) {
            coord4D.write(dataStream);
            dataStream.writeByte(type);
            TileNetworkList data = new TileNetworkList();
            if (type == 0) {
                tFilter.write(data);
            } else if (type == 1) {
                mFilter.write(data);
            } else if (type == 2) {
                oFilter.write(data);
            }
            PacketHandler.encode(data.toArray(), dataStream);
        }

        @Override
        public void fromBytes(ByteBuf dataStream) {
            coord4D = Coord4D.read(dataStream);
            type = dataStream.readByte();
            if (type == 0) {
                tFilter = TransporterFilter.readFromPacket(dataStream);
            } else if (type == 1) {
                mFilter = MinerFilter.readFromPacket(dataStream);
            } else if (type == 2) {
                oFilter = OredictionificatorFilter.readFromPacket(dataStream);
            }
        }
    }
}
