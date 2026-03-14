package com.storyengine.cutscene;

import net.minecraft.world.phys.Vec3;

public class CameraPoint {
    private final Vec3 position;
    private final float yaw;
    private final float pitch;
    private final int duration;
    private final InterpolationType interpolation;

    public enum InterpolationType {
        LINEAR, SMOOTH, CUBIC, INSTANT
    }

    public CameraPoint(Vec3 position, float yaw, float pitch, int duration) {
        this(position, yaw, pitch, duration, InterpolationType.SMOOTH);
    }

    public CameraPoint(Vec3 position, float yaw, float pitch,
                       int duration, InterpolationType interpolation) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.duration = duration;
        this.interpolation = interpolation;
    }

    public Vec3 getPosition() { return position; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public int getDuration() { return duration; }
    public InterpolationType getInterpolation() { return interpolation; }
}