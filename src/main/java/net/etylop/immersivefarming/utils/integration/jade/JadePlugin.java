package net.etylop.immersivefarming.utils.integration.jade;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.entity.AnimalFeeding;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * Jade integration, only loaded when Jade is installed.
 * Babies that need food show whether they are hungry, and how many meals they still need, next to Jade's own growth time
 * (which stops counting down while a baby is hungry). Adults that need food to produce show whether they are hungry,
 * and milkable ones how long before they can be milked again.
 * The feeding state is only known by the server, hence the server data.
 */
@WailaPlugin(ImmersiveFarming.MOD_ID)
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(BabyFeedingProvider.INSTANCE, Animal.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(BabyFeedingProvider.INSTANCE, Animal.class);
    }

    public enum BabyFeedingProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = new ResourceLocation(ImmersiveFarming.MOD_ID, "baby_feeding");
        private static final String HUNGRY = "IFHungry";
        private static final String MEALS_LEFT = "IFMealsLeft";
        private static final String MILKING_COOLDOWN = "IFMilkingCooldown";

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof Animal animal)) return;
            if (AnimalFeeding.needsFood(animal)) {
                data.putBoolean(HUNGRY, AnimalFeeding.isHungry(animal));
                data.putInt(MEALS_LEFT, AnimalFeeding.getMealsLeft(animal));
            } else if (AnimalFeeding.isProducer(animal)) {
                data.putBoolean(HUNGRY, AnimalFeeding.isHungry(animal));
                data.putInt(MILKING_COOLDOWN, AnimalFeeding.getMilkingCooldown(animal));
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(HUNGRY)) return;
            IThemeHelper theme = IThemeHelper.get();
            tooltip.add(data.getBoolean(HUNGRY)
                    ? theme.warning(Component.translatable("jade.immersivefarming.baby.hungry"))
                    : theme.success(Component.translatable("jade.immersivefarming.baby.fed")));
            int mealsLeft = data.getInt(MEALS_LEFT);
            if (mealsLeft > 0) {
                tooltip.add(Component.translatable("jade.immersivefarming.baby.meals_left", mealsLeft));
            }
            // Shown as Jade shows the next egg of a chicken
            int cooldown = data.getInt(MILKING_COOLDOWN);
            if (cooldown > 0) {
                tooltip.add(Component.translatable("jade.immersivefarming.animal.next_milk", theme.seconds(cooldown)));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
