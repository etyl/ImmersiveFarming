package net.etylop.immersivefarming.client.model;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.custom.CompostingBinBlock;
import net.etylop.immersivefarming.block.custom.FeedingTroughBlock;
import net.etylop.immersivefarming.client.FeedColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The content of the feeding trough (food) and of the composting bin (matter), drawn in code at a height depending on
 * the block state, on top of the block's JSON model (its structure only). JSON models can't change a height with the
 * block state: this replaces one JSON model per fill level, stage and side.
 * <p>The quads are baked as JSON model elements are (same coordinates, uvs and rotations as the blockstate's),
 * once per block state, the first time it is rendered after a resource reload.
 */
public final class ContentModels {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ResourceLocation NAME = new ResourceLocation(ImmersiveFarming.MOD_ID, "block/content");

    private static final ResourceLocation FEED = new ResourceLocation(ImmersiveFarming.MOD_ID, "block/feeding_trough_feed");
    /** Height of the food's surface, for fill levels 1 to 4 */
    private static final float[] FEED_HEIGHT = {3.5F, 5F, 6.5F, 7.5F};

    private static final ResourceLocation DRY_MATTER = new ResourceLocation("block/rooted_dirt");
    private static final ResourceLocation WET_MATTER = new ResourceLocation("block/composter_compost");
    private static final ResourceLocation COMPOST = new ResourceLocation("block/composter_compost");
    private static final ResourceLocation READY_COMPOST = new ResourceLocation("block/composter_ready");

    private ContentModels() {
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        wrap(event.getModels(), IFBlocks.FEEDING_TROUGH.get(), ContentModels::troughContent);
        wrap(event.getModels(), IFBlocks.COMPOSTING_BIN.get(), ContentModels::binContent);
    }

    private static void wrap(Map<ResourceLocation, BakedModel> models, Block block, Function<BlockState, List<BakedQuad>> content) {
        Map<BlockState, List<BakedQuad>> cache = new ConcurrentHashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = models.get(location);
            if (model != null) models.put(location, new ContentModel(model, state, content, cache));
        }
    }

    /** The food, in both halves of the trough: its surface only, tinted with the food's color (see FeedColors) */
    private static List<BakedQuad> troughContent(BlockState state) {
        int fill = state.getValue(FeedingTroughBlock.FILL);
        if (fill == 0) return List.of();
        float top = FEED_HEIGHT[fill - 1];
        Direction facing = state.getValue(FeedingTroughBlock.FACING);
        return List.of(quad(new Vector3f(3, 2, 0), new Vector3f(13, top, 11), Direction.UP, new float[]{3, 0, 13, 11},
                FeedColors.FEED_TINT_INDEX, FEED, facing));
    }

    /**
     * The matter in the lower half of the bin, while it is open: dry matter in the left half and wet matter in the right
     * half while filling, then compost. Its top and the side facing the other half are visible.
     */
    private static List<BakedQuad> binContent(BlockState state) {
        int level = state.getValue(CompostingBinBlock.LEVEL);
        CompostingBinBlock.Stage stage = state.getValue(CompostingBinBlock.STAGE);
        if (level == 0 || state.getValue(CompostingBinBlock.HALF) != DoubleBlockHalf.LOWER
                || stage == CompostingBinBlock.Stage.COMPOSTING) {
            return List.of();
        }
        boolean left = state.getValue(CompostingBinBlock.PART) == CompostingBinBlock.Part.LEFT;
        ResourceLocation texture = switch (stage) {
            case FILLING -> left ? DRY_MATTER : WET_MATTER;
            case READY -> READY_COMPOST;
            default -> COMPOST;
        };
        float top = 2 + 1.5F * level;
        Direction facing = state.getValue(CompostingBinBlock.FACING);
        Vector3f from = new Vector3f(left ? 0 : 2, 2, 2);
        Vector3f to = new Vector3f(left ? 14 : 16, top, 14);
        return List.of(
                quad(from, to, Direction.UP, new float[]{2, 2, 14, 14}, BlockElementFace.NO_TINT, texture, facing),
                quad(from, to, left ? Direction.WEST : Direction.EAST, new float[]{2, 16 - top, 14, 14}, BlockElementFace.NO_TINT, texture, facing)
        );
    }

    /**
     * One face of a box, as a JSON model element's face (coordinates and uvs in pixels), without ambient occlusion,
     * rotated as the blockstates rotate the block's model for this facing (north: 0°, east: 90°...)
     */
    private static BakedQuad quad(Vector3f from, Vector3f to, Direction face, float[] uv, int tintIndex,
                                  ResourceLocation texture, Direction facing) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(texture);
        BlockElementFace elementFace = new BlockElementFace(null, tintIndex, "", new BlockFaceUV(uv, 0));
        BlockModelRotation rotation = BlockModelRotation.by(0, (int) facing.getOpposite().toYRot());
        BakedQuad quad = BAKERY.bakeQuad(from, to, elementFace, sprite, face, rotation, null, true, NAME);
        return new BakedQuad(quad.getVertices(), quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), false);
    }

    /** A block state's model, plus its content */
    private static final class ContentModel extends BakedModelWrapper<BakedModel> {
        private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());

        private final BlockState state;
        private final Function<BlockState, List<BakedQuad>> content;
        private final Map<BlockState, List<BakedQuad>> cache;

        ContentModel(BakedModel model, BlockState state, Function<BlockState, List<BakedQuad>> content, Map<BlockState, List<BakedQuad>> cache) {
            super(model);
            this.state = state;
            this.content = content;
            this.cache = cache;
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                 @NotNull ModelData data, @Nullable RenderType renderType) {
            List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
            // The content is never culled, and drawn in the solid layer
            if (side != null || (renderType != null && renderType != RenderType.solid())) return quads;
            List<BakedQuad> content = this.cache.computeIfAbsent(state != null ? state : this.state, this.content);
            if (content.isEmpty()) return quads;
            List<BakedQuad> all = new ArrayList<>(quads.size() + content.size());
            all.addAll(quads);
            all.addAll(content);
            return all;
        }

        @Override
        public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return ChunkRenderTypeSet.union(super.getRenderTypes(state, rand, data), SOLID);
        }
    }
}
