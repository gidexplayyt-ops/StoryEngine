package com.storyengine.entity;

import com.storyengine.StoryEngineMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NPCRenderer extends MobRenderer<StoryNPC, PlayerModel<StoryNPC>> {

    private static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/entity/npc_default.png");

    public NPCRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(StoryNPC entity) {
        String skin = entity.getSkinName();
        if (skin != null && !skin.isEmpty()) {
            return new ResourceLocation(StoryEngineMod.MOD_ID,
                    "textures/entity/npc/" + skin + ".png");
        }
        return DEFAULT_TEXTURE;
    }
}