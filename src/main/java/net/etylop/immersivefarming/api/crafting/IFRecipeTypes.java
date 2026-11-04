package net.etylop.immersivefarming.api.crafting;

import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class IFRecipeTypes{
    private static final DeferredRegister<RecipeType<?>> REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, ImmersiveFarming.MOD_ID);

    public static final TypeWithClass<ComposterRecipe> COMPOSTER = makeType("composter", ComposterRecipe.class);


    public static void register(IEventBus eventBus){
        REGISTER.register(eventBus);
    }

    private static <T extends Recipe<?>> TypeWithClass<T> makeType(String name, Class<T> type){
        RegistryObject<RecipeType<T>> regObj = REGISTER.register(name, () -> new RecipeType<T>(){
            @Override
            public String toString(){
                return ImmersiveFarming.MOD_ID + ":" + name;
            }
        });
        return new TypeWithClass<>(regObj, type);
    }
}