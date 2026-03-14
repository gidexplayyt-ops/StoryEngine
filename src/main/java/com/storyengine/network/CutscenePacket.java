package com.storyengine.network;

import com.storyengine.gui.CutsceneOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CutscenePacket {
    public enum Action { START, STOP, FADE_IN, FADE_OUT }

    private final Action action;
    private final boolean hideHUD;
    private final boolean letterbox;
    private final int duration;

    public CutscenePacket(Action action, boolean hideHUD, boolean letterbox, int duration) {
        this.action = action;
        this.hideHUD = hideHUD;
        this.letterbox = letterbox;
        this.duration = duration;
    }

    public static void encode(CutscenePacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.action);
        buf.writeBoolean(pkt.hideHUD);
        buf.writeBoolean(pkt.letterbox);
        buf.writeInt(pkt.duration);
    }

    public static CutscenePacket decode(FriendlyByteBuf buf) {
        return new CutscenePacket(
                buf.readEnum(Action.class),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt());
    }

    public static void handle(CutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    CutsceneOverlay.handlePacket(pkt));
        });
        ctx.get().setPacketHandled(true);
    }

    public Action getAction() { return action; }
    public boolean isHideHUD() { return hideHUD; }
    public boolean isLetterbox() { return letterbox; }
    public int getDuration() { return duration; }
}