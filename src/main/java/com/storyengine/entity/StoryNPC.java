package com.storyengine.entity;

import com.storyengine.StoryEngineMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class StoryNPC extends PathfinderMob {

    private static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(StoryNPC.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_DISPLAY_NAME =
            SynchedEntityData.defineId(StoryNPC.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_NAME =
            SynchedEntityData.defineId(StoryNPC.class, EntityDataSerializers.STRING);

    private String dialogueId = "";
    private String onInteractAction = "";
    private Vec3 targetMovePos = null;
    private float moveSpeed = 1.0f;
    private boolean lookAtPlayer = true;
    private boolean isStationary = true;

    public StoryNPC(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_ID, "");
        this.entityData.define(NPC_DISPLAY_NAME, "NPC");
        this.entityData.define(SKIN_NAME, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new FloatGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide
                && player instanceof ServerPlayer serverPlayer
                && hand == InteractionHand.MAIN_HAND) {

            // Диалог
            if (dialogueId != null && !dialogueId.isEmpty()) {
                StoryEngineMod.getInstance().getDialogueManager()
                        .startDialogue(serverPlayer, dialogueId);
                return InteractionResult.SUCCESS;
            }

            // Кастомное действие
            if (onInteractAction != null && !onInteractAction.isEmpty()) {
                serverPlayer.sendSystemMessage(
                        Component.literal("§7[" + getNPCDisplayName() + "] §fВзаимодействие"));
                return InteractionResult.SUCCESS;
            }

            // По умолчанию
            serverPlayer.sendSystemMessage(
                    Component.literal("§e[" + getNPCDisplayName() + "] §fПривет!"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // Перемещение к цели
        if (targetMovePos != null) {
            this.getNavigation().moveTo(
                    targetMovePos.x, targetMovePos.y, targetMovePos.z, moveSpeed);
            if (this.distanceToSqr(targetMovePos) < 2.0) {
                targetMovePos = null;
                this.getNavigation().stop();
                this.isStationary = true;
            }
        }

        // Стационарный NPC не бродит
        if (isStationary && targetMovePos == null) {
            this.getNavigation().stop();
        }

        // Имя над головой
        String displayName = getNPCDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            this.setCustomName(Component.literal("§e" + displayName));
            this.setCustomNameVisible(true);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        // NPC не толкает
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    // === Геттеры / Сеттеры ===

    public void setNPCId(String id) {
        this.entityData.set(NPC_ID, id != null ? id : "");
    }

    public String getNPCId() {
        return this.entityData.get(NPC_ID);
    }

    public void setNPCDisplayName(String name) {
        this.entityData.set(NPC_DISPLAY_NAME, name != null ? name : "NPC");
    }

    public String getNPCDisplayName() {
        return this.entityData.get(NPC_DISPLAY_NAME);
    }

    public void setSkinName(String skin) {
        this.entityData.set(SKIN_NAME, skin != null ? skin : "");
    }

    public String getSkinName() {
        return this.entityData.get(SKIN_NAME);
    }

    public void setDialogueId(String dialogueId) {
        this.dialogueId = dialogueId != null ? dialogueId : "";
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public void setOnInteractAction(String action) {
        this.onInteractAction = action != null ? action : "";
    }

    public String getOnInteractAction() {
        return onInteractAction;
    }

    public void moveToPosition(double x, double y, double z, float speed) {
        this.targetMovePos = new Vec3(x, y, z);
        this.moveSpeed = speed;
        this.isStationary = false;
    }

    public void setLookAtPlayer(boolean look) {
        this.lookAtPlayer = look;
    }

    public boolean isLookAtPlayer() {
        return lookAtPlayer;
    }

    public void setStationary(boolean stationary) {
        this.isStationary = stationary;
    }

    public boolean isStationary() {
        return isStationary;
    }

    // === Сохранение / Загрузка ===

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NPCId", getNPCId());
        tag.putString("NPCDisplayName", getNPCDisplayName());
        tag.putString("SkinName", getSkinName());
        tag.putString("DialogueId", dialogueId != null ? dialogueId : "");
        tag.putString("OnInteractAction", onInteractAction != null ? onInteractAction : "");
        tag.putBoolean("LookAtPlayer", lookAtPlayer);
        tag.putBoolean("Stationary", isStationary);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NPCId")) setNPCId(tag.getString("NPCId"));
        if (tag.contains("NPCDisplayName")) setNPCDisplayName(tag.getString("NPCDisplayName"));
        if (tag.contains("SkinName")) setSkinName(tag.getString("SkinName"));
        if (tag.contains("DialogueId")) dialogueId = tag.getString("DialogueId");
        if (tag.contains("OnInteractAction")) onInteractAction = tag.getString("OnInteractAction");
        if (tag.contains("LookAtPlayer")) lookAtPlayer = tag.getBoolean("LookAtPlayer");
        if (tag.contains("Stationary")) isStationary = tag.getBoolean("Stationary");
    }
}