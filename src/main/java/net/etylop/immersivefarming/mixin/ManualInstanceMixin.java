package net.etylop.immersivefarming.mixin;

import blusunrize.lib.manual.ManualInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Engineer's manual: the garden cloche has its own entry in the farming section (immersivefarming:garden_cloche),
 * so IE's original entry is removed from the machines section.
 * (Overriding IE's garden_cloche.txt from this mod's resources doesn't work: IE's own file takes precedence.)
 * Autoload files can only add entries, hence this mixin, which runs after every manual (re)load.
 */
@Mixin(value = ManualInstance.class, remap = false)
public abstract class ManualInstanceMixin {
    private static final ResourceLocation MACHINES = new ResourceLocation("immersiveengineering", "machines");
    private static final ResourceLocation GARDEN_CLOCHE = new ResourceLocation("immersiveengineering", "garden_cloche");

    @Inject(method = "loadAutoEntries", at = @At("TAIL"))
    private void immersivefarming$moveGardenCloche(CallbackInfo ci) {
        ManualInstance manual = (ManualInstance) (Object) this;
        if (!"immersiveengineering".equals(manual.getDefaultResourceDomain())) return;
        // getChildren() returns an immutable copy: find the entry in it, then remove it through the node
        manual.getRoot().getSubnode(MACHINES).ifPresent(machines -> machines.getChildren().stream()
                .filter(node -> node.isLeaf() && GARDEN_CLOCHE.equals(node.getLeafData().getLocation()))
                .map(node -> node.getLeafData())
                .toList()
                .forEach(machines::removeLeaf));
    }
}
