package net.etylop.immersivefarming.gui;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import blusunrize.immersiveengineering.common.register.IEMenuTypes.ArgContainerConstructor;
import blusunrize.immersiveengineering.common.register.IEMenuTypes.ClientContainerConstructor;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterLogic;
import net.etylop.immersivefarming.gui.container.ComposterContainer;
import net.etylop.immersivefarming.gui.container.PlowContainer;
import net.etylop.immersivefarming.gui.container.SowerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import javax.annotation.Nonnull;

public class IFMenuTypes {
    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ImmersiveFarming.MOD_ID);

    public static final MultiblockContainerIF<ComposterLogic.State, ComposterContainer> COMPOSTER = registerMultiblock(
            "composter", ComposterContainer::makeServer, ComposterContainer::makeClient
    );

    public static final RegistryObject<MenuType<PlowContainer>> PLOW_CART = REGISTER.register("plow",
            () -> IForgeMenuType.create(PlowContainer::new));

    public static final RegistryObject<MenuType<SowerContainer>> SOWER_CART = REGISTER.register("sower",
            () -> IForgeMenuType.create(SowerContainer::new));


    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }

    public static <S extends IMultiblockState, C extends IEContainerMenu>
    MultiblockContainerIF<S, C> registerMultiblock(
            String name, ArgContainerConstructor<MultiblockMenuContext<S>, C> container, ClientContainerConstructor<C> client
    )
    {
        RegistryObject<MenuType<C>> typeRef = REGISTER.register(
                name, () -> {
                    Mutable<MenuType<C>> typeBox = new MutableObject<>();
                    MenuType<C> type = new MenuType<>((id, inv) -> client.construct(typeBox.getValue(), id, inv), FeatureFlagSet.of());
                    typeBox.setValue(type);
                    return type;
                }
        );
        return new MultiblockContainerIF<>(typeRef, container);
    }

    /**
     * Equivalent of IE's MultiblockContainer, which cannot be created outside of IE
     */
    public record MultiblockContainerIF<S extends IMultiblockState, C extends IEContainerMenu>(
            RegistryObject<MenuType<C>> type, ArgContainerConstructor<MultiblockMenuContext<S>, C> factory
    )
    {
        public C create(int windowId, Inventory playerInv, MultiblockMenuContext<S> ctx)
        {
            return factory.construct(getType(), windowId, playerInv, ctx);
        }

        public MenuProvider provide(IMultiblockContext<S> ctx, BlockPos relativeClicked)
        {
            final MultiblockMenuContext<S> menuCtx = new MultiblockMenuContext<>(ctx, ctx.getLevel().toAbsolute(relativeClicked));
            return new MenuProvider()
            {
                @Nonnull
                @Override
                public Component getDisplayName()
                {
                    return Component.empty();
                }

                @Nonnull
                @Override
                public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player)
                {
                    return create(containerId, inventory, menuCtx);
                }
            };
        }

        public MenuType<C> getType()
        {
            return type.get();
        }
    }
}
