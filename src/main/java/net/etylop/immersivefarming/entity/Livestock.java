package net.etylop.immersivefarming.entity;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.utils.ModTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The animals whose babies must be fed to grow, and the food they eat.
 * <ul>
 * <li>Animals in the #immersivefarming:needs_food_to_grow entity tag</li>
 * <li>If the config include_variants is true, animals from other mods whose class extends one of them
 * (a modded cow variant extending Cow...), so that they work without any configuration</li>
 * <li>Except animals in the #immersivefarming:grows_without_food entity tag</li>
 * </ul>
 * The food is whatever these animals accept for breeding (Animal#isFood), found by asking a sample of each animal,
 * plus the items in the #immersivefarming:animal_feed tag.
 * <p>Computed on first use, separately for each side, and again when the tags are reloaded or a level is unloaded
 * (samples are created in a level, but never added to it).
 */
public final class Livestock {
    private static final Livestock SERVER = new Livestock();
    private static final Livestock CLIENT = new Livestock();

    private volatile Set<EntityType<?>> types;
    private volatile List<Animal> samples;
    private volatile boolean includeVariants;

    private Livestock() {
    }

    public static boolean isLivestock(Animal animal) {
        return get(animal.level()).getTypes(animal.level()).contains(animal.getType());
    }

    /** Whether some baby animal eats this item */
    public static boolean isFeed(ItemStack stack, @Nullable Level level) {
        if (stack.isEmpty()) return false;
        if (stack.is(ModTags.Items.ANIMAL_FEED)) return true;
        if (level == null) return false;
        Livestock livestock = get(level);
        livestock.getTypes(level);
        List<Animal> samples = livestock.samples;
        if (samples == null) return false;
        for (Animal sample : samples) {
            if (isFood(sample, stack)) return true;
        }
        return false;
    }

    /** Forget everything computed on this side, it is computed again when needed */
    public static void invalidate(boolean clientSide) {
        Livestock livestock = clientSide ? CLIENT : SERVER;
        livestock.types = null;
        livestock.samples = null;
    }

    private static Livestock get(Level level) {
        return level.isClientSide ? CLIENT : SERVER;
    }

    private Set<EntityType<?>> getTypes(Level level) {
        boolean variants = level.isClientSide
                ? IFConfig.getSynced(IFConfig.get().animals.include_variants)
                : IFConfig.get().animals.include_variants.get();
        Set<EntityType<?>> result = this.types;
        if (result == null || variants != this.includeVariants) {
            result = this.compute(level, variants);
        }
        return result;
    }

    private Set<EntityType<?>> compute(Level level, boolean variants) {
        Set<EntityType<?>> types = new HashSet<>();
        List<Animal> samples = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            if (!type.is(ModTags.Entities.NEEDS_FOOD_TO_GROW) || type.is(ModTags.Entities.GROWS_WITHOUT_FOOD)) continue;
            Animal sample = createSample(type, level);
            if (sample == null) continue;
            types.add(type);
            samples.add(sample);
            classes.add(sample.getClass());
        }

        if (variants && !classes.isEmpty()) {
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
                // Farm animals are creatures: no need to create samples of every monster, projectile...
                if (types.contains(type) || type.getCategory() != MobCategory.CREATURE || type.is(ModTags.Entities.GROWS_WITHOUT_FOOD)) continue;
                Animal sample = createSample(type, level);
                if (sample == null || classes.stream().noneMatch(c -> c.isInstance(sample))) continue;
                types.add(type);
                samples.add(sample);
            }
        }

        this.samples = List.copyOf(samples);
        this.includeVariants = variants;
        Set<EntityType<?>> result = Set.copyOf(types);
        this.types = result;
        return result;
    }

    /** An animal of this type, not added to the level, or null if it is not an animal or can't be created */
    @Nullable
    private static Animal createSample(EntityType<?> type, Level level) {
        try {
            Entity entity = type.create(level);
            return entity instanceof Animal animal ? animal : null;
        } catch (RuntimeException | LinkageError e) {
            ImmersiveFarming.log.debug("Could not create a sample of {} to check whether it is livestock", ForgeRegistries.ENTITY_TYPES.getKey(type), e);
            return null;
        }
    }

    private static boolean isFood(Animal sample, ItemStack stack) {
        try {
            return sample.isFood(stack);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
