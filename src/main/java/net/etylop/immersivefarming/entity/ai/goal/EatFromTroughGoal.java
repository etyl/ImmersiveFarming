package net.etylop.immersivefarming.entity.ai.goal;

import net.etylop.immersivefarming.block.IFPoiTypes;
import net.etylop.immersivefarming.block.custom.FeedingTroughBlock;
import net.etylop.immersivefarming.block.entity.FeedingTroughBlockEntity;
import net.etylop.immersivefarming.entity.AnimalFeeding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * A hungry animal (a baby that needs food to grow, or an adult that needs food to produce) walks to the nearest
 * feeding trough holding food it eats, and eats one item from it. See {@link AnimalFeeding}.
 * <p>Most animals are driven by goals, this one moves them with their navigation. Some animals (goats, camels,
 * and some modded animals) are driven by a brain instead, whose behaviors also move them: for those, this goal sets
 * the brain's walk and look targets, and lets the brain move the animal. As goals tick before the brain, the walk target
 * is already set when the brain ticks, which keeps its idle behaviors (wandering, following adults) from starting.
 */
public class EatFromTroughGoal extends Goal {
    private static final int SEARCH_RADIUS = 16;
    /** Ticks between two searches for a trough while hungry */
    private static final int SEARCH_INTERVAL = 60;
    private static final int EAT_DURATION = 30;
    /** Give up when the trough could not be reached in this many ticks */
    private static final int TIMEOUT = 600;
    private static final double SPEED = 1.1;
    /** Horizontal distance to the trough under which the animal walks straight to it, once its path is done */
    private static final double APPROACH_DISTANCE = 1.5;

    private final Animal animal;
    @Nullable
    private BlockPos trough;
    /** Both halves of the trough */
    @Nullable
    private AABB troughBox;
    @Nullable
    private Path path;
    /** Where the animal stands to eat: the end of its path, next to the trough */
    @Nullable
    private BlockPos standPos;
    private int searchCooldown;
    private int ticks;
    private int eatTicks;

