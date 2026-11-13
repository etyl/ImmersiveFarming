package net.etylop.immersivefarming.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.common.config.IEServerConfig;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.config.IFConfig;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/**
 * Exposes config values to the engineer's manual, so that entries can use
 * {@code <config;b;immersivefarming.KEY;text if true;text if false>} or {@code <config;i;immersivefarming.KEY>}.
 * The manual text is built when resources are (re)loaded. It is rebuilt when joining a world, once IE's server config
 * has been synced, and when the server sends its own values of our config (see IFConfig#getSynced).
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
        IFConfig.ClocheConfig cloche = IFConfig.get().cloche;
        return switch (key.substring(PREFIX.length())) {
            case "require_sunlight" -> IFConfig.getSynced(config.require_sunlight);
            case "replace_farmland" -> IFConfig.getSynced(config.replace_farmland);
            case "spread_contamination" -> IFConfig.getSynced(config.spread_contamination);
            case "unfertilized_growth_percent" -> (int) Math.round(IFConfig.getSynced(config.unfertilized_growth) * 100);
            // Not a config value: mentions Farmer's Delight only when it is installed
            case "farmersdelight_loaded" -> ModList.get().isLoaded("farmersdelight");
            case "harvests_to_enrich" -> IFConfig.getSynced(config.harvests_to_enrich);
            case "rich_yield_bonus_percent" -> (int) Math.round(IFConfig.getSynced(config.rich_yield_bonus) * 100);
            case "flooded_growth_percent" -> (int) Math.round(IFConfig.getSynced(config.flooded_growth) * 100);
            case "cloche_require_fertilizer" -> IFConfig.getSynced(cloche.require_fertilizer);
            // Actual values, combining IE's server config (synced to the client) with ours
            case "cloche_energy" -> (int) Math.round(ieConfig(IEServerConfig.MACHINES.cloche_consumption, 8) * IFConfig.getSynced(cloche.energy_multiplier));
            case "cloche_dose_seconds" -> ieConfig(IEServerConfig.MACHINES.cloche_fertilizer, 6000) / IFConfig.getSynced(cloche.fertilizer_consumption) / 20;
            default -> null;
        };
    }

    private static int ieConfig(ForgeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) { // server config not loaded yet
            return fallback;
        }
    }
}
