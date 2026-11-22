package net.etylop.immersivefarming.block.multiblocks.breeder;

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
import net.minecraft.world.item.ItemStack;

/**
 * Moving parts of the animal breeder: the two gates, the strip curtain of the hatch, and the food in the trough.
 * They are objects of the model of the multiblock (see {@link BreederMultiblock#GATES}), where they are at rest:
 * gates closed, curtain hanging, trough full. Each has its own model, which shows this object only, and is moved
 * from there (see {@link GateBars} for the gates). Heights are in pixels.
 */
public class BreederRenderer extends IEBlockEntityRenderer<MultiblockBlockEntityMaster<BreederLogic.State>>
{
    private static final IFDynamicModel[] GATES = new IFDynamicModel[BreederLogic.STALLS];
    private static final GateBars[] BARS = new GateBars[BreederLogic.STALLS];
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
            float open = 1-Mth.lerp(partialTicks, state.prevGate[stall], state.gate[stall]);
            BARS[stall] = GateBars.of(BARS[stall], GATES[stall], GATE_TOP);
            BARS[stall].render(pose, buffer, open, light, overlay);
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
}
