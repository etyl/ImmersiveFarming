package net.etylop.immersivefarming.utils;

import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.config.IFConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IFFunctions {
    // Tallest plant (in blocks) that is searched when looking for the soil under a plant
    private static final int MAX_PLANT_HEIGHT = 8;

    public static boolean isBlockTillable(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.TILLABLE_BLOCK) && !level.isClientSide();
    }

    /**
     * Tilling speed of a tool, based on its tier level (works for modded tiers too).
     * Tools that can till but have no tier (e.g. Tinkers' tools) till at speed 1.
     */
    public static int getHoeSpeed(ItemStack stack) {
        if (stack.getItem() instanceof TieredItem tiered) {
            @SuppressWarnings("deprecation")
            int level = tiered.getTier().getLevel();
            if (level <= 0) return 1;                                  // wood, gold
            if (level == 1) return 1 + (Math.random() > 0.5 ? 1 : 0);  // stone
            return Math.min(level, 4);                                 // iron 2, diamond 3, netherite 4
        }
        return 1;
    }

    /* ---------------------------------------------------------------- */
    /* Plants                                                            */
    /* ---------------------------------------------------------------- */

    /**
     * Lowest block of the plant at {@code pos}: walks down while the block below is the same plant,
     * so that tall plants (pitcher plants, IE hemp, tomato vines...) are handled from any of their blocks.
     */
    public static BlockPos getPlantBase(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return pos;
        Block plant = state.getBlock();
        BlockPos base = pos;
        for (int i = 0; i < MAX_PLANT_HEIGHT && level.getBlockState(base.below()).is(plant); i++) {
            base = base.below();
        }
        return base;
    }

    /** Highest block of the plant at {@code pos}. */
    public static BlockPos getPlantTop(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return pos;
        Block plant = state.getBlock();
        BlockPos top = pos;
        for (int i = 0; i < MAX_PLANT_HEIGHT && level.getBlockState(top.above()).is(plant); i++) {
            top = top.above();
        }
        return top;
    }

    /**
     * Position of the soil related to a clicked block: the block itself if it is soil,
     * otherwise the block under the plant that was clicked.
     */
    public static BlockPos getSoilPos(BlockGetter level, BlockPos clicked) {
        if (level.getBlockState(clicked).getBlock() instanceof Soil) return clicked;
        return getPlantBase(level, clicked).below();
    }

    /** Replaces a plant by a dead crop. */
    public static void killPlant(LevelAccessor level, BlockPos pos, int flags) {
        BlockPos base = getPlantBase(level, pos);
        if (level.getBlockState(base).hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            // Two-half plants (pitcher, sunflower...): remove the upper half silently, without updating the lower half
            BlockPos top = getPlantTop(level, base);
            for (BlockPos p = top; p.getY() > base.getY(); p = p.below()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), flags | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        // Other stacked plants (IE hemp, tomato vines on ropes...) handle their own unsupported blocks
        level.setBlock(base, IFBlocks.DEAD_CROP.get().defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
    }

    /* ---------------------------------------------------------------- */
    /* Farmland conversion                                               */
    /* ---------------------------------------------------------------- */

    private record ConvertibleBlocks(List<? extends String> source, Set<Block> blocks, List<TagKey<Block>> tags) {}

    private static volatile ConvertibleBlocks convertibleCache = null;

    private static boolean configLoaded() {
        return IFConfig.spec().isLoaded();
    }

    /** Whether the block is listed in the convertible_farmland config (ignores replace_farmland). */
    public static boolean isConvertibleFarmland(BlockState state) {
        if (!configLoaded() || state.getBlock() instanceof Soil) return false;
        List<? extends String> list = IFConfig.get().cropConfig.convertible_farmland.get();
        ConvertibleBlocks cache = convertibleCache;
        if (cache == null || cache.source() != list) {
            Set<Block> blocks = new HashSet<>();
            List<TagKey<Block>> tags = new ArrayList<>();
            for (String entry : list) {
                String id = entry.trim();
                if (id.startsWith("#")) {
                    ResourceLocation rl = ResourceLocation.tryParse(id.substring(1));
                    if (rl != null) tags.add(BlockTags.create(rl));
                }
                else {
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    if (rl != null && ForgeRegistries.BLOCKS.containsKey(rl)) blocks.add(ForgeRegistries.BLOCKS.getValue(rl));
                }
            }
            cache = new ConvertibleBlocks(list, blocks, tags);
            convertibleCache = cache;
        }
        if (cache.blocks().contains(state.getBlock())) return true;
        for (TagKey<Block> tag : cache.tags()) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    /** Whether a block is (or will be converted to) soil managed by this mod. */
    public static boolean isManagedSoil(BlockState state) {
        if (state.getBlock() instanceof Soil) return true;
        return configLoaded() && IFConfig.get().cropConfig.replace_farmland.get() && isConvertibleFarmland(state);
    }

    /**
     * Whether bone meal must be refused for the plant at {@code plantPos}: any plant on soil, and crops on
     * farmland that will be converted to soil (other plants on farmland, such as saplings, are never converted).
     */
    public static boolean isBonemealBlocked(BlockGetter level, BlockPos plantPos) {
        BlockPos base = getPlantBase(level, plantPos);
        BlockState soil = level.getBlockState(base.below());
        if (soil.getBlock() instanceof Soil) return true;
        if (!isManagedSoil(soil)) return false;
        Block plant = level.getBlockState(base).getBlock();
        return plant instanceof CropBlock || plant instanceof StemBlock || plant instanceof PitcherCropBlock;
    }
}
