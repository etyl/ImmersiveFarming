package net.etylop.immersivefarming.common.data.generators;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.api.crafting.builders.ComposterRecipeBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


public class IFRecipes extends RecipeProvider{
    private final Map<String, Integer> PATH_COUNT = new HashMap<>();

    protected Consumer<FinishedRecipe> out;
    public IFRecipes(PackOutput output){
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> out){
        this.out = out;

        composterRecipes(out);
    }

    private void composterRecipes(Consumer<FinishedRecipe> out) {
        ComposterRecipeBuilder.builder("minecraft:logs", 0, 50).build(out, toRL("composter/logs"));
        ComposterRecipeBuilder.builder("minecraft:saplings", 0, 20).build(out, toRL("composter/saplings"));
        ComposterRecipeBuilder.builder("minecraft:leaves", 0, 10).build(out, toRL("composter/leaves"));

        ComposterRecipeBuilder.builder("forge:seeds", 10, 0).build(out, toRL("composter/seed"));
        ComposterRecipeBuilder.builder("forge:crops", 10, 0).build(out, toRL("composter/crops"));
        ComposterRecipeBuilder.builder("forge:fruits", 10, 0).build(out, toRL("composter/fruits"));
        ComposterRecipeBuilder.builder("forge:vegetables", 10, 0).build(out, toRL("composter/vegetables"));
        ComposterRecipeBuilder.builder("forge:grain", 10, 0).build(out, toRL("composter/grain"));

        ComposterRecipeBuilder.builder(Items.MELON, 20, 0).build(out, toRL("composter/melon"));
        ComposterRecipeBuilder.builder(Items.MELON_SLICE, 2, 0).build(out, toRL("composter/melon_slice"));
        ComposterRecipeBuilder.builder(Items.PUMPKIN, 20, 0).build(out, toRL("composter/pumpkin"));
        ComposterRecipeBuilder.builder(Items.APPLE, 10, 0).build(out, toRL("composter/apple"));
        ComposterRecipeBuilder.builder(Items.SUGAR_CANE, 10, 0).build(out, toRL("composter/sugar_cane"));
    }

    private ResourceLocation toRL(String loc) {
        return new ResourceLocation(ImmersiveFarming.MOD_ID,"crafting/"+loc);
    }
}