package net.etylop.immersivefarming.block.multiblocks;

import blusunrize.immersiveengineering.api.tool.conveyor.ConveyorHandler;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * A stall of a machine that keeps an animal behind a gate (animal breeder, animal pen).
 * <ul>
 * <li>An open gate takes in the animal standing in its doorway (it is walked to its place in the stall), and closes
 * behind it. An animal carried through the doorway by a conveyor is taken off it.</li>
 * <li>The gate is opened and closed by hand with {@link #toggle}: opening it lets its animal out, a gate closed on
 * an empty stall keeps animals out.</li>
 * <li>The animal is a real entity, held in its stall by the shapes of the multiblock: nothing is stored in the
 * machine but who it is.</li>
 * </ul>
 * Server side, but for {@link #closed}, which the machine syncs.
 */
public class AnimalStall
{
    /** An animal this close to its place in its stall is not walked there, but brought there slowly (blocks) */
    private static final double HOLD_RADIUS = 0.3;
    private static final double HOLD_SPEED = 0.03;
    /** How far the head of a held animal turns away from the trough (degrees): its body follows its head */
    private static final float HOLD_HEAD_TURN = 20;
    /** How fast an animal carried by a conveyor is taken off it, and into the stall (blocks per tick) */
    private static final double PULL_SPEED = 0.12;
    /** An animal this close to the middle of the doorway goes through it (blocks) */
    private static final double PULL_ALIGNED = 0.04;
    /** Ticks before the gate of a missing animal opens: entities are loaded after block entities */
    private static final int MISSING_GRACE = 100;

    /** Persistent data of an animal: game time of the last tick a stall walked or held it, see {@link #onJump} */
    private static final String CONTROLLED = ImmersiveFarming.MOD_ID+":stall";

    /** The animal held in the stall, null if the stall is free */
    @Nullable
    private UUID occupant;
    /** Its entity, when it is known (not saved) */
    @Nullable
    private Animal animal;
    private int missingTicks;
    /** Whether the gate is closed: behind its animal, or by hand on an empty stall (synced) */
    public boolean closed;
    /** The animal let out when the gate was opened by hand, until it has left the doorway (not saved) */
    @Nullable
    private UUID released;

    /** What happened to a stall during a tick */
    public enum Change
    {
        NONE,
        /** An animal came in, the gate closed behind it */
        TAKEN,
        /** The animal is gone (dead, taken away), the gate opened */
        LOST
    }

    /**
     * Where a stall is in the world.
     *
     * @param interior inside of the stall
     * @param doorway  inside of the stall, and what lies behind its gate
     * @param standing where its animal stands
     * @param yaw      the way its animal faces: its trough
     */
    public record Place(AABB interior, AABB doorway, Vec3 standing, float yaw)
    {
    }

    /**
     * Holds the animal of the stall, or looks for one to take in.
     *
     * @param accepts the animals an open gate takes in
     */
    public Change tick(Level level, Place place, Predicate<Animal> accepts)
    {
        if(occupant!=null)
        {
            Animal held = findOccupant(level, place.interior);
            if(held==null)
            {
                // Dead, taken away... or not loaded yet
                if(++missingTicks < MISSING_GRACE)
                    return Change.NONE;
                clear();
                return Change.LOST;
            }
            missingTicks = 0;
            hold(held, place.standing, place.yaw);
            return Change.NONE;
        }
        // Closed by hand on an empty stall: nothing comes in
        if(closed)
            return Change.NONE;
        // The animal that was let out is not taken back in before it has left
        if(released!=null&&level.getEntitiesOfClass(Animal.class, place.doorway, a -> a.getUUID().equals(released)).isEmpty())
            released = null;

        Animal candidate = findCandidate(level, place, accepts);
        if(candidate==null)
            return Change.NONE;
        // Walk it in, whatever it was doing
        control(candidate);
        Vec3 target = place.standing;
        candidate.getMoveControl().setWantedPosition(target.x, candidate.getY(), target.z, 1);
        if(isOnConveyor(level, candidate))
            pull(candidate, target, place.yaw);
        if(!isInside(candidate.getBoundingBox(), place.interior)||!isAlone(level, candidate, place.interior))
            return Change.NONE;
        occupant = candidate.getUUID();
        animal = candidate;
        missingTicks = 0;
        closed = true;
        return Change.TAKEN;
    }

    /**
     * Opens or closes the gate by hand. An open gate closes on the animal standing in the stall if there is one:
     * an empty stall stays empty.
     *
     * @param accepts the animals the gate can be closed on
     * @return whether the gate is now closed
     */
    public boolean toggle(Level level, Place place, Predicate<Animal> accepts)
    {
        if(closed)
        {
            // Its animal is free to leave, and the gate will close behind the next one
            released = occupant;
            clear();
            return false;
        }
        Animal inside = findInside(level, place, accepts);
        closed = true;
        released = null;
        missingTicks = 0;
        animal = inside;
        occupant = inside==null?null: inside.getUUID();
        return true;
    }

    /** Lets the animal go, and opens the gate */
    public void clear()
    {
        if(animal!=null)
            animal.getPersistentData().remove(CONTROLLED);
        occupant = null;
        animal = null;
        missingTicks = 0;
        closed = false;
    }

    /** The animal held in the stall, if it is there */
    @Nullable
    public Animal getAnimal()
    {
        return occupant!=null&&animal!=null&&animal.isAlive()&&missingTicks==0?animal: null;
    }

    public boolean isOccupied()
    {
        return occupant!=null;
    }

    public CompoundTag save()
    {
        CompoundTag nbt = new CompoundTag();
        if(occupant!=null)
            nbt.putUUID("occupant", occupant);
        nbt.putBoolean("closed", closed);
        return nbt;
    }

    public void load(CompoundTag nbt)
    {
        occupant = nbt.hasUUID("occupant")?nbt.getUUID("occupant"): null;
        animal = null;
        closed = nbt.getBoolean("closed");
    }

    // ------------------------------------------------------------------ animals

    /** Animals on their own feet. Leashed animals are left to whoever leads them */
    public static boolean isFree(Animal animal)
    {
        return animal.isAlive()&&!animal.isLeashed()&&!animal.isPassenger()&&!animal.isVehicle();
    }

    /** The animal held in this stall, null if it is gone */
    @Nullable
    private Animal findOccupant(Level level, AABB interior)
    {
        Animal known = animal;
        if(known!=null&&known.isAlive()&&known.level()==level&&known.getUUID().equals(occupant)
                &&known.getBoundingBox().intersects(interior))
            return known;
        animal = null;
        // Not looked for every tick: its gate stays closed in the meantime
        if(missingTicks%10!=0)
            return null;
        for(Animal found : level.getEntitiesOfClass(Animal.class, interior, a -> a.isAlive()&&a.getUUID().equals(occupant)))
        {
            animal = found;
            break;
        }
        return animal;
    }

    /** The animal to take in: the one closest to its place in the stall, among those standing in the doorway */
    @Nullable
    private Animal findCandidate(Level level, Place place, Predicate<Animal> accepts)
    {
        AABB doorway = place.doorway;
        Animal best = null;
        double bestDistance = Double.MAX_VALUE;
        for(Animal candidate : level.getEntitiesOfClass(Animal.class, doorway,
                a -> doorway.contains(a.getX(), a.getY()+0.5, a.getZ())&&!a.getUUID().equals(released)&&accepts.test(a)))
        {
            double distance = candidate.distanceToSqr(place.standing);
            if(distance < bestDistance)
            {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The animal a gate closed by hand is closed on: the one closest to its place, among those inside the stall */
    @Nullable
    private static Animal findInside(Level level, Place place, Predicate<Animal> accepts)
    {
        Animal best = null;
        double bestDistance = Double.MAX_VALUE;
        for(Animal candidate : level.getEntitiesOfClass(Animal.class, place.interior,
                a -> isInside(a.getBoundingBox(), place.interior)&&accepts.test(a)))
        {
            double distance = candidate.distanceToSqr(place.standing);
            if(distance < bestDistance)
            {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Whether the animal has passed the gate (the walls keep it in on the other sides) */
    private static boolean isInside(AABB animal, AABB interior)
    {
        final double margin = 1e-4;
        return animal.minX >= interior.minX-margin&&animal.maxX <= interior.maxX+margin
                &&animal.minZ >= interior.minZ-margin&&animal.maxZ <= interior.maxZ+margin;
    }

    /**
     * The gate does not close on anything else: a baby following its parent, an animal that is not taken in,
     * or a player, who would be locked in
     */
    private static boolean isAlone(Level level, Animal animal, AABB interior)
    {
        return level.getEntitiesOfClass(LivingEntity.class, interior, e -> e!=animal&&e.isAlive()&&!e.isSpectator()).isEmpty();
    }

    /** Called every tick an animal is walked in or held: it stops going its own way */
    private static void control(Animal animal)
    {
        animal.getNavigation().stop();
        animal.getPersistentData().putLong(CONTROLLED, animal.level().getGameTime());
    }

    /** Whether a conveyor carries the animal: any part of it is on one */
    private static boolean isOnConveyor(Level level, Animal animal)
    {
        AABB box = animal.getBoundingBox().deflate(1e-3, 0, 1e-3);
        int y = Mth.floor(box.minY);
        for(BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), y, Mth.floor(box.minZ), Mth.floor(box.maxX), y, Mth.floor(box.maxZ)))
            if(ConveyorHandler.isConveyorBlock(level.getBlockState(pos).getBlock()))
                return true;
        return false;
    }

    /**
     * Takes an animal off the conveyor that carries it through the doorway: a conveyor is faster than the animal
     * walks, and brings back to its middle what is on it. The conveyor moves what is on it when entities tick,
     * which is before the machine ticks: the animal goes where the stall sends it.
     */
    private static void pull(Animal animal, Vec3 target, float troughYaw)
    {
        Vec3 inward = Vec3.directionFromRotation(0, troughYaw);
        Vec3 toTarget = new Vec3(target.x-animal.getX(), 0, target.z-animal.getZ());
        Vec3 across = toTarget.subtract(inward.scale(toTarget.dot(inward)));
        double offset = across.length();
        // In front of the middle of the doorway first, then through it: the posts of the gate are in the way otherwise
        Vec3 motion = offset > PULL_ALIGNED?across.scale(Math.min(offset, PULL_SPEED)/offset): inward.scale(PULL_SPEED).add(across);
        animal.setDeltaMovement(motion.x, animal.getDeltaMovement().y, motion.z);
    }

    /**
     * Called when a living entity jumps. Mobs jump whenever they walk inside a block that has a collision shape
     * (they take it for a step), which the blocks of the stalls have: the animals of a stall would hop about.
     */
    public static void onJump(LivingEntity entity)
    {
        if(entity.level().isClientSide||!entity.getPersistentData().contains(CONTROLLED))
            return;
        if(entity.level().getGameTime()-entity.getPersistentData().getLong(CONTROLLED) > 2)
            return;
        // The event comes after the jump: take its upward speed back
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, Math.min(motion.y, 0), motion.z);
    }

    /**
     * Keeps an animal still in its stall: at its place, facing the trough. It no longer walks or jumps of its own
     * accord (wandering, following food, fleeing), and only looks around a little.
     */
    private static void hold(Animal animal, Vec3 standing, float troughYaw)
    {
        control(animal);
        // The animal sets these flags back itself every few ticks (Mob#updateControlFlags), which is before the
        // machine ticks: they stay off as long as it is held, and nothing is left to undo when it is let go
        animal.goalSelector.disableControlFlag(Goal.Flag.MOVE);
        animal.goalSelector.disableControlFlag(Goal.Flag.JUMP);
        // Animals with a brain (goats, camels, ...) walk to this instead
        animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        double dx = standing.x-animal.getX();
        double dz = standing.z-animal.getZ();
        double distance = Math.sqrt(dx*dx+dz*dz);
        if(distance > HOLD_RADIUS)
        {
            // Just taken in, or pushed away
            animal.getMoveControl().setWantedPosition(standing.x, animal.getY(), standing.z, 0.8);
            return;
        }
        Vec3 motion = animal.getDeltaMovement();
        animal.setDeltaMovement(0, Math.min(motion.y, 0), 0);
        if(distance > 1e-3)
        {
            double step = Math.min(distance, HOLD_SPEED)/distance;
            animal.setPos(animal.getX()+dx*step, animal.getY(), animal.getZ()+dz*step);
        }
        animal.setYRot(Mth.approachDegrees(animal.getYRot(), troughYaw, 10));
        animal.setYBodyRot(Mth.approachDegrees(animal.yBodyRot, troughYaw, 10));
        animal.setYHeadRot(Mth.rotateIfNecessary(animal.getYHeadRot(), troughYaw, HOLD_HEAD_TURN));
    }
}
