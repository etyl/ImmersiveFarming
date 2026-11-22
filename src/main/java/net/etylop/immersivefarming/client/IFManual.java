package net.etylop.immersivefarming.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.config.IFConfig;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/**
 * Exposes config values to the engineer's manual, so that entries can use
 * {@code <config;b;immersivefarming.KEY;text if true;text if false>} or {@code <config;i;immersivefarming.KEY>}.
 * Tags cannot be nested: IE reads a tag up to the first {@code >}, and fails to load the entry (and those after it) otherwise.
 * The manual text is built when resources are (re)loaded. It is rebuilt when joining a world,
 * and when the server sends its own values of our config (see IFConfig#getSynced).
 * Note: IE replaces the tags with String#replaceFirst, so the texts must not contain regex characters such as ( ) [ ] ? * + $
 */
public final class IFManual {
    private static final String PREFIX = ImmersiveFarming.MOD_ID + ".";

    public static void registerConfigGetters() {
        try {
            ManualHelper.addConfigGetter(IFManual::getConfigValue);
            MinecraftForge.EVENT_BUS.<ClientPlayerNetworkEvent.LoggingIn>addListener(e -> reload());
            MinecraftForge.EVENT_BUS.<ClientPlayerNetworkEvent.LoggingOut>addListener(e -> IFConfig.clearSynced());
        } catch (RuntimeException e) {
            ImmersiveFarming.log.error("Could not register config values for the engineer's manual", e);
        }
    }

    public static void reload() {
        try {
            ManualHelper.getManual().reload();
        } catch (RuntimeException e) {
            ImmersiveFarming.log.error("Could not reload the engineer's manual", e);
        }
    }

    private static Object getConfigValue(String key) {
        if (!key.startsWith(PREFIX)) return null;
        IFConfig.CropConfig config = IFConfig.get().cropConfig;
        IFConfig.AnimalConfig animals = IFConfig.get().animals;
        return switch (key.substring(PREFIX.length())) {
            case "babies_need_food" -> IFConfig.getSynced(animals.babies_need_food);
            case "adults_need_food" -> IFConfig.getSynced(animals.adults_need_food);
            case "milking_cooldown" -> IFConfig.getSynced(animals.milking_cooldown);
            // A tag cannot be nested in the text of another one: the cooldown of a sentence only shown when adults need food
            case "milking_cooldown_if_fed" -> IFConfig.getSynced(animals.adults_need_food) ? String.valueOf(IFConfig.getSynced(animals.milking_cooldown)) : "";
            case "breeder_time" -> IFConfig.getSynced(animals.breeder_time);
            case "breeder_food" -> 2 * IFConfig.getSynced(animals.breeder_food);
            case "animal_pen_energy" -> IFConfig.getSynced(animals.animal_pen_energy);
            case "include_variants" -> IFConfig.getSynced(animals.include_variants);
            case "baby_min_meals" -> Math.min(IFConfig.getSynced(animals.min_meals), IFConfig.getSynced(animals.max_meals));
            case "baby_max_meals" -> Math.max(IFConfig.getSynced(animals.min_meals), IFConfig.getSynced(animals.max_meals));
            case "require_sunlight" -> IFConfig.getSynced(config.require_sunlight);
            case "replace_farmland" -> IFConfig.getSynced(config.replace_farmland);
            case "spread_contamination" -> IFConfig.getSynced(config.spread_contamination);
            case "unfertilized_growth_percent" -> (int) Math.round(IFConfig.getSynced(config.unfertilized_growth) * 100);
            // Not a config value: mentions Farmer's Delight only when it is installed
            case "farmersdelight_loaded" -> ModList.get().isLoaded("farmersdelight");
            case "harvests_to_enrich" -> IFConfig.getSynced(config.harvests_to_enrich);
            case "rich_yield_bonus_percent" -> (int) Math.round(IFConfig.getSynced(config.rich_yield_bonus) * 100);
            case "flooded_growth_percent" -> (int) Math.round(IFConfig.getSynced(config.flooded_growth) * 100);
            default -> null;
        };
    }
}
