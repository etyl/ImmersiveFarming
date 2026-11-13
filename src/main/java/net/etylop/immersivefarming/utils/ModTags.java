package net.etylop.immersivefarming.utils;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> TILLABLE_BLOCK =
                tag("tillable_block");
        // Blocks that give rich soil when tilled (Farmer's Delight rich soil)
        public static final TagKey<Block> RICH_TILLABLE_BLOCK =
                tag("rich_tillable_block");
        // Farmland converted to rich soil (Farmer's Delight rich soil farmland), always convertible
        public static final TagKey<Block> RICH_FARMLAND =
                tag("rich_farmland");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(ImmersiveFarming.MOD_ID, name));
        }

        private static TagKey<Block> forgeTag(String name) {
            return BlockTags.create(new ResourceLocation("forge", name));
        }
    }

    public static class Items {
        // Extra items accepted as seeds by the sower (in addition to #forge:seeds and items placing crops)
        public static final TagKey<Item> SOWER_SEEDS =
                tag("sower_seeds");

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(new ResourceLocation(ImmersiveFarming.MOD_ID, name));
        }

        private static TagKey<Item> forgeTag(String name) {
            return ItemTags.create(new ResourceLocation("forge", name));
        }
    }
}
