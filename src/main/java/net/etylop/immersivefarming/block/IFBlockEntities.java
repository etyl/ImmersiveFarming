package net.etylop.immersivefarming.block;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.MultiblockBEType;
import com.google.common.collect.ImmutableSet;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.entity.CompostingBinBlockEntity;
import net.etylop.immersivefarming.block.entity.SprinklerBlockEntity;
import net.etylop.immersivefarming.block.entity.SprinklerExtendedBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class IFBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, ImmersiveFarming.MOD_ID);

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }

    public static final MultiblockBEType<SprinklerBlockEntity> SPRINKLER = makeMultiblock(
            "sprinkler", SprinklerBlockEntity::new, IFBlocks.SPRINKLER
    );

    public static final MultiblockBEType<SprinklerExtendedBlockEntity> SPRINKLER_EXTENDED = makeMultiblock(
            "sprinkler_extended", SprinklerExtendedBlockEntity::new, IFBlocks.SPRINKLER_EXTENDED
    );

    public static final RegistryObject<BlockEntityType<CompostingBinBlockEntity>> COMPOSTING_BIN = REGISTER.register(
            "composting_bin", makeType(CompostingBinBlockEntity::new, IFBlocks.COMPOSTING_BIN)
    );

    public static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeType(BlockEntityType.BlockEntitySupplier<T> create, Supplier<? extends Block> valid)
    {
        return makeTypeMultipleBlocks(create, ImmutableSet.of(valid));
    }

    public static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeTypeMultipleBlocks(
            BlockEntityType.BlockEntitySupplier<T> create, Collection<? extends Supplier<? extends Block>> valid
    )
    {
        return () -> new BlockEntityType<>(
                create, ImmutableSet.copyOf(valid.stream().map(Supplier::get).collect(Collectors.toList())), null
        );
    }

    private static <T extends BlockEntity & IEBlockInterfaces.IGeneralMultiblock>
    MultiblockBEType<T> makeMultiblock(String name, MultiblockBEType.BEWithTypeConstructor<T> make, Supplier<? extends Block> block)
    {
        return new MultiblockBEType<>(
                name, REGISTER, make, block, state -> state.hasProperty(IEProperties.MULTIBLOCKSLAVE)&&!state.getValue(IEProperties.MULTIBLOCKSLAVE)
        );
    }
}
