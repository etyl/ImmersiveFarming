package net.etylop.immersivefarming.utils.integration.jei;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.api.crafting.ComposterRecipe;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.Arrays;
import java.util.List;

public class ComposterRecipeCategory extends IFRecipeCategory<ComposterRecipe>
{
	public static final RecipeType<ComposterRecipe> TYPE = RecipeType.create(ImmersiveFarming.MOD_ID, "composter", ComposterRecipe.class);

	private static final int WIDTH = 155;
	private static final int HEIGHT = 60;
	private static final int TANK_Y = 6;
	private static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME/10;
	private static final int SLOT_Y = 22;
	private static final int TEXT_COLOR = 0xFF808080;

	// x positions of the arrow (24x17) for each layout
	private static final int FLUID_PRODUCT_ARROW_X = 56;
	private static final int ITEM_PRODUCT_ARROW_X = 86;

	private final IDrawableAnimated arrow;

	public ComposterRecipeCategory(IGuiHelper helper)
	{
		super(TYPE, helper, "block.immersivefarming.composter");
		setBackground(helper.createBlankDrawable(WIDTH, HEIGHT));
		setIcon(new ItemStack(IFMultiblockLogic.COMPOSTER.block().get()));
		this.arrow = helper.createAnimatedRecipeArrow(40);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ComposterRecipe recipe, IFocusGroup focuses)
	{
		if (recipe.fluidProduct) {
			// organic matter -> wet + dry matter
			builder.addSlot(RecipeIngredientRole.INPUT, 24, SLOT_Y)
					.addItemStacks(Arrays.asList(recipe.itemInput.getMatchingStacks()))
					.setBackground(JEIHelper.slotDrawable, -1, -1);

			FluidStack[] outputs = recipe.getFluidOutput();
			addTank(builder, RecipeIngredientRole.OUTPUT, 104, outputs[0]==null||outputs[0].isEmpty() ? List.of() : List.of(outputs[0]));
			addTank(builder, RecipeIngredientRole.OUTPUT, 130, outputs[1]==null||outputs[1].isEmpty() ? List.of() : List.of(outputs[1]));
		}
		else {
			// water + wet matter + dry matter -> compost
			FluidTagInput[] inputs = recipe.getFluidInput();
			for (int i = 0; i < inputs.length; i++)
				addTank(builder, RecipeIngredientRole.INPUT, 8 + 24*i, inputs[i]==null ? List.of() : inputs[i].getMatchingFluidStacks());

			builder.addSlot(RecipeIngredientRole.OUTPUT, 126, SLOT_Y)
					.addItemStacks(Arrays.asList(recipe.itemOutput.getMatchingStacks()))
					.setBackground(JEIHelper.slotDrawable, -1, -1);
		}
	}

	private static void addTank(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, List<FluidStack> fluids)
	{
		IRecipeSlotBuilder slot = builder.addSlot(role, x, TANK_Y)
				.setFluidRenderer(TANK_CAPACITY, false, 16, 47)
				.setBackground(JEIHelper.fluidDrawable, -2, -2)
				.setOverlay(JEIHelper.fluidOverlay, -2, -2);
		// JEI crashes when normalizing an empty FluidStack, so leave the tank empty instead
		List<FluidStack> nonEmpty = fluids.stream().filter(f -> f!=null&&!f.isEmpty()).toList();
		if (!nonEmpty.isEmpty())
			slot.addIngredients(ForgeTypes.FLUID_STACK, nonEmpty)
					.addTooltipCallback(JEIHelper.fluidTooltipCallback);
	}

	@Override
	public void draw(ComposterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
	{
		int arrowX = recipe.fluidProduct ? FLUID_PRODUCT_ARROW_X : ITEM_PRODUCT_ARROW_X;
		this.arrow.draw(graphics, arrowX, SLOT_Y);

		Font font = Minecraft.getInstance().font;
		int centerX = arrowX + 12;
		drawCentered(graphics, font, Component.translatable("jei.immersivefarming.energy", recipe.getTotalProcessEnergy()), centerX, SLOT_Y + 22);
		String seconds = String.format("%.1f", recipe.getTotalProcessTime()/20f);
		drawCentered(graphics, font, Component.translatable("jei.immersivefarming.time", seconds), centerX, SLOT_Y + 32);
	}

	private static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y)
	{
		graphics.drawString(font, text, centerX - font.width(text)/2, y, TEXT_COLOR, false);
	}
}
