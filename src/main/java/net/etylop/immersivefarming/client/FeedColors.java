package net.etylop.immersivefarming.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.etylop.immersivefarming.block.custom.FeedingTroughBlock;
import net.etylop.immersivefarming.block.entity.FeedingTroughBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Colors of the food in feeding troughs: the content model uses a grey texture, tinted with the average color
 * of the food item's texture (the most abundant food, see FeedingTroughBlockEntity#getDisplayFood).
 */
public final class FeedColors {
    /** Very dark colors are brightened up to this brightest channel, so that the grey texture keeps its contrast */
    private static final int MIN_BRIGHTNESS = 200;
    private static final Map<Item, Integer> CACHE = new ConcurrentHashMap<>();

    private FeedColors() {
    }

    /**
     * Tint index of the content faces. Not 0: block breaking, hitting and running particles are tinted with the color
     * of index 0, and they use the wood texture, which must stay untinted.
     */
    public static final int FEED_TINT_INDEX = 1;

    /** Block color handler: the content of both halves takes the color held by the main half */
    public static int getBlockColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex != FEED_TINT_INDEX) return -1;
        if (level == null || pos == null || !(state.getBlock() instanceof FeedingTroughBlock)) return FeedingTroughBlockEntity.DEFAULT_FEED_COLOR;
        return level.getBlockEntity(FeedingTroughBlock.getMainPos(pos, state)) instanceof FeedingTroughBlockEntity trough
                ? trough.getDisplayColor() : FeedingTroughBlockEntity.DEFAULT_FEED_COLOR;
    }

    /** The color of a food item, computed once per item (main thread only: it reads the item's texture) */
    public static int getColor(ItemStack stack) {
        return CACHE.computeIfAbsent(stack.getItem(), item -> compute(stack));
    }

    /** Forget the colors when the resources are reloaded (resource packs may change the textures) */
    public static void clear() {
        CACHE.clear();
    }

    private static int compute(ItemStack stack) {
        try {
            Minecraft mc = Minecraft.getInstance();
            TextureAtlasSprite sprite = mc.getItemRenderer().getModel(stack, null, null, 0).getParticleIcon(ModelData.EMPTY);
            SpriteContents contents = sprite.contents();
            NativeImage image = contents.getOriginalImage();
            long red = 0, green = 0, blue = 0, count = 0;
            // The first frame of animated textures
            for (int x = 0; x < contents.width(); x++) {
                for (int y = 0; y < contents.height(); y++) {
                    int pixel = image.getPixelRGBA(x, y); // ABGR
                    if ((pixel >>> 24) < 128) continue;
                    red += pixel & 0xFF;
                    green += (pixel >> 8) & 0xFF;
                    blue += (pixel >> 16) & 0xFF;
                    count++;
                }
            }
            if (count == 0) return FeedingTroughBlockEntity.DEFAULT_FEED_COLOR;
            int r = (int) (red / count), g = (int) (green / count), b = (int) (blue / count);

            // Items tinted in code (dyed, potions...)
            int tint = mc.getItemColors().getColor(stack, 0);
            if (tint != -1) {
                r = r * ((tint >> 16) & 0xFF) / 255;
                g = g * ((tint >> 8) & 0xFF) / 255;
                b = b * (tint & 0xFF) / 255;
            }

            // Brighten without changing the hue: item textures have dark outlines, which darken the average
            int max = Math.max(r, Math.max(g, b));
            if (max > 0 && max < MIN_BRIGHTNESS) {
                r = r * MIN_BRIGHTNESS / max;
                g = g * MIN_BRIGHTNESS / max;
                b = b * MIN_BRIGHTNESS / max;
            }
            return (r << 16) | (g << 8) | b;
        } catch (RuntimeException e) {
            return FeedingTroughBlockEntity.DEFAULT_FEED_COLOR;
        }
    }
}
