package net.etylop.immersivefarming.mixin;

import blusunrize.immersiveengineering.common.util.compat.jei.cloche.ClocheRecipeCategory;
import net.etylop.immersivefarming.config.IFConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Set;

/**
 * JEI cloche recipes: IE lists the fertilizer slot as [nothing, fertilizers...].
 * When a fertilizer is required, "nothing" is no longer an option, so it is removed from the list.
 */
@Mixin(value = ClocheRecipeCategory.class, remap = false)
public abstract class ClocheRecipeCategoryMixin {

    @Redirect(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lblusunrize/immersiveengineering/api/crafting/ClocheRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Collections;singleton(Ljava/lang/Object;)Ljava/util/Set;"))
    private Set<Object> immersivefarming$noEmptyFertilizer(Object empty) {
        return IFConfig.getSynced(IFConfig.get().cloche.require_fertilizer) ? Collections.emptySet() : Collections.singleton(empty);
    }
}
