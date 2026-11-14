package net.etylop.immersivefarming.block;

import com.google.common.collect.ImmutableSet;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Points of interest, so that mobs can find blocks around them without scanning the area.
 * The level keeps track of them as blocks are placed and broken, and saves them with the chunks.
 */
public class IFPoiTypes {
    public static final DeferredRegister<PoiType> REGISTER =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, ImmersiveFarming.MOD_ID);

    // Both halves of a trough are points of interest: hungry babies go to the nearest half (both share the food)
    public static final RegistryObject<PoiType> FEEDING_TROUGH = REGISTER.register("feeding_trough",
            () -> new PoiType(ImmutableSet.copyOf(IFBlocks.FEEDING_TROUGH.get().getStateDefinition().getPossibleStates()), 0, 1));

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
