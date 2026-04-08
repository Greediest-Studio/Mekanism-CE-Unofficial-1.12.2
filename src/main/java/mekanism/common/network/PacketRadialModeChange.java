package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.api.radial.RadialData;
import mekanism.api.radial.mode.INestedRadialMode;
import mekanism.api.radial.mode.IRadialMode;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.lib.radial.IGenericRadialModeItem;
import mekanism.common.network.PacketRadialModeChange.RadialModeChangeMessage;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketRadialModeChange implements IMessageHandler<RadialModeChangeMessage, IMessage> {

    private static final int MAX_PATH_SIZE = 32;


    @Override
    @SuppressWarnings("ConstantConditions")//not null, validated by hasNestedData
    public IMessage onMessage(RadialModeChangeMessage message, MessageContext context) {
        EntityPlayer player = PacketHandler.getPlayer(context);
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            ItemStack stack = player.getItemStackFromSlot(message.slot);
            if (!stack.isEmpty() && stack.getItem() instanceof IGenericRadialModeItem radialModeItem) {
                RadialData<?> radialData = radialModeItem.getRadialData(stack);
                if (radialData != null) {
                    for (ResourceLocation path : message.path) {
                        INestedRadialMode nestedData = radialData.fromIdentifier(path);
                        if (nestedData == null || !nestedData.hasNestedData()) {
                            Mekanism.logger.warn("Could not find path ({}) in current radial data.", path);
                            return;
                        }
                        radialData = nestedData.nestedData();
                    }
                    setMode(player, stack, radialModeItem, radialData, message);
                }
            }
        }, player);
        return null;
    }


    private <MODE extends IRadialMode> void setMode(EntityPlayer player, ItemStack stack, IGenericRadialModeItem item, RadialData<MODE> radialData, RadialModeChangeMessage message) {
        MODE newMode = radialData.fromNetworkRepresentation(message.networkRepresentation);
        if (newMode != null) {
            item.setMode(stack, player, radialData, newMode);
        }
    }

    public static class RadialModeChangeMessage implements IMessage {

        private EntityEquipmentSlot slot;
        private List<ResourceLocation> path;
        private int networkRepresentation;

        public RadialModeChangeMessage() {
        }

        public RadialModeChangeMessage(EntityEquipmentSlot slot, List<ResourceLocation> path, int networkRepresentation) {
            this.slot = slot;
            this.path = path;
            this.networkRepresentation = networkRepresentation;
        }

        @Override
        public void toBytes(ByteBuf dataStream) {
            dataStream.writeInt(slot.ordinal());
            new PacketBuffer(dataStream).writeVarInt(path.size());
            path.forEach(paths -> new PacketBuffer(dataStream).writeResourceLocation(paths));
            dataStream.writeInt(networkRepresentation);
        }

        @Override
        public void fromBytes(ByteBuf dataStream) {
            slot = MekanismUtils.getByIndex(EntityEquipmentSlot.values(), dataStream.readInt(), EntityEquipmentSlot.MAINHAND);
            int size = new PacketBuffer(dataStream).readVarInt();
            if (size < 0 || size > MAX_PATH_SIZE) {
                throw new IllegalArgumentException("Invalid radial path size: " + size);
            }
            List<ResourceLocation> locations = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                locations.add(new PacketBuffer(dataStream).readResourceLocation());
            }
            path = locations;
            networkRepresentation = dataStream.readInt();
        }
    }

}
