package net.etylop.immersivefarming.block.multiblocks.breeder;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelperMaster;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockBlockEntityMaster;
import blusunrize.immersiveengineering.client.render.tile.IEBlockEntityRenderer;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.etylop.immersivefarming.block.utils.IFDynamicModel;
import net.etylop.immersivefarming.client.FeedColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Moving parts of the animal breeder: the two gates, the strip curtain of the hatch, and the food in the trough.
 * They are objects of the model of the multiblock (see {@link BreederMultiblock#GATES}), where they are at rest:
 * gates closed, curtain hanging, trough full. Each has its own model, which shows this object only, and is moved
 * from there. Heights are in pixels.
 */
public class BreederRenderer extends IEBlockEntityRenderer<MultiblockBlockEntityMaster<BreederLogic.State>>
{
    private static final IFDynamicModel[] GATES = new IFDynamicModel[BreederLogic.STALLS];
    private static final Bars[] BARS = new Bars[BreederLogic.STALLS];
    private static IFDynamicModel CURTAIN;
    private static IFDynamicModel FEED;

    private static final float PX = 1/16F;
    /** The bars of an open gate are stowed under the top of the machine (blocks) */
    private static final float GATE_TOP = 2;
    /** The curtain of the hatch hangs from its top edge */
    private static final float CURTAIN_Y = BreederShapes.HATCH_HEIGHT, CURTAIN_Z = 0.5F;
    private static final float CURTAIN_ANGLE = 65;
    /** Height of the food in the trough, from nearly empty to full (as in the model) */
    private static final float FEED_MIN_Y = 6.5F, FEED_MAX_Y = 11;

    /** Called before the models are registered */
    public static void initModels()
    {
        for(int stall = 0; stall < GATES.length; stall++)
            GATES[stall] = model(BreederMultiblock.GATES[stall]);
        CURTAIN = model(BreederMultiblock.CURTAIN);
        FEED = model(BreederMultiblock.FEED);
    }

    private static IFDynamicModel model(String part)
    {
        return new IFDynamicModel("breeder_"+part);
    }

    @Override
    public void render(MultiblockBlockEntityMaster<BreederLogic.State> be, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay)
    {
        final IMultiblockBEHelperMaster<BreederLogic.State> helper = be.getHelper();
        final BreederLogic.State state = helper.getState();
        final Direction facing = helper.getContext().getLevel().getOrientation().front();
        final VertexConsumer buffer = buffers.getBuffer(RenderType.solid());

        pose.pushPose();
        // As the block model: relative to the master block, turned as the multiblock is
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);

        for(int stall = 0; stall < BreederLogic.STALLS; stall++)
        {
            // Each bar slides up on its own, none changes shape
            float open = 1-Mth.lerp(partialTicks, state.prevGate[stall], state.gate[stall]);
            Bars bars = getBars(stall);
            for(int bar = 0; bar < bars.quads.size(); bar++)
            {
                pose.pushPose();
                pose.translate(0, bars.rise[bar]*open, 0);
                RenderUtils.renderModelTESRFast(bars.quads.get(bar), buffer, pose, light, overlay);
                pose.popPose();
            }
        }

        // Pushed open from the inside, around its top edge
        BlockPos master = BreederMultiblock.MASTER;
        float hingeY = CURTAIN_Y*PX-master.getY();
        float hingeZ = CURTAIN_Z*PX-master.getZ();
        pose.pushPose();
        pose.translate(0, hingeY, hingeZ);
        pose.mulPose(Axis.XP.rotationDegrees(CURTAIN_ANGLE*Mth.lerp(partialTicks, state.prevFlap, state.flap)));
        pose.translate(0, -hingeY, -hingeZ);
        RenderUtils.renderModelTESRFast(CURTAIN.getNullQuads(), buffer, pose, light, overlay);
        pose.popPose();

        ItemStack food = state.getDisplayFood();
        if(!food.isEmpty())
        {
            float fill = Mth.clamp(food.getCount()/(float)BreederLogic.TROUGH_CAPACITY, 0, 1);
            pose.pushPose();
            pose.translate(0, (Mth.lerp(fill, FEED_MIN_Y, FEED_MAX_Y)-FEED_MAX_Y)*PX, 0);
            RenderUtils.renderModelTESRFast(FEED.getNullQuads(), buffer, pose, FeedColors.getColor(food), light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The bars of a gate, worked out again when the models are reloaded */
    private static Bars getBars(int stall)
    {
        BakedModel model = GATES[stall].get();
        Bars bars = BARS[stall];
        if(bars==null||bars.model!=model)
        {
            bars = Bars.of(model, GATES[stall].getNullQuads());
            BARS[stall] = bars;
        }
        return bars;
    }

    /**
     * A gate is a single object of the model, made of several bars: whatever is apart from the rest in height.
     * When the gate opens, the bars are stacked under the top of the machine, the highest first.
     *
     * @param quads the faces of each bar, from the lowest to the highest
     * @param rise how far each bar goes up when the gate opens (blocks)
     */
    private record Bars(BakedModel model, List<List<BakedQuad>> quads, float[] rise)
    {
        private static final float GAP = 1e-3F;

        static Bars of(BakedModel model, List<BakedQuad> all)
        {
            List<BakedQuad> sorted = new ArrayList<>(all);
            sorted.sort(Comparator.comparingDouble(Bars::minY));
            List<List<BakedQuad>> quads = new ArrayList<>();
            List<float[]> ranges = new ArrayList<>();
            for(BakedQuad quad : sorted)
            {
                float[] range = ranges.isEmpty()?null: ranges.get(ranges.size()-1);
                if(range==null||minY(quad) > range[1]+GAP)
                {
                    // Above everything seen so far: another bar
                    quads.add(new ArrayList<>());
                    range = new float[]{minY(quad), maxY(quad)};
                    ranges.add(range);
                }
                range[1] = Math.max(range[1], maxY(quad));
                quads.get(quads.size()-1).add(quad);
            }
            float[] rise = new float[quads.size()];
            float top = GATE_TOP;
            for(int bar = quads.size()-1; bar >= 0; bar--)
            {
                float[] range = ranges.get(bar);
                top -= range[1]-range[0];
                rise[bar] = top-range[0];
            }
            return new Bars(model, quads, rise);
        }

        private static float minY(BakedQuad quad)
        {
            float min = Float.MAX_VALUE;
            for(int vertex = 0; vertex < 4; vertex++)
                min = Math.min(min, y(quad, vertex));
            return min;
        }

        private static float maxY(BakedQuad quad)
        {
            float max = -Float.MAX_VALUE;
            for(int vertex = 0; vertex < 4; vertex++)
                max = Math.max(max, y(quad, vertex));
            return max;
        }

        /** Height of a corner of a face: baked vertices are 8 integers each, the position comes first */
        private static float y(BakedQuad quad, int vertex)
        {
            return Float.intBitsToFloat(quad.getVertices()[vertex*8+1]);
        }
    }
}
