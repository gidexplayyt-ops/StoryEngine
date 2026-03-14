package com.storyengine.network;

import com.storyengine.StoryEngineMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NPCInteractPacket {
    public enum InteractType {
        DIALOGUE_CHOICE,
        DIALOGUE_ADVANCE,
        SKIP_CUTSCENE
    }

    private final InteractType type;
    private final int data;

    public NPCInteractPacket(InteractType type, int data) {
        this.type = type;
        this.data = data;
    }

    public static void encode(NPCInteractPacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.type);
        buf.writeInt(pkt.data);
    }

    public static NPCInteractPacket decode(FriendlyByteBuf buf) {
        return new NPCInteractPacket(buf.readEnum(InteractType.class), buf.readInt());
    }

    public static void handle(NPCInteractPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (pkt.type) {
                case DIALOGUE_CHOICE ->
                        StoryEngineMod.getInstance().getDialogueManager()
                                .handleChoice(player, pkt.data);
                case DIALOGUE_ADVANCE ->
                        StoryEngineMod.getInstance().getDialogueManager()
                                .advanceDialogue(player);
                case SKIP_CUTSCENE ->
                        StoryEngineMod.getInstance().getCutsceneManager()
                                .stopCutscene(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}