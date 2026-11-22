package net.etylop.immersivefarming.event;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.custom.FeedingTroughBlock;
import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.block.entity.SprinklerRegistry;
import net.etylop.immersivefarming.block.multiblocks.AnimalStall;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederBlock;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederLogic;
import net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenBlock;
import net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenLogic;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.entity.AnimalFeeding;
import net.etylop.immersivefarming.entity.Livestock;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.etylop.immersivefarming.utils.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



public class IFEvents {

    @Mod.EventBusSubscriber(modid = ImmersiveFarming.MOD_ID)
    public static class ForgeEvents {

        // Tilling a tillable block (by hand, or by any tool of any mod that can till) gives soil.
        // Further tilling of the soil is handled by Soil#getToolModifiedState.
        @SubscribeEvent
        public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
            if (event.getToolAction() != ToolActions.HOE_TILL ||
                !(event.getState().is(ModTags.Blocks.TILLABLE_BLOCK) || event.getState().is(ModTags.Blocks.RICH_TILLABLE_BLOCK))) {
                return;
            }
            // Same rule as vanilla tilling: from above, with air above the block
            if (event.getContext().getClickedFace() == Direction.DOWN ||
                !event.getLevel().getBlockState(event.getPos().above()).isAir()) {
                return;
            }
            event.setFinalState(IFFunctions.getTilledSoil(event.getState()));
        }

        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (!(event.getTarget() instanceof Animal animal)) return;
            ItemStack held = event.getItemStack();
            if (!animal.isFood(held)) return;
            if (AnimalFeeding.needsFood(animal)) {
                feedBaby(event, animal, held);
            } else if (!event.getLevel().isClientSide && AnimalFeeding.isHungry(animal) && !isLeashedBy(animal, event.getEntity())) {
                feedAdult(event, animal, held);
            }
        }

        // Babies that need food eat one meal when hungry, instead of the vanilla growth boost
        private static void feedBaby(PlayerInteractEvent.EntityInteract event, Animal animal, ItemStack held) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            if (event.getLevel().isClientSide) return;

            Player player = event.getEntity();
            if (!AnimalFeeding.isHungry(animal)) {
                player.displayClientMessage(Component.translatable("entity.immersivefarming.baby.not_hungry"), true);
                return;
            }
            AnimalFeeding.feed(animal, held);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }

        // A hungry adult eats the food (server side). Breeding still works as in vanilla: when the animal can fall in love,
        // vanilla takes the item and the same item also makes it breed. Otherwise the meal takes the item here.
        private static void feedAdult(PlayerInteractEvent.EntityInteract event, Animal animal, ItemStack held) {
            AnimalFeeding.feed(animal, held);
            if (animal.getAge() == 0 && animal.canFallInLove()) return;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
        }

        // Hungry adults, and adults milked recently, can't be milked.
        // The client doesn't know whether the animal is hungry and has already milked it on its side:
        // send the player's inventory again to undo it.
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onMilking(PlayerInteractEvent.EntityInteract event) {
            if (event.getLevel().isClientSide || !(event.getTarget() instanceof Animal animal)) return;
            Player player = event.getEntity();
            if (!isMilking(animal, event.getItemStack()) || isLeashedBy(animal, player)) return;

            String refusal = AnimalFeeding.getMilkingCooldown(animal) > 0 ? "entity.immersivefarming.animal.no_milk"
                    : AnimalFeeding.isHungry(animal) ? "entity.immersivefarming.animal.hungry" : null;
            if (refusal != null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                player.displayClientMessage(Component.translatable(refusal), true);
                player.containerMenu.sendAllDataToRemote();
            } else {
                AnimalFeeding.produce(animal);
            }
        }

        /** Vanilla milking: a bucket on a cow or goat, a bowl on a mooshroom (and their variants) */
        private static boolean isMilking(Animal animal, ItemStack held) {
            if (animal.isBaby()) return false;
            return held.is(Items.BUCKET) && AnimalFeeding.isMilkable(animal)
                    || held.is(Items.BOWL) && animal instanceof MushroomCow;
        }

        /** Right clicking an animal on the player's own lead unleashes it, before any other interaction */
        private static boolean isLeashedBy(Animal animal, Player player) {
            return animal.isLeashed() && animal.getLeashHolder() == player;
        }

        // Block#use is skipped when sneaking with an item: let the trough take whole stacks of food
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getEntity().isSecondaryUseActive() || event.getItemStack().isEmpty()) return;
            Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
            // The troughs of the breeder and of the animal pen also take the food of the animals in their stalls:
            // they sort the items out themselves, and pass on anything else, which is then placed against them
            if (block instanceof BreederBlock || block instanceof AnimalPenBlock
                    || (block instanceof FeedingTroughBlock && Livestock.isFeed(event.getItemStack(), event.getLevel()))) {
                event.setUseBlock(Event.Result.ALLOW);
            }
        }

        // The animals held in a stall (animal breeder, animal pen) don't hop about
        @SubscribeEvent
        public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
            AnimalStall.onJump(event.getEntity());
        }

        // What an animal pen harvests comes out of its hatch
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onLivingDrops(LivingDropsEvent event) {
            AnimalPenLogic.onDrops(event);
        }

        // What an animal breeder breeds comes out of its hatch
        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            BreederLogic.onEntityJoin(event.getEntity(), event.getLevel());
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            SprinklerRegistry.clear(event.getLevel());
            // The livestock samples belong to a level, don't keep it loaded
            Livestock.invalidate(event.getLevel().isClientSide());
        }

        @SubscribeEvent
        public static void onTagsUpdated(TagsUpdatedEvent event) {
            Livestock.invalidate(event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED);
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

            // Fertility is consumed when a plant reaches maturity (any plant with an "age" property),
            // and each crop grown to maturity makes the soil a bit richer.
            // Mature stems also fire this event when they fail to place a fruit.
            if (IFFunctions.isMaxAge(cropBlock) && event.getOriginalState() != cropBlock) {
                newSoil = Soil.enrich(newSoil).setValue(Soil.FERTILITY, 0);
                event.setResult(Event.Result.ALLOW);
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
