package net.etylop.immersivefarming.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.config.IFConfig;

/**
 * Exposes config values to the engineer's manual, so that entries can use
 * {@code <config;b;immersivefarming.KEY;text if true;text if false>} or {@code <config;i;immersivefarming.KEY>}.
 * The manual text is built when resources are (re)loaded: after changing the config, press F3+T to update it.
 * Note: IE replaces the tags with String#replaceFirst, so the texts must not contain regex characters such as ( ) [ ] ? * + $
 */
public final class IFManual {
    private static final String PREFIX = ImmersiveFarming.MOD_ID + ".";

    public static void registerConfigGetters() {
        try {
            ManualHelper.addConfigGetter(IFManual::getConfigValue);
        } catch (RuntimeException e) {
            ImmersiveFarming.log.error("Could not register config values for the engineer's manual", e);
        }
    }

    private static Object getConfigValue(String key) {
        if (!key.startsWith(PREFIX)) return null;
        IFConfig.CropConfig config = IFConfig.get().cropConfig;
        return switch (key.substring(PREFIX.length())) {
            case "require_sunlight" -> config.require_sunlight.get();
            case "replace_farmland" -> config.replace_farmland.get();
            case "spread_contamination" -> config.spread_contamination.get();
            case "unfertilized_growth_percent" -> (int) Math.round(config.unfertilized_growth.get() * 100);
            case "flooded_growth_percent" -> (int) Math.round(config.flooded_growth.get() * 100);
            default -> null;
        };
    }
}