    public EatFromTroughGoal(Animal animal) {
        this.animal = animal;
        this.searchCooldown = animal.getRandom().nextInt(reducedTickDelay(SEARCH_INTERVAL));
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }
        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL);
        if (this.isBrainBusy() || !AnimalFeeding.isHungry(this.animal)) return false;

        this.trough = this.findTrough();
        if (this.trough == null) return false;
        this.troughBox = this.getTroughBox(this.trough);
        this.path = this.pathToTrough();
        if (this.path == null || !this.path.canReach()) return false;
        this.standPos = this.path.getEndNode() != null ? this.path.getEndNode().asBlockPos() : this.animal.blockPosition();
        return true;
    }

    /**
     * A path to a position next to the trough (within 1 block of it).
     * Not PathNavigation#createPath(BlockPos, int): ground navigation replaces a solid target (the trough, 1.5 blocks high
     * as fences) with the first non solid block above it, which can't be reached from the ground next to the trough.
     * Villagers path to their workstation the same way.
     */
    @Nullable
    private Path pathToTrough() {
        return this.trough == null ? null : this.animal.getNavigation().createPath(Set.of(this.trough), 1);
    }

    @Override
    public boolean canContinueToUse() {
        return this.trough != null && this.troughBox != null && this.ticks < TIMEOUT && !this.isBrainBusy()
                && AnimalFeeding.isHungry(this.animal) && this.getTrough() != null;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.eatTicks = 0;
        if (this.usesBrain()) {
            this.setBrainTargets();
        } else {
            this.animal.getNavigation().moveTo(this.path, SPEED);
        }
    }

    @Override
    public void stop() {
        if (this.trough != null && this.usesBrain()) {
            // Only forget the targets this goal set, the brain may have set its own meanwhile
            Brain<?> brain = this.animal.getBrain();
            brain.getMemory(MemoryModuleType.WALK_TARGET)
                    .filter(target -> target.getTarget().currentBlockPosition().equals(this.standPos))
                    .ifPresent(target -> brain.eraseMemory(MemoryModuleType.WALK_TARGET));
            brain.getMemory(MemoryModuleType.LOOK_TARGET)
                    .filter(target -> target.currentBlockPosition().equals(this.trough))
                    .ifPresent(target -> brain.eraseMemory(MemoryModuleType.LOOK_TARGET));
        }
        this.trough = null;
        this.troughBox = null;
        this.path = null;
        this.standPos = null;
        this.animal.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.trough == null) return;
        this.ticks++;
        boolean brain = this.usesBrain();
        if (brain) {
            // Set again each tick: it keeps the brain's idle behaviors from moving the animal elsewhere
            this.setBrainTargets();
        } else {
            this.animal.getLookControl().setLookAt(this.trough.getX() + 0.5, this.trough.getY() + 0.3, this.trough.getZ() + 0.5);
        }

        if (!this.isNextToTrough()) {
            this.eatTicks = 0;
            if (this.animal.getNavigation().isDone()) {
                if (this.isCloseToTrough()) {
                    // The path ends next to the trough, but it is done as soon as the animal is near the end of the path
                    // (or, for brains, in its last block), which may be a bit too far to eat: walk the last steps to it
                    this.walkToTrough();
                } else if (!brain && this.ticks % 20 == 0) {
                    // The path may have been interrupted (pushed by another animal...)
                    Path path = this.pathToTrough();
                    if (path != null) this.animal.getNavigation().moveTo(path, SPEED);
                }
            }
            return;
        }

        this.animal.getNavigation().stop();
        FeedingTroughBlockEntity trough = this.getTrough();
        if (trough == null) return;
        this.eatTicks++;
        if (this.eatTicks % 6 == 0 && this.animal.level() instanceof ServerLevel level) {
            ItemStack food = trough.peekFoodFor(this.animal);
            if (!food.isEmpty()) {
                // Above the part of the trough the animal eats from
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.copyWithCount(1)),
                        this.closestTroughX(), this.trough.getY() + 0.6, this.closestTroughZ(), 3, 0.2, 0.05, 0.2, 0.03);
            }
        }
        if (this.eatTicks >= EAT_DURATION) {
            ItemStack food = trough.takeFoodFor(this.animal);
            if (!food.isEmpty()) AnimalFeeding.feed(this.animal, food);
        }
    }

    /** Whether the animal is driven by a brain, which moves it through its walk target */
    private boolean usesBrain() {
        return this.animal.getBrain().checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
    }

    /** A brain-driven animal fleeing or following a player holding food has better things to do */
    private boolean isBrainBusy() {
        Brain<?> brain = this.animal.getBrain();
        return brain.hasMemoryValue(MemoryModuleType.IS_PANICKING) || brain.hasMemoryValue(MemoryModuleType.IS_TEMPTED);
    }

    private void setBrainTargets() {
        if (this.trough == null || this.standPos == null) return;
        Brain<?> brain = this.animal.getBrain();
        // To the standing position found by the path, not the trough itself: the brain's walking behavior
        // paths with PathNavigation#createPath(BlockPos, int), which can't target the trough (see pathToTrough)
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.standPos, (float) SPEED, 0));
        if (brain.checkMemory(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED)) {
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.trough));
        }
    }

    /** The trough this animal is going to, if it still holds food for it */
    @Nullable
    private FeedingTroughBlockEntity getTrough() {
        if (this.trough != null && this.animal.level().getBlockEntity(this.trough) instanceof FeedingTroughBlockEntity trough
                && trough.hasFoodFor(this.animal)) {
            return trough;
        }
        return null;
    }

    /** Close enough to eat, from either half of the trough */
    private boolean isNextToTrough() {
        return this.troughBox != null && this.animal.getBoundingBox().inflate(0.6, 0.5, 0.6).intersects(this.troughBox);
    }

    private boolean isCloseToTrough() {
        if (this.troughBox == null) return false;
        AABB box = this.animal.getBoundingBox();
        double dx = Math.max(this.troughBox.minX - box.maxX, box.minX - this.troughBox.maxX);
        double dz = Math.max(this.troughBox.minZ - box.maxZ, box.minZ - this.troughBox.maxZ);
        return dx < APPROACH_DISTANCE && dz < APPROACH_DISTANCE
                && Math.abs(this.animal.getY() - this.troughBox.minY) < 1.0;
    }

    /** Walk straight to the closest point of the trough, at the animal's height (so it doesn't try to jump on it) */
    private void walkToTrough() {
        this.animal.getMoveControl().setWantedPosition(this.closestTroughX(), this.animal.getY(), this.closestTroughZ(), SPEED);
    }

    private double closestTroughX() {
        return this.troughBox == null ? this.animal.getX() : Mth.clamp(this.animal.getX(), this.troughBox.minX, this.troughBox.maxX);
    }

    private double closestTroughZ() {
        return this.troughBox == null ? this.animal.getZ() : Mth.clamp(this.animal.getZ(), this.troughBox.minZ, this.troughBox.maxZ);
    }

    /** The trough block at this position and its other half, which FACING points to */
    private AABB getTroughBox(BlockPos pos) {
        AABB box = new AABB(pos);
        BlockState state = this.animal.level().getBlockState(pos);
        if (state.getBlock() instanceof FeedingTroughBlock) {
            box = box.minmax(new AABB(pos.relative(state.getValue(FeedingTroughBlock.FACING))));
        }
        return box;
    }

    @Nullable
    private BlockPos findTrough() {
        if (!(this.animal.level() instanceof ServerLevel level)) return null;
        return level.getPoiManager().findClosest(
                type -> type.is(IFPoiTypes.FEEDING_TROUGH.getKey()),
                pos -> level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough && trough.hasFoodFor(this.animal),
                this.animal.blockPosition(), SEARCH_RADIUS, PoiManager.Occupancy.ANY
        ).orElse(null);
    }
}
