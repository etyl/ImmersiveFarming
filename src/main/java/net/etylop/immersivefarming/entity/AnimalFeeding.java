package net.etylop.immersivefarming.entity;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.config.IFConfig;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Baby animals (see {@link Livestock}) only grow while they are fed.
 * <p>When a baby is first seen growing, it is given a random number of meals (config min_meals to max_meals),
 * which splits its remaining growth time into equal portions. Each meal lets it grow for one portion,
 * after which it is hungry and stops growing until it eats again (by hand, or from a feeding trough).
 * The growth itself is gated in {@link net.etylop.immersivefarming.mixin.AgeableMobMixin}.
 * <p>Adults that produce something (see {@link #isProducer}) must eat before each product: cows, goats and mooshrooms
 * before each milking (or bowl of stew), chickens before each egg. They are hungry after producing, until they eat.
 * Milk also takes time to come back (config milking_cooldown, from the last milking), as eggs do (vanilla egg timer).
 * Sheep are hungry while sheared: eating makes their wool grow back (eating grass still does too).
 * <p>The state is kept in the entity's persistent data, so it survives saving and needs no capability.
 */
public final class AnimalFeeding {
    private static final String TAG = ImmersiveFarming.MOD_ID + ":feeding";
    /** Growth ticks given by one meal */
    private static final String PORTION = "portion";
    /** Growth ticks left before the baby is hungry again */
    private static final String SATIETY = "satiety";
    /** Set on an adult once it has produced, until it eats. Absent (adults already in the world...) means fed */
    private static final String ADULT_HUNGRY = ImmersiveFarming.MOD_ID + ":hungry";
    /** Game time of an adult's last milking */
    private static final String LAST_MILKING = ImmersiveFarming.MOD_ID + ":last_milking";

    private AnimalFeeding() {
    }

    /** Whether this animal's babies must be fed to grow (regardless of its current age) */
    public static boolean isConcerned(Animal animal) {
        return config(IFConfig.get().animals.babies_need_food, animal) && Livestock.isLivestock(animal);
    }

    /** Whether this animal is a baby that needs to be fed to grow */
    public static boolean needsFood(Animal animal) {
        return animal.isBaby() && isConcerned(animal);
    }

    /** Whether this animal is an adult that must eat before producing (milk, eggs, wool) */
    public static boolean isProducer(Animal animal) {
        if (animal.isBaby() || !config(IFConfig.get().animals.adults_need_food, animal)) return false;
        if (config(IFConfig.get().animals.include_variants, animal)) {
            return animal instanceof Chicken || animal instanceof Cow || animal instanceof Goat || animal instanceof Sheep;
        }
        EntityType<?> type = animal.getType();
        return type == EntityType.CHICKEN || type == EntityType.COW || type == EntityType.MOOSHROOM
                || type == EntityType.GOAT || type == EntityType.SHEEP;
    }

    /**
     * Whether this animal would eat now: a baby that needs food to grow, or an adult that needs food to produce.
     * Server side only: the client doesn't know the feeding state.
     */
    public static boolean isHungry(Animal animal) {
        if (animal.isBaby()) return needsFood(animal) && getData(animal).getInt(SATIETY) <= 0;
        if (!isProducer(animal)) return false;
        if (animal instanceof Sheep sheep) return sheep.isSheared();
        return animal.getPersistentData().getBoolean(ADULT_HUNGRY);
    }

    /** Called when a fed adult produces (milked, lays an egg): it is hungry until it eats again (server side) */
    public static void produce(Animal animal) {
        if (!isProducer(animal) || animal instanceof Sheep) return;
        animal.getPersistentData().putBoolean(ADULT_HUNGRY, true);
        if (isMilkable(animal)) animal.getPersistentData().putLong(LAST_MILKING, animal.level().getGameTime());
    }

    /** Cows, goats and mooshrooms (a Cow too), and their variants */
    public static boolean isMilkable(Animal animal) {
        return animal instanceof Cow || animal instanceof Goat;
    }

    /** Ticks left before this adult can be milked again, regardless of its hunger (server side) */
    public static int getMilkingCooldown(Animal animal) {
        if (!isProducer(animal) || !isMilkable(animal)) return 0;
        CompoundTag persistent = animal.getPersistentData();
        if (!persistent.contains(LAST_MILKING)) return 0;
        long elapsed = animal.level().getGameTime() - persistent.getLong(LAST_MILKING);
        long cooldown = IFConfig.get().animals.milking_cooldown.get() * 20L;
        return elapsed < 0 ? 0 : (int) Math.max(0, cooldown - elapsed);
    }

    /**
     * Called each tick a baby is about to grow by one tick (server side)
     * @return whether it may grow
     */
    public static boolean tryGrow(AgeableMob mob) {
        if (!(mob instanceof Animal animal) || !isConcerned(animal)) return true;
        CompoundTag data = getData(animal);
        int satiety = data.getInt(SATIETY);
        if (satiety <= 0) return false;
        data.putInt(SATIETY, satiety - 1);
        // Last growth tick: the animal becomes an adult, its feeding state is no longer needed
        if (animal.getAge() >= -1) animal.getPersistentData().remove(TAG);
        return true;
    }

    /** Feed a hungry animal one meal (server side), the item is not consumed here */
    public static void feed(Animal animal, ItemStack food) {
        if (animal.isBaby()) {
            CompoundTag data = getData(animal);
            data.putInt(SATIETY, data.getInt(SATIETY) + data.getInt(PORTION));
        } else if (animal instanceof Sheep sheep) {
            // As when eating grass: the wool grows back
            sheep.ate();
        } else {
            animal.getPersistentData().remove(ADULT_HUNGRY);
        }

        animal.playSound(animal.getEatingSound(food), 1.0F, 1.0F);
        if (animal.level() instanceof ServerLevel level) {
            double y = animal.getY() + animal.getBbHeight() * 0.6;
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.copyWithCount(1)),
                    animal.getX(), y, animal.getZ(), 8, 0.15, 0.1, 0.15, 0.05);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, animal.getX(), y, animal.getZ(), 4, 0.3, 0.3, 0.3, 0);
        }
    }

    /** Meals this baby still needs to grow up, for the growth time not covered by its last meal (server side) */
    public static int getMealsLeft(Animal animal) {
        CompoundTag data = getData(animal);
        int portion = Math.max(1, data.getInt(PORTION));
        int growthLeft = Math.max(0, -animal.getAge() - data.getInt(SATIETY));
        return (growthLeft + portion - 1) / portion;
    }

    /** The feeding state of a baby, created the first time it is needed */
    private static CompoundTag getData(Animal animal) {
        CompoundTag persistent = animal.getPersistentData();
        if (persistent.contains(TAG, CompoundTag.TAG_COMPOUND)) return persistent.getCompound(TAG);

        int min = IFConfig.get().animals.min_meals.get();
        int max = IFConfig.get().animals.max_meals.get();
        int meals = Math.min(min, max) + animal.getRandom().nextInt(Math.abs(max - min) + 1);
        int growth = Math.max(1, -animal.getAge());
        CompoundTag data = new CompoundTag();
        data.putInt(PORTION, (growth + meals - 1) / meals);
        data.putInt(SATIETY, 0);
        persistent.put(TAG, data);
        return data;
    }

    /** Config values are read on both sides: the client uses the server's values */
    private static boolean config(ForgeConfigSpec.BooleanValue value, Animal animal) {
        return animal.level().isClientSide ? IFConfig.getSynced(value) : value.get();
    }
}
