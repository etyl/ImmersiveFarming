package net.etylop.immersivefarming.block;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.multiblocks.BlockMatcher;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import com.google.common.collect.ImmutableList;
import net.etylop.immersivefarming.block.multiblocks.IFTemplateMultiblock;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederMultiblock;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterMultiblock;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class IFMultiblocks {
    public static final List<MultiblockHandler.IMultiblock> IF_MULTIBLOCKS = new ArrayList<>();

    public static IFTemplateMultiblock COMPOSTER;
    public static IFTemplateMultiblock BREEDER;

    public static void init()
    {
        //Add general matcher predicates
        //Basic blockstate matcher
        BlockMatcher.addPredicate((expected, found, world, pos) -> expected==found? BlockMatcher.Result.allow(1): BlockMatcher.Result.deny(1));
        //FourWayBlock (fences etc): allow additional connections
        List<Property<Boolean>> sideProperties = ImmutableList.of(
                CrossCollisionBlock.NORTH, CrossCollisionBlock.EAST, CrossCollisionBlock.SOUTH, CrossCollisionBlock.WEST
        );
        BlockMatcher.addPreprocessor((expected, found, world, pos) -> {
            if(expected.getBlock() instanceof CrossCollisionBlock&&expected.getBlock()==found.getBlock())
                for(Property<Boolean> side : sideProperties)
                    if(!expected.getValue(side))
                        found = found.setValue(side, false);
            return found;
        });
        //Tags
        ImmutableList.Builder<TagKey<Block>> genericTagsBuilder = ImmutableList.builder();
        for(EnumMetals metal : EnumMetals.values())
        {
            IETags.MetalTags tags = IETags.getTagsFor(metal);
            genericTagsBuilder.add(tags.storage)
                    .add(tags.sheetmetal);
        }
        genericTagsBuilder.add(IETags.scaffoldingAlu);
        genericTagsBuilder.add(IETags.scaffoldingSteel);
        genericTagsBuilder.add(IETags.treatedWoodSlab);
        genericTagsBuilder.add(IETags.treatedWood);
        genericTagsBuilder.add(IETags.fencesSteel);
        genericTagsBuilder.add(IETags.fencesAlu);
        List<TagKey<Block>> genericTags = genericTagsBuilder.build();
        BlockMatcher.addPredicate((expected, found, world, pos) -> {
            if(expected.getBlock()!=found.getBlock())
                for(TagKey<Block> t : genericTags)
                    if(expected.is(t)&&found.is(t))
                        return BlockMatcher.Result.allow(2);
            return BlockMatcher.Result.DEFAULT;
        });
        //Ignore hopper facing
        BlockMatcher.addPreprocessor((expected, found, world, pos) -> {
            if(expected.getBlock()== Blocks.HOPPER&&found.getBlock()==Blocks.HOPPER)
                return found.setValue(HopperBlock.FACING, expected.getValue(HopperBlock.FACING));
            return found;
        });
        //Allow multiblocks to be formed underwater
        BlockMatcher.addPreprocessor((expected, found, world, pos) -> {
            // Un-waterlog if the expected state is dry, but the found one is not
            if(expected.hasProperty(WATERLOGGED)&&found.hasProperty(WATERLOGGED)
                    &&!expected.getValue(WATERLOGGED)&&found.getValue(WATERLOGGED))
                return found.setValue(WATERLOGGED, false);
            else
                return found;
        });

        //Init multiblocks
        COMPOSTER = register(new ComposterMultiblock());
        BREEDER = register(new BreederMultiblock());

    }

    private static <T extends MultiblockHandler.IMultiblock>
    T register(T multiblock) {
        IF_MULTIBLOCKS.add(multiblock);
        MultiblockHandler.registerMultiblock(multiblock);
        return multiblock;
    }


}
