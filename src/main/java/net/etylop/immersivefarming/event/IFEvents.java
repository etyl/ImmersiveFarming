package net.etylop.immersivefarming.event;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.utils.ModTags;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Block target = event.getLevel().getBlockState(event.getPos()).getBlock();
            if (target.defaultBlockState().is(ModTags.Blocks.TILLABLE_BLOCK) &&
                !event.getLevel().isClientSide() &&
                event.getEntity().getMainHandItem().getItem() instanceof HoeItem) {

                event.getLevel().setBlock(event.getPos(), IFBlocks.SOIL.get().defaultBlockState(), 3);
                event.getEntity().getMainHandItem().hurtAndBreak(1, event.getEntity(), (val) -> val.broadcastBreakEvent(event.getEntity().getUsedItemHand()));            }
        }

        // Tilling by other mods gives soil
        @SubscribeEvent
        public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
            if (event.getToolAction() == ToolActions.HOE_TILL &&
                event.getFinalState() != null && event.getFinalState().is(Blocks.FARMLAND) &&
                IFConfig.get().cropConfig.replace_farmland.get()) {
                event.setFinalState(IFBlocks.SOIL.get().defaultBlockState());
            }
        }

        // Crops on soil can't be bonemealed
        @SubscribeEvent
        public static void onBonemeal(BonemealEvent event) {
            if (event.getLevel().getBlockState(event.getPos().below()).getBlock() instanceof Soil) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onTrampleFarmland(BlockEvent.FarmlandTrampleEvent event) {
            if (event.getState().getBlock() instanceof Soil) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onCropGrowth(BlockEvent.CropGrowEvent.Pre event) {
            if (event.getResult() == Event.Result.DENY) return;

            LevelAccessor level = event.getLevel();
            BlockState cropBlock =  level.getBlockState(event.getPos());

            if (!(cropBlock.getBlock() instanceof CropBlock || cropBlock.getBlock() instanceof StemBlock)) {
                return;
            }

            BlockState soilBlock = level.getBlockState(event.getPos().below());
            if (soilBlock.is(Blocks.FARMLAND) && IFConfig.get().cropConfig.replace_farmland.get()) {
                soilBlock = Soil.fromFarmland(soilBlock);
                level.setBlock(event.getPos().below(), soilBlock, 2);
            }
            if (!(soilBlock.getBlock() instanceof Soil)) {
                return;
            }

            if (level.getBiome(event.getPos()).is(Tags.Biomes.IS_COLD)) {
                event.setResult(Event.Result.DENY);
                if (Math.random()<.3) {
                    level.setBlock(event.getPos(),IFBlocks.DEAD_CROP.get().defaultBlockState(), 2);
                }
                return;
            }

            int water = soilBlock.getValue(Soil.WATER);
            if (water == 0 || (water == Soil.WATER_FLOODED && Math.random() >= IFConfig.get().cropConfig.flooded_growth.get())) {
                event.setResult(Event.Result.DENY);
                return;
            }

            if (level.canSeeSky(event.getPos())) {
                int fertilization = soilBlock.getValue(Soil.FERTILITY);

                if (fertilization>=1 || Math.random() < IFConfig.get().cropConfig.unfertilized_growth.get()) {
                    event.setResult(Event.Result.DEFAULT);
                }
                else {
                    event.setResult(Event.Result.DENY);
                }
            }
        }

        @SubscribeEvent
        public static void onCropGrowthPost(BlockEvent.CropGrowEvent.Post event) {
            BlockState cropBlock = event.getState();
            LevelAccessor level = event.getLevel();
            BlockState soilBlock = event.getLevel().getBlockState(event.getPos().below());

            if (!(soilBlock.getBlock() instanceof Soil)) {
                return;
            }

            BlockState newSoil = soilBlock;
            int water = soilBlock.getValue(Soil.WATER);
            if (water > 0 && water < Soil.WATER_FLOODED) {
                newSoil = newSoil.setValue(Soil.WATER, water - 1);
            }

            if (cropBlock.getBlock() instanceof CropBlock || cropBlock.getBlock() instanceof StemBlock) {
                Optional<Integer> ageOptional = cropBlock.getValues().entrySet().stream()
                        .filter(entry -> "age".equals(entry.getKey().getName()) && entry.getValue() instanceof Integer)
                        .map(entry -> (Integer) entry.getValue())
                        .findFirst();

                Optional<Integer> maxAgeOptional = cropBlock.getValues().entrySet().stream()
                        .filter(entry -> "age".equals(entry.getKey().getName()) && entry.getKey() instanceof IntegerProperty)
                        .map(entry -> Collections.max(((IntegerProperty) entry.getKey()).getPossibleValues()))
                        .findFirst();

                int age = ageOptional.orElse(0);
                int maxAge = maxAgeOptional.orElse(10);

                // Mature stems also fire this event when they fail to place a fruit
                if (age == maxAge && event.getOriginalState() != cropBlock) {
                    newSoil = newSoil.setValue(Soil.FERTILITY, 0);
                    event.setResult(Event.Result.ALLOW);
                }
            }

            if (newSoil != soilBlock) {
                level.setBlock(event.getPos().below(), newSoil, 2);
            }

            if (cropBlock.getBlock() instanceof AttachedStemBlock) {
                if (Math.random() < IFConfig.get().cropConfig.stem_death.get()) {
                    level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }
}
