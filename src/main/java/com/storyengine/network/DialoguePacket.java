package com.storyengine.network;

import com.storyengine.gui.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DialoguePacket {
    private final String speakerName;
    private final String text;
    private final List<String> choices;
    private final boolean close;

    public DialoguePacket(String speakerName, String text,
                          List<String> choices, boolean close) {
        this.speakerName = speakerName;
        this.text = text;
        this.choices = choices;
        this.close = close;
    }

    public static void encode(DialoguePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.speakerName);
        buf.writeUtf(pkt.text);
        buf.writeInt(pkt.choices.size());
        for (String c : pkt.choices) buf.writeUtf(c);
        buf.writeBoolean(pkt.close);
    }

    public static DialoguePacket decode(FriendlyByteBuf buf) {
        String speaker = buf.readUtf();
        String text = buf.readUtf();
        int size = buf.readInt();
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < size; i++) choices.add(buf.readUtf());
        boolean close = buf.readBoolean();
        return new DialoguePacket(speaker, text, choices, close);
    }

    public static void handle(DialoguePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (pkt.close) {
                    Minecraft.getInstance().setScreen(null);
                } else {
                    Minecraft.getInstance().setScreen(
                            new DialogueScreen(pkt.speakerName, pkt.text, pkt.choices));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}