package net.etylop.immersivefarming.block.utils;

import blusunrize.immersiveengineering.api.ApiUtils;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

public class IFDynamicModel {
    private static final List<ResourceLocation> MODELS = new ArrayList<>();

    private final ResourceLocation name;

    public IFDynamicModel(String desc)
    {
        this.name = new ResourceLocation(ImmersiveFarming.MOD_ID, "dynamic/"+desc);
        MODELS.add(this.name);
    }

    public static void registerModels(ModelEvent.RegisterAdditional ev)
    {
        for(ResourceLocation model : MODELS)
            ev.register(model);
    }

    public BakedModel get()
    {
        final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        return blockRenderer.getBlockModelShaper().getModelManager().getModel(name);
    }

    public List<BakedQuad> getNullQuads()
    {
        return getNullQuads(ModelData.EMPTY);
    }

    public List<BakedQuad> getNullQuads(ModelData data)
    {
        return get().getQuads(null, null, ApiUtils.RANDOM_SOURCE, data, null);
    }

    public ResourceLocation getName()
    {
        return name;
    }
}
