package com.storyengine.network;

import com.storyengine.gui.LevelHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LevelSyncPacket {
    public final int level;
    public final long xp;
    public final long xpToNext;
    public final int points;
    public final int strength;
    public final int vitality;
    public final int agility;
    public final int endurance;
    public final int luck;

    public LevelSyncPacket(int level, long xp, long xpToNext, int points,
                           int str, int vit, int agi, int end, int lck) {
        this.level = level;
        this.xp = xp;
        this.xpToNext = xpToNext;
        this.points = points;
        this.strength = str;
        this.vitality = vit;
        this.agility = agi;
        this.endurance = end;
        this.luck = lck;
    }

    public static void encode(LevelSyncPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.level);
        buf.writeLong(p.xp);
        buf.writeLong(p.xpToNext);
        buf.writeInt(p.points);
        buf.writeInt(p.strength);
        buf.writeInt(p.vitality);
        buf.writeInt(p.agility);
        buf.writeInt(p.endurance);
        buf.writeInt(p.luck);
    }

    public static LevelSyncPacket decode(FriendlyByteBuf buf) {
        return new LevelSyncPacket(
                buf.readInt(), buf.readLong(), buf.readLong(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(LevelSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    LevelHudOverlay.updateData(p));
        });
        ctx.get().setPacketHandled(true);
    }
}