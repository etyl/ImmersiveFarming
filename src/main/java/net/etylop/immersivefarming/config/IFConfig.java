package net.etylop.immersivefarming.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.etylop.immersivefarming.block.custom.Soil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Common config (config/immersivefarming-common.toml), shared by all the worlds of a game instance.
 * Gameplay code runs on the server and reads the values directly. Code showing values on the client (engineer's manual, JEI)
 * must use {@link #getSynced}: when playing on a dedicated server, the server sends its values when the player joins
 * ({@link net.etylop.immersivefarming.network.clientbound.SyncConfigMessage}), and they replace the client's own values.
 */
public final class IFConfig {
    public static Common get() {
        return Holder.COMMON;
    }

    public static ForgeConfigSpec spec() {
        return Holder.COMMON_SPEC;
    }

    public static boolean isLoaded() {
        return Holder.COMMON_SPEC.isLoaded();
    }

    /* Values received from the server, by config path ("cloche.energy_multiplier"), empty when not connected to a server */
    private static volatile Map<String, Object> synced = Map.of();

    /** The value to show on the client: the server's value if it sent one, else the local value (or its default if not loaded yet) */
    @SuppressWarnings("unchecked")
    public static <T> T getSynced(ForgeConfigSpec.ConfigValue<T> value) {
        Object syncedValue = synced.get(String.join(".", value.getPath()));
        if (syncedValue != null) {
            try {
                return (T) syncedValue;
            } catch (ClassCastException ignored) {
            }
        }
        return isLoaded() ? value.get() : value.getDefault();
    }

    public static void setSynced(Map<String, Object> values) {
        synced = Map.copyOf(values);
    }

    public static void clearSynced() {
        synced = Map.of();
    }

    /** All the config values, by path, to be sent to the clients */
    public static Map<String, Object> snapshot() {
        Map<String, Object> values = new LinkedHashMap<>();
        collect(spec().getValues(), values);
        return values;
    }

    private static void collect(UnmodifiableConfig config, Map<String, Object> values) {
        for (Object entry : config.valueMap().values()) {
            if (entry instanceof UnmodifiableConfig child) collect(child, values);
            else if (entry instanceof ForgeConfigSpec.ConfigValue<?> value) values.put(String.join(".", value.getPath()), value.get());
        }
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
        public final ClocheConfig cloche;
        public final AnimalConfig animals;

        Common(final ForgeConfigSpec.Builder builder) {
            this.cropConfig = new CropConfig(builder);
            this.cloche = new ClocheConfig(builder);
            this.animals = new AnimalConfig(builder);
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

    public static class ClocheConfig {
        public final ForgeConfigSpec.BooleanValue require_fertilizer;
        public final ForgeConfigSpec.DoubleValue energy_multiplier;
        public final ForgeConfigSpec.IntValue fertilizer_consumption;
        public final ForgeConfigSpec.DoubleValue growth_time_multiplier;

        ClocheConfig(final ForgeConfigSpec.Builder builder) {
            builder.comment("Immersive Engineering garden cloche").push("cloche");
            this.require_fertilizer = builder.comment("Whether the cloche needs a fertilizer to work (Industrial Fertilizer, see the immersiveengineering:fertilizer recipes)",
                            "If false, the cloche runs on water alone and fertilizers only speed it up, as in Immersive Engineering")
                    .define("require_fertilizer", true);
            this.energy_multiplier = builder.comment("Multiplier applied to the cloche energy consumption (Immersive Engineering server config, 8 FE/t by default)",
                            "The cloche can't receive more than 256 FE/t (or the IE consumption value if higher)")
                    .defineInRange("energy_multiplier", 4.0D, 0.0D, 32.0D);
            this.fertilizer_consumption = builder.comment("Multiplier applied to the speed at which the cloche uses up its fertilizer",
                            "(2 = a dose lasts half the Immersive Engineering server config duration)")
                    .defineInRange("fertilizer_consumption", 1, 1, 100);
            this.growth_time_multiplier = builder.comment("Multiplier applied to the time a plant takes to grow in the cloche (Immersive Engineering recipe time)",
                            "(4 = plants take four times as long to grow, the fertilizer consumption per minute is unchanged)")
                    .defineInRange("growth_time_multiplier", 4.0D, 0.01D, 100.0D);
            builder.pop();
        }
    }

    public static class AnimalConfig {
        public final ForgeConfigSpec.BooleanValue babies_need_food;
        public final ForgeConfigSpec.IntValue min_meals;
        public final ForgeConfigSpec.IntValue max_meals;
        public final ForgeConfigSpec.BooleanValue include_variants;
        public final ForgeConfigSpec.BooleanValue adults_need_food;
        public final ForgeConfigSpec.IntValue milking_cooldown;
        public final ForgeConfigSpec.IntValue breeder_time;
        public final ForgeConfigSpec.IntValue breeder_food;
        public final ForgeConfigSpec.IntValue animal_pen_energy;

        AnimalConfig(final ForgeConfigSpec.Builder builder) {
            builder.comment("Baby animals (those in the #immersivefarming:needs_food_to_grow entity tag), and adults producing milk, eggs and wool").push("animals");
            this.include_variants = builder.comment("Whether animals from other mods that are variants of these animals also need to be fed (their entity class extends one of them, such as a modded cow)",
                            "Animals in the #immersivefarming:grows_without_food entity tag never need to be fed as babies")
                    .define("include_variants", true);
            this.babies_need_food = builder.comment("Whether baby animals need to be fed to grow up",
                            "Each meal lets a baby grow for a while, it then stops growing until it eats again",
                            "Babies eat their breeding food, by hand or from a feeding trough",
                            "If false, babies grow on their own and feeding them speeds up their growth, as in vanilla")
                    .define("babies_need_food", true);
            this.min_meals = builder.comment("Minimum number of meals a baby needs to grow up (each baby needs a random number between min_meals and max_meals)")
                    .defineInRange("min_meals", 4, 1, 64);
            this.max_meals = builder.comment("Maximum number of meals a baby needs to grow up")
                    .defineInRange("max_meals", 8, 1, 64);
            this.adults_need_food = builder.comment("Whether adult animals need to be fed to produce",
                            "Cows, goats and mooshrooms must eat before each milking (or bowl of stew), chickens before each egg, and sheared sheep before their wool grows back (eating grass still works too)",
                            "They eat their breeding food, by hand or from a feeding trough")
                    .define("adults_need_food", true);
            this.milking_cooldown = builder.comment("When adults need food to produce: seconds before an animal can be milked again (or give another bowl of stew), it must also eat in between",
                            "Chickens lay an egg every 5 to 10 minutes, as in vanilla")
                    .defineInRange("milking_cooldown", 300, 0, 3600);
            this.breeder_time = builder.comment("Animal breeder: seconds between the meal of the two animals and the birth",
                            "The parents then rest before they can breed again, as after any breeding (5 minutes for vanilla animals)")
                    .defineInRange("breeder_time", 30, 1, 3600);
            this.breeder_food = builder.comment("Animal breeder: food items each of the two animals eats from the trough to breed")
                    .defineInRange("breeder_food", 1, 1, 32);
            this.animal_pen_energy = builder.comment("Animal pen: energy (FE) used to slaughter a grown animal, the only thing the machine uses energy for",
                            "(it stores up to 32000 FE)")
                    .defineInRange("animal_pen_energy", 8000, 0, 32000);
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
        public final ForgeConfigSpec.IntValue harvests_to_enrich;
        public final ForgeConfigSpec.DoubleValue rich_yield_bonus;

        CropConfig(final ForgeConfigSpec.Builder builder) {
            this.replace_farmland = builder.comment("Whether the blocks listed in convertible_farmland are replaced by soil when a plant grows on them",
                            "(Tilling dirt always gives soil, with any tool able to till)")
                    .define("replace_farmland", true);
            this.convertible_farmland = builder.comment("Blocks converted to soil when a plant grows on them (if replace_farmland is true)",
                            "Block ids such as \"minecraft:farmland\", or block tags starting with #, such as \"#forge:farmland\"",
                            "Converted blocks lose their own behaviour",
                            "(Blocks in the #immersivefarming:rich_farmland tag, such as Farmer's Delight rich soil farmland, are always converted, to rich soil)")
                    .defineList("convertible_farmland", List.of("minecraft:farmland"), o -> o instanceof String);
            this.harvests_to_enrich = builder.comment("Number of crops grown to maturity on soil for it to become rich",
                            "(0 = soil never becomes rich naturally, only by tilling Farmer's Delight rich soil)")
                    .defineInRange("harvests_to_enrich", 8, 0, Soil.RICHNESS_MAX);
            this.rich_yield_bonus = builder.comment("Extra loot from mature crops harvested on rich soil, as a fraction of the normal loot",
                            "(0.3 = +30% items on average, seeds included)")
                    .defineInRange("rich_yield_bonus", 0.3D, 0D, 1.0D);
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
                    .defineInRange("flooded_growth", 0.2D, 0D, 1.0D);
            this.unfertilized_growth = builder.comment("Probability for a crop on unfertilized soil to grow when it would normally grow")
                    .defineInRange("unfertilized_growth", 0.2D, 0D, 1.0D);
            this.require_sunlight = builder.comment("Whether plants on soil need to see the sky to grow (blocks letting sunlight through, such as glass, are fine)",
                            "If false, artificial light is enough, as in vanilla")
                    .define("require_sunlight", true);
        }
    }

}
