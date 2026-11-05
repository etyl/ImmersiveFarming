package net.etylop.immersivefarming.config;

import net.jodah.typetools.TypeResolver;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class IFConfig {
    public static Common get() {
        return Holder.COMMON;
    }

    public static ForgeConfigSpec spec() {
        return Holder.COMMON_SPEC;
    }

    private static final class Holder {
        private static final Common COMMON;

        private static final ForgeConfigSpec COMMON_SPEC;

        static {
            final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
            COMMON = specPair.getLeft();
            COMMON_SPEC = specPair.getRight();
        }
    }

    public static class Common {
        public final CartConfig plow;
        public final CropConfig cropConfig;

        Common(final ForgeConfigSpec.Builder builder) {
            this.cropConfig = new CropConfig(builder);
            builder.comment("Configuration for all carts and cart-like vehicles\n\nDefault pull_animals = " + referencePullAnimals()).push("carts");
            this.plow = new CartConfig(builder, "plow", "The Plow, an animal pulled machine for tilling soil");
            builder.pop();
        }

        static String referencePullAnimals() {
            return "[\n" +
                StreamSupport.stream(ForgeRegistries.ENTITY_TYPES.spliterator(), false)
                    .filter(type -> {
                        final Class<?> entityClass = TypeResolver.resolveRawArgument(EntityType.EntityFactory.class, Objects.requireNonNull(
                            ObfuscationReflectionHelper.getPrivateValue(EntityType.class, type, "f_20535_"),
                            "factory"
                        ).getClass());
                        return Saddleable.class.isAssignableFrom(entityClass) &&
                            !ItemSteerable.class.isAssignableFrom(entityClass) &&
                            !Llama.class.isAssignableFrom(entityClass); // no horse-llamas
                    })
                    .map(ForgeRegistries.ENTITY_TYPES::getKey)
                    .filter(Objects::nonNull)
                    .map(type -> "    \"" + type.toString() + "\"")
                    .collect(Collectors.joining(",\n")) +
                "\n  ]";
        }
    }

    public static class CartConfig {
        public final ForgeConfigSpec.ConfigValue<ArrayList<String>> pullAnimals;
        public final ForgeConfigSpec.DoubleValue slowSpeed;
        public final ForgeConfigSpec.DoubleValue pullSpeed;

        CartConfig(final ForgeConfigSpec.Builder builder, final String name, final String description) {
            builder.comment(description).push(name);
            this.pullAnimals = builder
                .comment(
                    "Animals that are able to pull this cart, such as [\"minecraft:horse\"]\n" +
                    "An empty list defaults to all which may wear a saddle but not steered by an item"
                )
                .define("pull_animals", new ArrayList<>());
            this.slowSpeed = builder.comment("Slow speed modifier toggled by the sprint key")
                .defineInRange("slow_speed", -0.0D, -1.0D, 0.0D);
            this.pullSpeed = builder.comment("Base speed modifier applied to animals (-0.5 = half normal speed)")
                .defineInRange("pull_speed", -0.5D, -1.0D, 0.0D);
            builder.pop();
        }
    }

    public static class CropConfig {
        public final ForgeConfigSpec.BooleanValue spread_contamination;
        public final ForgeConfigSpec.DoubleValue start_contamination;
        public final ForgeConfigSpec.DoubleValue proximity_contamination;
        public final ForgeConfigSpec.DoubleValue lethality_contamination;
        public final ForgeConfigSpec.DoubleValue stem_death;
        public final ForgeConfigSpec.DoubleValue evaporation;
        public final ForgeConfigSpec.DoubleValue flooded_growth;
        public final ForgeConfigSpec.DoubleValue unfertilized_growth;
        public final ForgeConfigSpec.BooleanValue require_sunlight;
        public final ForgeConfigSpec.BooleanValue replace_farmland;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> convertible_farmland;

        CropConfig(final ForgeConfigSpec.Builder builder) {
            this.replace_farmland = builder.comment("Whether the blocks listed in convertible_farmland are replaced by soil when a plant grows on them",
                            "(Tilling dirt always gives soil, with any tool able to till)")
                    .define("replace_farmland", true);
            this.convertible_farmland = builder.comment("Blocks converted to soil when a plant grows on them (if replace_farmland is true)",
                            "Block ids such as \"minecraft:farmland\", or block tags starting with #, such as \"#forge:farmland\"",
                            "Converted blocks lose their own behaviour (e.g. the growth boost of Farmer's Delight rich soil)")
                    .defineList("convertible_farmland", List.of("minecraft:farmland"), o -> o instanceof String);
            this.spread_contamination = builder.comment("Whether sick crops can contaminate neighbouring crops")
                    .define("spread_contamination", false);
            this.start_contamination = builder.comment("Probability for a crop to become sick during a random tick")
                    .defineInRange("start_contamination", 0.0001D, 0D, 1.0D);
            this.proximity_contamination = builder.comment("Probability for a crop to be contaminated by a neighbouring sick crop during a random tick")
                    .defineInRange("proximity_contamination", 0.1D, 0D, 1.0D);
            this.lethality_contamination = builder.comment("Probability for a sick crop to die during a random tick")
                    .defineInRange("lethality_contamination", 0.02D, 0D, 1.0D);
            this.stem_death = builder.comment("Probability for a steam to die after giving a fruit")
                    .defineInRange("stem_death", 0.5D, 0D, 1.0D);
            this.evaporation = builder.comment("Probability for soil that is not being watered to lose one water level during a random tick")
                    .defineInRange("evaporation", 0.01D, 0D, 1.0D);
            this.flooded_growth = builder.comment("Probability for a crop on flooded soil (next to a water block) to grow when it would normally grow")
                    .defineInRange("flooded_growth", 0.25D, 0D, 1.0D);
            this.unfertilized_growth = builder.comment("Probability for a crop on unfertilized soil to grow when it would normally grow")
                    .defineInRange("unfertilized_growth", 0.25D, 0D, 1.0D);
            this.require_sunlight = builder.comment("Whether plants on soil need to see the sky to grow (blocks letting sunlight through, such as glass, are fine)",
                            "If false, artificial light is enough, as in vanilla")
                    .define("require_sunlight", true);
        }
    }

}
