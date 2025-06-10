package cc.loveloli.packagespy.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

import java.lang.reflect.Field;

public class PackageSpyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            ClientConnection conn = handler.getConnection();
            conn.channel.pipeline().addBefore("packet_handler", "packetspy", new ChannelDuplexHandler() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof Packet<?> p) dump("IN", p);
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof Packet<?> p) dump("OUT", p);
                    super.write(ctx, msg, promise);
                }

                private void dump(String dir, Packet<?> p) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[").append(dir).append("] ").append(p.getClass().getSimpleName()).append(" { \n");
                    Class<?> clazz = p.getClass();
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        try {
                            Object value = field.get(p);
                            stringBuilder.append(" - ").append(field.getName()).append(" (").append(field.getType().getSimpleName()).append("): ").append(value).append("\n");
                        } catch (IllegalAccessException e) {
                            stringBuilder.append(" - ").append(field.getName()).append(": <access denied>\n");
                        }
                    }
                    System.out.println(stringBuilder + "}");
                }
            });
        });
    }
}
