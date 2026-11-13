package net.etylop.immersivefarming.block.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Loaded sprinklers of each server level, by chunk, so that soil can find the sprinklers watering it
 * without scanning the block entities of the surrounding chunks.
 * Sprinklers register themselves when loaded and unregister when removed or unloaded.
 * Only accessed from the server thread.
 */
public final class SprinklerRegistry {
    /** Soil watered when it is at the height of the sprinkler's bottom block, or at most this many blocks below it */
    public static final int MAX_DEPTH = 6;

    public static final int NONE = 0;
    public static final int WATER = 1;
    public static final int TREATED_WATER = 2;

    private static final Map<LevelAccessor, Long2ObjectMap<Set<SprinklerBlockEntity>>> SPRINKLERS = new IdentityHashMap<>();

    private SprinklerRegistry() {
    }

    static void add(SprinklerBlockEntity sprinkler) {
        SPRINKLERS.computeIfAbsent(sprinkler.getLevel(), level -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(ChunkPos.asLong(sprinkler.getBlockPos()), chunk -> new HashSet<>())
                .add(sprinkler);
    }

    static void remove(SprinklerBlockEntity sprinkler) {
        Long2ObjectMap<Set<SprinklerBlockEntity>> chunks = SPRINKLERS.get(sprinkler.getLevel());
        if (chunks == null) return;
        long chunk = ChunkPos.asLong(sprinkler.getBlockPos());
        Set<SprinklerBlockEntity> sprinklers = chunks.get(chunk);
        if (sprinklers == null) return;
        sprinklers.remove(sprinkler);
        if (sprinklers.isEmpty()) chunks.remove(chunk);
    }

    /** Forget all the sprinklers of a level being unloaded */
    public static void clear(LevelAccessor level) {
        SPRINKLERS.remove(level);
    }

    /**
     * The best active sprinkler watering the soil at this position
     * @return {@link #NONE}, {@link #WATER}, or {@link #TREATED_WATER} if a sprinkler using treated water reaches it
     */
    public static int getWatering(LevelAccessor level, BlockPos soil) {
        Long2ObjectMap<Set<SprinklerBlockEntity>> chunks = SPRINKLERS.get(level);
        if (chunks == null || chunks.isEmpty()) return NONE;

        int result = NONE;
        int chunkX = SectionPos.blockToSectionCoord(soil.getX());
        int chunkZ = SectionPos.blockToSectionCoord(soil.getZ());
        int chunkRange = SectionPos.blockToSectionCoord(SprinklerExtendedBlockEntity.RADIUS + 15);
        for (int x = chunkX - chunkRange; x <= chunkX + chunkRange; x++) {
            for (int z = chunkZ - chunkRange; z <= chunkZ + chunkRange; z++) {
                Set<SprinklerBlockEntity> sprinklers = chunks.get(ChunkPos.asLong(x, z));
                if (sprinklers == null) continue;
                for (SprinklerBlockEntity sprinkler : sprinklers) {
                    if (!sprinkler.isWatering(soil)) continue;
                    if (sprinkler.isUsingTreatedWater()) return TREATED_WATER;
                    result = WATER;
                }
            }
        }
        return result;
    }
}
