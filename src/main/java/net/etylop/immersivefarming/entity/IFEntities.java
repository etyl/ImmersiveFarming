package net.etylop.immersivefarming.entity;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class IFEntities {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ImmersiveFarming.MOD_ID);

    public static final RegistryObject<EntityType<PlowEntity>> PLOW = ENTITIES.register("plow",
            () -> EntityType.Builder.of(PlowEntity::new, MobCategory.MISC)
                .sized(1.3F, 1.4F)
                .build(ImmersiveFarming.MOD_ID + ":plow"));

    public static final RegistryObject<EntityType<SowerEntity>> SOWER = ENTITIES.register("sower",
            () -> EntityType.Builder.of(SowerEntity::new, MobCategory.MISC)
                    .sized(1.3F, 1.4F)
                    .build(ImmersiveFarming.MOD_ID + ":sower"));



    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
