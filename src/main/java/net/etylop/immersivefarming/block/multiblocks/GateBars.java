package net.etylop.immersivefarming.block.multiblocks;

import blusunrize.immersiveengineering.client.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.etylop.immersivefarming.block.utils.IFDynamicModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The gate of a stall (client): a single object of the model of its machine, where it is closed, made of several
 * bars: whatever is apart from the rest in height. When the gate opens, each bar slides up on its own, none changes
 * shape: they are stacked under the top of the machine, the highest first.
 *
 * @param quads the faces of each bar, from the lowest to the highest
 * @param rise  how far each bar goes up when the gate opens (blocks)
 */
public record GateBars(BakedModel model, List<List<BakedQuad>> quads, float[] rise)
{
    private static final float GAP = 1e-3F;

    /**
     * The bars of a gate, worked out again when the models are reloaded.
     *
     * @param cached what this method returned the last time for this gate
     * @param top    the bars of an open gate are stowed under this height (blocks, as in the model)
     */
    public static GateBars of(@Nullable GateBars cached, IFDynamicModel gate, float top)
    {
        BakedModel model = gate.get();
        if(cached!=null&&cached.model==model)
            return cached;
        List<BakedQuad> sorted = new ArrayList<>(gate.getNullQuads());
        sorted.sort(Comparator.comparingDouble(GateBars::minY));
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
        for(int bar = quads.size()-1; bar >= 0; bar--)
        {
            float[] range = ranges.get(bar);
            top -= range[1]-range[0];
            rise[bar] = top-range[0];
        }
        return new GateBars(model, quads, rise);
    }

    /** @param open from 0 (closed) to 1 */
    public void render(PoseStack pose, VertexConsumer buffer, float open, int light, int overlay)
    {
        for(int bar = 0; bar < quads.size(); bar++)
        {
            pose.pushPose();
            pose.translate(0, rise[bar]*open, 0);
            RenderUtils.renderModelTESRFast(quads.get(bar), buffer, pose, light, overlay);
            pose.popPose();
        }
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
