package net.etylop.immersivefarming.loot;

import com.mojang.serialization.Codec;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class IFLootModifiers {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ImmersiveFarming.MOD_ID);

    public static final RegistryObject<Codec<RichSoilYieldModifier>> RICH_SOIL_YIELD =
            REGISTER.register("rich_soil_yield", RichSoilYieldModifier.CODEC);

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
