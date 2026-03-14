package com.storyengine.stamina;

import net.minecraft.nbt.CompoundTag;

public class StaminaData {
    private double stamina;
    private double maxStamina;
    private boolean exhausted;

    public StaminaData() {
        this.maxStamina = 100.0;
        this.stamina = maxStamina;
        this.exhausted = false;
    }

    public double getStamina() { return stamina; }
    public double getMaxStamina() { return maxStamina; }
    public boolean isExhausted() { return exhausted; }
    public double getPercentage() { return maxStamina > 0 ? stamina / maxStamina : 0; }

    public void setMaxStamina(double max) {
        this.maxStamina = max;
        this.stamina = Math.min(stamina, max);
    }

    public boolean consume(double amount) {
        if (stamina >= amount) {
            stamina -= amount;
            if (stamina <= 0) {
                stamina = 0;
                exhausted = true;
            }
            return true;
        }
        return false;
    }

    public void regenerate(double amount) {
        stamina = Math.min(stamina + amount, maxStamina);
        if (stamina > maxStamina * 0.2) exhausted = false;
    }

    public void fill() {
        stamina = maxStamina;
        exhausted = false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("stamina", stamina);
        tag.putDouble("maxStamina", maxStamina);
        tag.putBoolean("exhausted", exhausted);
        return tag;
    }

    public void load(CompoundTag tag) {
        stamina = tag.getDouble("stamina");
        maxStamina = tag.getDouble("maxStamina");
        exhausted = tag.getBoolean("exhausted");
        if (maxStamina <= 0) maxStamina = 100.0;
    }
}