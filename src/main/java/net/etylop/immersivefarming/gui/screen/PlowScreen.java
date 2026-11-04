package net.etylop.immersivefarming.gui.screen;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.gui.container.CartContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class PlowScreen extends AbstractContainerScreen<CartContainer> {
    private static final ResourceLocation PLOW_GUI_TEXTURES = new ResourceLocation(ImmersiveFarming.MOD_ID, "textures/gui/plow.png");

    public PlowScreen(final CartContainer screenContainer, final Inventory inv, final Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        final int i = (this.width - this.imageWidth) / 2;
        final int j = (this.height - this.imageHeight) / 2;
        graphics.blit(PLOW_GUI_TEXTURES, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
