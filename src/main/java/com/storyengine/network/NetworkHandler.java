package com.storyengine.network;

import com.storyengine.StoryEngineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;
    private static int packetId = 0;

    private static int nextId() { return packetId++; }

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(StoryEngineMod.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

        // Сервер → Клиент
        CHANNEL.messageBuilder(DialoguePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialoguePacket::encode)
                .decoder(DialoguePacket::decode)
                .consumerMainThread(DialoguePacket::handle)
                .add();

        CHANNEL.messageBuilder(CutscenePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CutscenePacket::encode)
                .decoder(CutscenePacket::decode)
                .consumerMainThread(CutscenePacket::handle)
                .add();

        CHANNEL.messageBuilder(StaminaSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StaminaSyncPacket::encode)
                .decoder(StaminaSyncPacket::decode)
                .consumerMainThread(StaminaSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(LevelSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LevelSyncPacket::encode)
                .decoder(LevelSyncPacket::decode)
                .consumerMainThread(LevelSyncPacket::handle)
                .add();

        // Клиент → Сервер
        CHANNEL.messageBuilder(NPCInteractPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(NPCInteractPacket::encode)
                .decoder(NPCInteractPacket::decode)
                .consumerMainThread(NPCInteractPacket::handle)
                .add();

        StoryEngineMod.LOGGER.info("Network packets registered");
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}