package net.etylop.immersivefarming.item;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.fluid.IFFluids;
import net.etylop.immersivefarming.item.custom.CompostItem;
import net.etylop.immersivefarming.item.custom.WateringCanItem;
import net.etylop.immersivefarming.item.custom.TrowelItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class IFItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ImmersiveFarming.MOD_ID);

    public static final RegistryObject<Item> TREATED_WATER_BUCKET = ITEMS.register("treated_water_bucket",
            () -> new BucketItem(IFFluids.TREATED_WATER_FLUID,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CompostItem> COMPOST = ITEMS.register("compost",
            () -> new CompostItem(new Item.Properties()));

    public static final RegistryObject<WateringCanItem> WATERING_CAN = ITEMS.register("watering_can",
            () -> new WateringCanItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<TrowelItem> TROWEL = ITEMS.register("trowel",
            () -> new TrowelItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WHEEL = ITEMS.register("wheel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLOW = ITEMS.register("plow",
            () -> new CartItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SOWER = ITEMS.register("sower",
            () -> new CartItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
