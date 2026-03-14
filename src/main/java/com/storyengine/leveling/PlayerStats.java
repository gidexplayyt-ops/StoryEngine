package com.storyengine.leveling;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

public class PlayerStats {
    private int level;
    private long xp;
    private long xpToNextLevel;
    private int availablePoints;
    private final Map<Skill, Integer> skills;

    public PlayerStats() {
        this.level = 1;
        this.xp = 0;
        this.xpToNextLevel = 100;
        this.availablePoints = 0;
        this.skills = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) skills.put(skill, 0);
    }

    public int getLevel() { return level; }
    public long getXp() { return xp; }
    public long getXpToNextLevel() { return xpToNextLevel; }
    public int getAvailablePoints() { return availablePoints; }
    public int getSkillLevel(Skill skill) { return skills.getOrDefault(skill, 0); }
    public double getXpPercentage() { return xpToNextLevel > 0 ? (double) xp / xpToNextLevel : 0; }

    public boolean addXp(long amount, int maxLevel, int baseXp,
                         double multiplier, int pointsPerLevel) {
        xp += amount;
        boolean leveled = false;

        while (xp >= xpToNextLevel && level < maxLevel) {
            xp -= xpToNextLevel;
            level++;
            availablePoints += pointsPerLevel;
            xpToNextLevel = (long) (baseXp * Math.pow(multiplier, level - 1));
            leveled = true;
        }

        if (level >= maxLevel) xp = Math.min(xp, xpToNextLevel);
        return leveled;
    }

    public boolean upgradeSkill(Skill skill) {
        if (availablePoints <= 0) return false;
        skills.put(skill, skills.getOrDefault(skill, 0) + 1);
        availablePoints--;
        return true;
    }

    public double getStrengthBonus() { return getSkillLevel(Skill.STRENGTH) * 0.5; }
    public double getHealthBonus() { return getSkillLevel(Skill.VITALITY) * 2.0; }
    public double getSpeedBonus() { return getSkillLevel(Skill.AGILITY) * 0.01; }
    public double getStaminaBonus() { return getSkillLevel(Skill.ENDURANCE) * 5.0; }
    public double getLuckBonus() { return getSkillLevel(Skill.LUCK) * 1.0; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putLong("xpToNext", xpToNextLevel);
        tag.putInt("points", availablePoints);
        for (Skill skill : Skill.values())
            tag.putInt("skill_" + skill.name(), skills.getOrDefault(skill, 0));
        return tag;
    }

    public void load(CompoundTag tag) {
        level = tag.getInt("level");
        if (level <= 0) level = 1;
        xp = tag.getLong("xp");
        xpToNextLevel = tag.getLong("xpToNext");
        if (xpToNextLevel <= 0) xpToNextLevel = 100;
        availablePoints = tag.getInt("points");
        for (Skill skill : Skill.values())
            skills.put(skill, tag.getInt("skill_" + skill.name()));
    }
}