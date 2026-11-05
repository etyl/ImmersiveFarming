package net.etylop.immersivefarming.event;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.etylop.immersivefarming.utils.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Optional;


public class IFEvents {

    @Mod.EventBusSubscriber(modid = ImmersiveFarming.MOD_ID)
    public static class ForgeEvents {

        // Tilling a tillable block (by hand, or by any tool of any mod that can till) gives soil.
        // Further tilling of the soil is handled by Soil#getToolModifiedState.
        @SubscribeEvent
        public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
            if (event.getToolAction() != ToolActions.HOE_TILL || !event.getState().is(ModTags.Blocks.TILLABLE_BLOCK)) {
                return;
            }
            // Same rule as vanilla tilling: from above, with air above the block
            if (event.getContext().getClickedFace() == Direction.DOWN ||
                !event.getLevel().getBlockState(event.getPos().above()).isAir()) {
                return;
            }
            event.setFinalState(IFBlocks.SOIL.get().defaultBlockState());
        }

        // Crops on soil can't be bonemealed (direct calls to performBonemeal are blocked by the mixins)
        @SubscribeEvent
        public static void onBonemeal(BonemealEvent event) {
            if (IFFunctions.isBonemealBlocked(event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onCropGrowth(BlockEvent.CropGrowEvent.Pre event) {
            if (event.getResult() == Event.Result.DENY) return;
            // Some plants (cave/weeping vines) fire the event at the empty block they grow into
            if (event.getState().isAir()) return;

            LevelAccessor level = event.getLevel();
            // Any plant growing on soil is affected; tall plants are checked from their lowest block
            BlockPos plantPos = IFFunctions.getPlantBase(level, event.getPos());
            BlockPos soilPos = plantPos.below();

            BlockState soilBlock = level.getBlockState(soilPos);
            if (IFConfig.get().cropConfig.replace_farmland.get() && IFFunctions.isConvertibleFarmland(soilBlock)) {
                soilBlock = Soil.fromFarmland(soilBlock);
                level.setBlock(soilPos, soilBlock, 2);
            }
            if (!(soilBlock.getBlock() instanceof Soil)) {
                return;
            }

            if (level.getBiome(plantPos).is(Tags.Biomes.IS_COLD)) {
                event.setResult(Event.Result.DENY);
                if (Math.random()<.3) {
                    IFFunctions.killPlant(level, plantPos, 2);
                }
                return;
            }

            int water = soilBlock.getValue(Soil.WATER);
            if (water == 0 || (water == Soil.WATER_FLOODED && Math.random() >= IFConfig.get().cropConfig.flooded_growth.get())) {
                event.setResult(Event.Result.DENY);
                return;
            }

            // Plants need sunlight (checked at their highest block, for tall plants)
            if (IFConfig.get().cropConfig.require_sunlight.get() && !level.canSeeSky(IFFunctions.getPlantTop(level, plantPos))) {
                event.setResult(Event.Result.DENY);
                return;
            }

            int fertilization = soilBlock.getValue(Soil.FERTILITY);
            if (fertilization>=1 || Math.random() < IFConfig.get().cropConfig.unfertilized_growth.get()) {
                event.setResult(Event.Result.DEFAULT);
            }
            else {
                event.setResult(Event.Result.DENY);
            }
        }

        @SubscribeEvent
        public static void onCropGrowthPost(BlockEvent.CropGrowEvent.Post event) {
            BlockState cropBlock = event.getState();
            LevelAccessor level = event.getLevel();
            BlockPos soilPos = IFFunctions.getPlantBase(level, event.getPos()).below();
            BlockState soilBlock = level.getBlockState(soilPos);

            if (!(soilBlock.getBlock() instanceof Soil)) {
                return;
            }

            BlockState newSoil = soilBlock;
            int water = soilBlock.getValue(Soil.WATER);
            if (water > 0 && water < Soil.WATER_FLOODED) {
                newSoil = newSoil.setValue(Soil.WATER, water - 1);
            }

            // Fertility is consumed when a plant reaches maturity (any plant with an "age" property)
            Optional<IntegerProperty> ageProperty = cropBlock.getProperties().stream()
                    .filter(property -> "age".equals(property.getName()) && property instanceof IntegerProperty)
                    .map(property -> (IntegerProperty) property)
                    .findFirst();

            if (ageProperty.isPresent()) {
                int age = cropBlock.getValue(ageProperty.get());
                int maxAge = Collections.max(ageProperty.get().getPossibleValues());

                // Mature stems also fire this event when they fail to place a fruit
                if (age == maxAge && event.getOriginalState() != cropBlock) {
                    newSoil = newSoil.setValue(Soil.FERTILITY, 0);
                    event.setResult(Event.Result.ALLOW);
                }
            }

            if (newSoil != soilBlock) {
                level.setBlock(soilPos, newSoil, 2);
            }

            if (cropBlock.getBlock() instanceof AttachedStemBlock) {
                if (Math.random() < IFConfig.get().cropConfig.stem_death.get()) {
                    level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }
}
