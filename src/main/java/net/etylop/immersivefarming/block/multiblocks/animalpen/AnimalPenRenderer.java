package net.etylop.immersivefarming.block.multiblocks.animalpen;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelperMaster;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockBlockEntityMaster;
import blusunrize.immersiveengineering.client.render.tile.IEBlockEntityRenderer;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.etylop.immersivefarming.block.multiblocks.GateBars;
import net.etylop.immersivefarming.block.utils.IFDynamicModel;
import net.etylop.immersivefarming.client.FeedColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Moving parts of the animal pen: the gate, the food in the trough, and the arcs between the two coils when an
 * animal is slaughtered.
 * The first two are objects of the model of the multiblock (see {@link AnimalPenMultiblock#GATE}), where they
 * are at rest: gate closed, trough full. Each has its own model, which shows this object only, and is moved from
 * there (see {@link GateBars} for the gate). Positions are in pixels, in multiblock coordinates.
 */
public class AnimalPenRenderer extends IEBlockEntityRenderer<MultiblockBlockEntityMaster<AnimalPenLogic.State>>
{
    private static IFDynamicModel GATE;
    private static IFDynamicModel FEED;
    private static GateBars BARS;

    private static final float PX = 1/16F;
    /** The bars of an open gate are stowed under the top of the machine (blocks) */
    private static final float GATE_TOP = 2;
    /** Height of the food in the trough, from nearly empty to full (as in the model) */
    private static final float FEED_MIN_Y = 6.5F, FEED_MAX_Y = 11;
    /** The tips of the two coils, which the arcs join through the animal standing in the stall */
    private static final Vector3f[] COIL_TIPS = {new Vector3f(14, 22.25F, 24), new Vector3f(34, 22.25F, 24)};
    private static final Vector3f ARC_MIDDLE = new Vector3f(24, 9, 18);
    /** Each arc is a broken line, which changes every other tick */
    private static final int ARCS = 3;
    private static final int ARC_POINTS = 9;
    private static final float ARC_SPREAD = 2.5F;
    /** A wide dim glow around a thin bright core: half width (pixels) and color of each */
    private static final float[][] ARC_LAYERS = {{0.7F, 0.3F, 0.29F, 0.6F, 0.45F}, {0.25F, 0.9F, 0.9F, 1, 0.9F}};

    /** Called before the models are registered */
    public static void initModels()
    {
        GATE = model(AnimalPenMultiblock.GATE);
        FEED = model(AnimalPenMultiblock.FEED);
    }

    private static IFDynamicModel model(String part)
    {
        return new IFDynamicModel("animal_pen_"+part);
    }

    @Override
    public void render(MultiblockBlockEntityMaster<AnimalPenLogic.State> be, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay)
    {
        final IMultiblockBEHelperMaster<AnimalPenLogic.State> helper = be.getHelper();
        final AnimalPenLogic.State state = helper.getState();
        final Direction facing = helper.getContext().getLevel().getOrientation().front();
        final VertexConsumer buffer = buffers.getBuffer(RenderType.solid());
        final BlockPos master = AnimalPenMultiblock.MASTER;

        pose.pushPose();
        // As the block model: relative to the master block, turned as the multiblock is
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);

        BARS = GateBars.of(BARS, GATE, GATE_TOP);
        BARS.render(pose, buffer, 1-Mth.lerp(partialTicks, state.prevGate, state.gate), light, overlay);

        ItemStack food = state.getDisplayFood();
        if(!food.isEmpty())
        {
            float fill = Mth.clamp(food.getCount()/(float)AnimalPenLogic.TROUGH_CAPACITY, 0, 1);
            pose.pushPose();
            pose.translate(0, (Mth.lerp(fill, FEED_MIN_Y, FEED_MAX_Y)-FEED_MAX_Y)*PX, 0);
            RenderUtils.renderModelTESRFast(FEED.getNullQuads(), buffer, pose, FeedColors.getColor(food), light, overlay);
            pose.popPose();
        }

        if(state.arcTicks > 0)
        {
            // From pixels in the multiblock to the model
            pose.pushPose();
            pose.translate(-master.getX(), -master.getY(), -master.getZ());
            pose.scale(PX, PX, PX);
            Level level = be.getLevel();
            long time = level==null?0: level.getGameTime()/2;
            RandomSource random = RandomSource.create(be.getBlockPos().asLong()^(time*0x9E3779B97F4A7C15L));
            // They die down at the end
            float strength = Math.min(1, (state.arcTicks-partialTicks)/4);
            renderArcs(pose.last().pose(), buffers.getBuffer(RenderType.lightning()), random, strength);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderArcs(Matrix4f matrix, VertexConsumer buffer, RandomSource random, float strength)
    {
        Vector3f[] points = new Vector3f[ARC_POINTS];
        for(int arc = 0; arc < ARCS; arc++)
        {
            // From a coil to the other, by way of the animal: the further from the coils, the more it strays
            for(int i = 0; i < ARC_POINTS; i++)
            {
                float along = i/(float)(ARC_POINTS-1);
                float sag = 1-Math.abs(2*along-1);
                Vector3f point = new Vector3f(COIL_TIPS[0]).lerp(COIL_TIPS[1], along);
                point.y = Mth.lerp(sag*sag, point.y, ARC_MIDDLE.y);
                point.z = Mth.lerp(sag, point.z, ARC_MIDDLE.z);
                float spread = ARC_SPREAD*Math.min(1, 3*sag);
                point.add(0, (random.nextFloat()-0.5F)*2*spread, (random.nextFloat()-0.5F)*2*spread);
                points[i] = point;
            }
            for(float[] layer : ARC_LAYERS)
                for(int i = 0; i < ARC_POINTS-1; i++)
                    renderBeam(matrix, buffer, points[i], points[i+1], layer[0], layer[1], layer[2], layer[3], layer[4]*strength);
        }
    }

    /** A segment of an arc: two flat strips across each other, seen from any side */
    private static void renderBeam(Matrix4f matrix, VertexConsumer buffer, Vector3f from, Vector3f to, float halfWidth,
                                   float red, float green, float blue, float alpha)
    {
        Vector3f along = new Vector3f(to).sub(from);
        if(along.lengthSquared() < 1e-6F)
            return;
        // Two directions across the segment, and across each other
        Vector3f side = Math.abs(along.y) < 0.9F*along.length()?new Vector3f(0, 1, 0): new Vector3f(1, 0, 0);
        Vector3f first = new Vector3f(along).cross(side).normalize(halfWidth);
        Vector3f second = new Vector3f(along).cross(first).normalize(halfWidth);
        for(Vector3f across : new Vector3f[]{first, second})
        {
            Vector3f a = new Vector3f(from).sub(across);
            Vector3f b = new Vector3f(from).add(across);
            Vector3f c = new Vector3f(to).add(across);
            Vector3f d = new Vector3f(to).sub(across);
            // Both faces
            for(Vector3f corner : new Vector3f[]{a, b, c, d, d, c, b, a})
                buffer.vertex(matrix, corner.x, corner.y, corner.z).color(red, green, blue, alpha).endVertex();
        }
    }
}
