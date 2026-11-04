package net.etylop.immersivefarming.gui.screen;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.info.EnergyInfoArea;
import blusunrize.immersiveengineering.client.gui.info.FluidInfoArea;
import blusunrize.immersiveengineering.client.gui.info.InfoArea;
import com.google.common.collect.ImmutableList;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterLogic;
import net.etylop.immersivefarming.gui.container.ComposterContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;
import java.util.List;

public final class ComposterScreen extends IEContainerScreen<ComposterContainer>
{
	private static final ResourceLocation TEXTURE = new ResourceLocation(ImmersiveFarming.MOD_ID, "textures/gui/composter.png");

	public ComposterScreen(ComposterContainer container, Inventory inventoryPlayer, Component component)
	{
		super(container, inventoryPlayer, component, TEXTURE);
	}

	@Nonnull
	@Override
	protected List<InfoArea> makeInfoAreas()
	{
		return ImmutableList.of(
				new EnergyInfoArea(leftPos+157, topPos+21, menu.energy),
				new FluidInfoArea(menu.tanks[0], new Rect2i(leftPos+109, topPos+20, 16, 47), 177, 31, 20, 51, TEXTURE),
				new FluidInfoArea(menu.tanks[1], new Rect2i(leftPos+82, topPos+20, 16, 47), 177, 31, 20, 51, TEXTURE),
				new FluidInfoArea(menu.tanks[2], new Rect2i(leftPos+55, topPos+20, 16, 47), 177, 31, 20, 51, TEXTURE)
		);
	}

	@Override
	protected void drawContainerBackgroundPre(@Nonnull GuiGraphics graphics, float f, int mx, int my)
	{
		final float progress = menu.progress.get();
		if(progress >= 0)
		{
			final int slot = ComposterLogic.INPUT_SLOT;
			final int h = (int)Math.max(1, progress*16);
			graphics.blit(TEXTURE, leftPos+24+slot%2*21, topPos+7+slot/2*18+(16-h), 176, 16-h, 2, h);
		}
	}
}
