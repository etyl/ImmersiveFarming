package net.etylop.immersivefarming.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

public final class IFFunctions {

    public static boolean isBlockTillable(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.TILLABLE_BLOCK) && !level.isClientSide();
    }

    public static final int getHoeSpeed(Item item) {
        if (!(item instanceof HoeItem)) return 0;
        TagKey<Block> tag = ((HoeItem) item).getTier().getTag();
        if (tag == Tags.Blocks.NEEDS_WOOD_TOOL || tag == Tags.Blocks.NEEDS_GOLD_TOOL) {
            return 1;
        }
        else if (tag == BlockTags.NEEDS_STONE_TOOL) {
            return 1 + (Math.random()>0.5 ? 1 : 0);
        }
        else if (tag == BlockTags.NEEDS_IRON_TOOL) {
            return 2;
        }
        else if (tag == BlockTags.NEEDS_DIAMOND_TOOL) {
            return 3;
        }
        else if (tag == Tags.Blocks.NEEDS_NETHERITE_TOOL) {
            return 4;
        }
        return 1;
    }

}
