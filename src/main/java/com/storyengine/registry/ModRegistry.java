package com.storyengine.registry;

import com.storyengine.StoryEngineMod;
import com.storyengine.entity.NPCRenderer;
import com.storyengine.entity.StoryNPC;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StoryEngineMod.MOD_ID);

    public static final RegistryObject<EntityType<StoryNPC>> STORY_NPC =
            ENTITIES.register("story_npc", () ->
                    EntityType.Builder.of(StoryNPC::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(64)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("story_npc"));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(ModRegistry::onAttributeCreate);
        StoryEngineMod.LOGGER.info("ModRegistry: entities registered");
    }

    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(STORY_NPC.get(), StoryNPC.createAttributes().build());
        StoryEngineMod.LOGGER.info("ModRegistry: NPC attributes registered");
    }

    public static void registerRenderers() {
        EntityRenderers.register(STORY_NPC.get(), NPCRenderer::new);
        StoryEngineMod.LOGGER.info("ModRegistry: renderers registered");
    }
}