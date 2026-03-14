package com.storyengine.network;

import com.storyengine.gui.StaminaHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StaminaSyncPacket {
    public final double stamina;
    public final double maxStamina;
    public final boolean exhausted;

    public StaminaSyncPacket(double stamina, double maxStamina, boolean exhausted) {
        this.stamina = stamina;
        this.maxStamina = maxStamina;
        this.exhausted = exhausted;
    }

    public static void encode(StaminaSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeDouble(pkt.stamina);
        buf.writeDouble(pkt.maxStamina);
        buf.writeBoolean(pkt.exhausted);
    }

    public static StaminaSyncPacket decode(FriendlyByteBuf buf) {
        return new StaminaSyncPacket(buf.readDouble(), buf.readDouble(), buf.readBoolean());
    }

    public static void handle(StaminaSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    StaminaHudOverlay.updateData(pkt.stamina, pkt.maxStamina, pkt.exhausted));
        });
        ctx.get().setPacketHandled(true);
    }
}