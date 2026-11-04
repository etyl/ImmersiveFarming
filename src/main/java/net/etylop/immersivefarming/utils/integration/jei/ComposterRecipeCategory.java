package net.etylop.immersivefarming.utils.integration.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.api.crafting.ComposterRecipe;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidType;

public class ComposterRecipeCategory extends IFRecipeCategory<ComposterRecipe>
{
	public static final RecipeType<ComposterRecipe> TYPE = RecipeType.create(ImmersiveFarming.MOD_ID, "composter", ComposterRecipe.class);


	public ComposterRecipeCategory(IGuiHelper helper)
	{
		super(TYPE, helper, "block.immersivefarming.composter");
		setBackground(helper.createBlankDrawable(155, 60));
		setIcon(new ItemStack(IFMultiblockLogic.COMPOSTER.block().get()));

	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ComposterRecipe recipe, IFocusGroup focuses)
	{
		if (recipe.fluidProduct) {
			builder.addSlot(RecipeIngredientRole.INPUT, 10, 20)
					.addItemStack(recipe.itemInput.getMatchingStacks()[0])
					.setBackground(JEIHelper.slotDrawable, -1, -1);

			builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 10)
					.setFluidRenderer(FluidType.BUCKET_VOLUME/10, false, 16, 47)
					.addIngredient(ForgeTypes.FLUID_STACK, recipe.getFluidOutput()[0])
					.addTooltipCallback(blusunrize.immersiveengineering.common.util.compat.jei.JEIHelper.fluidTooltipCallback)
					.setBackground(JEIHelper.fluidDrawable, -2, -2)
					.setOverlay(JEIHelper.fluidOverlay, -2, -2);

			builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 10)
					.setFluidRenderer(FluidType.BUCKET_VOLUME/10, false, 16, 47)
					.addIngredient(ForgeTypes.FLUID_STACK, recipe.getFluidOutput()[1])
					.addTooltipCallback(blusunrize.immersiveengineering.common.util.compat.jei.JEIHelper.fluidTooltipCallback)
					.setBackground(JEIHelper.fluidDrawable, -2, -2)
					.setOverlay(JEIHelper.fluidOverlay, -2, -2);
		}

		else {
			builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 20)
					.addItemStack(recipe.itemOutput.getMatchingStacks()[0])
					.setBackground(JEIHelper.slotDrawable, -1, -1);
		}


	}

	@Override
	public void draw(ComposterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
	{
		PoseStack transform = graphics.pose();
		transform.pushPose();
		transform.scale(3f, 3f, 1);
		this.getIcon().draw(graphics, 14, 2);
		transform.popPose();
	}
}