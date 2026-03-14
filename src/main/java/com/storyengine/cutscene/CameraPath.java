package com.storyengine.cutscene;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CameraPath {
    private final List<CameraPoint> points;

    public CameraPath() {
        this.points = new ArrayList<>();
    }

    public CameraPath addPoint(CameraPoint point) {
        points.add(point);
        return this;
    }

    public CameraPath addPoint(double x, double y, double z,
                               float yaw, float pitch, int duration) {
        return addPoint(new CameraPoint(new Vec3(x, y, z), yaw, pitch, duration));
    }

    public List<CameraPoint> getPoints() { return points; }

    public int getTotalDuration() {
        return points.stream().mapToInt(CameraPoint::getDuration).sum();
    }

    public CameraState interpolate(int currentTick) {
        if (points.isEmpty()) return null;

        int elapsed = 0;
        for (int i = 0; i < points.size(); i++) {
            CameraPoint current = points.get(i);
            int nextElapsed = elapsed + current.getDuration();

            if (currentTick <= nextElapsed || i == points.size() - 1) {
                if (i == points.size() - 1) {
                    return new CameraState(current.getPosition(),
                            current.getYaw(), current.getPitch());
                }

                CameraPoint next = points.get(i + 1);
                float progress = current.getDuration() > 0
                        ? (float) (currentTick - elapsed) / current.getDuration() : 1.0f;
                progress = Math.max(0, Math.min(1, progress));
                progress = applyInterpolation(progress, current.getInterpolation());

                Vec3 pos = current.getPosition().lerp(next.getPosition(), progress);
                float yaw = lerp(current.getYaw(), next.getYaw(), progress);
                float pitch = lerp(current.getPitch(), next.getPitch(), progress);

                return new CameraState(pos, yaw, pitch);
            }
            elapsed = nextElapsed;
        }

        CameraPoint last = points.get(points.size() - 1);
        return new CameraState(last.getPosition(), last.getYaw(), last.getPitch());
    }

    private float applyInterpolation(float t, CameraPoint.InterpolationType type) {
        return switch (type) {
            case LINEAR -> t;
            case SMOOTH -> t * t * (3 - 2 * t);
            case CUBIC -> t * t * t * (t * (6 * t - 15) + 10);
            case INSTANT -> t >= 1.0f ? 1.0f : 0.0f;
        };
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static class CameraState {
        public final Vec3 position;
        public final float yaw;
        public final float pitch;

        public CameraState(Vec3 position, float yaw, float pitch) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}