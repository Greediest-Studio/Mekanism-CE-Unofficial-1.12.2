package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.network.PacketGearStateUpdate.GearStateUpdateMessage;
import mekanism.common.network.PacketPlayerData.PlayerDataMessage;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class PacketGearStateUpdate implements IMessageHandler<GearStateUpdateMessage, IMessage> {

    @Override
    public IMessage onMessage(GearStateUpdateMessage message, MessageContext context) {
        EntityPlayer player = PacketHandler.getPlayer(context);
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            UUID playerUUID = player.getUniqueID();
            try {
                if (message.gearType == GearType.FLAMETHROWER) {
                    Mekanism.playerState.setFlamethrowerState(playerUUID, message.state, false);
                } else if (message.gearType == GearType.JETPACK) {
                    Mekanism.playerState.setJetpackState(playerUUID, message.state, false);
                } else if (message.gearType == GearType.SCUBA_MASK) {
                    Mekanism.playerState.setScubaMaskState(playerUUID, message.state, false);
                } else if (message.gearType == GearType.GRAVITATIONAL_MODULATOR) {
                    Mekanism.playerState.setGravitationalModulationState(playerUUID, message.state, false);
                }
                //If we got this packet on the server, inform all clients tracking the changed player
                //Note: We just resend all the data for the updated player as the packet size is about the same
                // and this allows us to separate the packet into a server to client and client to server packet
                Mekanism.packetHandler.sendToAllTracking(new PlayerDataMessage(playerUUID), player);
            } catch (Exception e) {
                Mekanism.logger.error("FIXME: Packet handling error", e);
            }
        }, player);
        return null;
    }

    public enum GearType {
        FLAMETHROWER,
        JETPACK,
        SCUBA_MASK,
        GRAVITATIONAL_MODULATOR
    }

    public static class GearStateUpdateMessage implements IMessage {

        private GearType gearType;
        private boolean state;
        private UUID uuid;

        public GearStateUpdateMessage() {
        }

        public GearStateUpdateMessage(GearType gearType, UUID uuid, boolean state) {
            this.gearType = gearType;
            this.state = state;
            this.uuid = uuid;
        }

        @Override
        public void toBytes(ByteBuf dataStream) {
            dataStream.writeInt(gearType.ordinal());
            dataStream.writeBoolean(state);
            PacketHandler.writeUUID(dataStream, uuid);
        }

        @Override
        public void fromBytes(ByteBuf dataStream) {
            gearType = MekanismUtils.getByIndex(GearType.values(), dataStream.readInt(), GearType.FLAMETHROWER);
            state = dataStream.readBoolean();
            uuid = PacketHandler.readUUID(dataStream);
        }


    }
}
