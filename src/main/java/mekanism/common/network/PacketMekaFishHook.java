package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.gear.mekafishrod.IMekaFishHook;
import mekanism.common.network.PacketMekaFishHook.PacketMekaFishHookMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketMekaFishHook implements IMessageHandler<PacketMekaFishHookMessage, IMessage> {

    @Override
    public IMessage onMessage(PacketMekaFishHookMessage message, MessageContext context) {
        if (context.side.isClient()) {
            handleCapability(message);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static void handleCapability(PacketMekaFishHookMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world != null && mc.world.getEntityByID(message.entityId) instanceof EntityFishHook fishHook) {
                Capabilities.getMekaFishCap(fishHook).ifPresent(IMekaFishHook::setFish);
            }
        });
    }


    public static class PacketMekaFishHookMessage implements IMessage {
        private int entityId;

        public PacketMekaFishHookMessage() {
        }

        public PacketMekaFishHookMessage(int entityId) {
            this.entityId = entityId;
        }

        @Override
        public void toBytes(ByteBuf dataStream) {
            dataStream.writeInt(entityId);
        }

        @Override
        public void fromBytes(ByteBuf dataStream) {
            entityId = dataStream.readInt();
        }
    }

}
