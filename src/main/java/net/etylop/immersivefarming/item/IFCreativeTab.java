package net.etylop.immersivefarming.item;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class IFCreativeTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmersiveFarming.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TAB = REGISTER.register("immersivefarmingtab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.immersivefarmingtab"))
                    .icon(() -> new ItemStack(IFBlocks.SPRINKLER.get()))
                    .displayItems((params, output) -> {
                        // Multiblock items are only obtained by forming the multiblock
                        Set<Item> hidden = Set.of(
                                IFMultiblockLogic.COMPOSTER.blockItem().get(),
                                IFMultiblockLogic.BREEDER.blockItem().get(),
                                IFMultiblockLogic.ANIMAL_PEN.blockItem().get());
                        for (RegistryObject<Item> item : IFItems.ITEMS.getEntries()) {
                            if (!hidden.contains(item.get()))
                                output.accept(item.get());
                        }
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
