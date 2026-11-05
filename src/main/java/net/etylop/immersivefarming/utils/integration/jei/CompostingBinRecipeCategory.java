package net.etylop.immersivefarming.utils.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.entity.CompostingBinBlockEntity;
import net.etylop.immersivefarming.fluid.IFFluids;
import net.etylop.immersivefarming.item.IFItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * The Composting Bin: wet + dry matter -> compost, turned with a shovel during the process.
 * The matter values of items are shown by the Industrial Composter category (click on a fluid).
 */
public class CompostingBinRecipeCategory extends IFRecipeCategory<CompostingBinRecipeCategory.BinRecipe>
{
	public static final RecipeType<BinRecipe> TYPE = RecipeType.create(ImmersiveFarming.MOD_ID, "composting_bin", BinRecipe.class);

	private static final int WIDTH = 155;
	private static final int HEIGHT = 60;
	private static final int TANK_Y = 6;
	private static final int SLOT_Y = 22;
	private static final int ARROW_X = 66;
	private static final int TEXT_COLOR = 0xFF808080;

	private final IDrawableAnimated arrow;

	public CompostingBinRecipeCategory(IGuiHelper helper)
	{
		super(TYPE, helper, "block.immersivefarming.composting_bin");
		setBackground(helper.createBlankDrawable(WIDTH, HEIGHT));
		setIcon(new ItemStack(IFBlocks.COMPOSTING_BIN.get()));
		this.arrow = helper.createAnimatedRecipeArrow(40);
	}

	/**
	 * The single bin recipe
	 */
	public record BinRecipe(int matter, int output, int turns)
	{
		public static BinRecipe create()
		{
			return new BinRecipe(CompostingBinBlockEntity.MATTER_CAPACITY, CompostingBinBlockEntity.COMPOST_OUTPUT, CompostingBinBlockEntity.TURNS);
		}
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, BinRecipe recipe, IFocusGroup focuses)
	{
		addTank(builder, 16, new FluidStack(IFFluids.WET_MATTER_FLUID.get(), recipe.matter()), recipe.matter());
		addTank(builder, 40, new FluidStack(IFFluids.DRY_MATTER_FLUID.get(), recipe.matter()), recipe.matter());

		// The shovel used to turn the compost
		builder.addSlot(RecipeIngredientRole.CATALYST, ARROW_X+24, 2)
				.addItemStacks(List.of(new ItemStack(Items.WOODEN_SHOVEL), new ItemStack(Items.STONE_SHOVEL),
						new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.GOLDEN_SHOVEL),
						new ItemStack(Items.DIAMOND_SHOVEL), new ItemStack(Items.NETHERITE_SHOVEL)))
				.setBackground(JEIHelper.slotDrawable, -1, -1);

		builder.addSlot(RecipeIngredientRole.OUTPUT, 126, SLOT_Y)
				.addItemStack(new ItemStack(IFItems.COMPOST.get(), recipe.output()))
				.setBackground(JEIHelper.slotDrawable, -1, -1);
	}

	private static void addTank(IRecipeLayoutBuilder builder, int x, FluidStack fluid, int capacity)
	{
		builder.addSlot(RecipeIngredientRole.INPUT, x, TANK_Y)
				.setFluidRenderer(Math.max(1, capacity), false, 16, 47)
				.setBackground(JEIHelper.fluidDrawable, -2, -2)
				.setOverlay(JEIHelper.fluidOverlay, -2, -2)
				.addIngredient(ForgeTypes.FLUID_STACK, fluid)
				.addTooltipCallback(JEIHelper.fluidTooltipCallback);
	}

	@Override
	public void draw(BinRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
	{
		this.arrow.draw(graphics, ARROW_X+20, SLOT_Y);
		Font font = Minecraft.getInstance().font;
		int centerX = ARROW_X+32;
		drawCentered(graphics, font, Component.translatable("jei.immersivefarming.composting_bin.turns", recipe.turns()), centerX, SLOT_Y+24);
	}

	private static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y)
	{
		graphics.drawString(font, text, centerX-font.width(text)/2, y, TEXT_COLOR, false);
	}

	static ItemStack catalyst()
	{
		return new ItemStack(IFBlocks.COMPOSTING_BIN.get());
	}

	static void registerInfo(mezz.jei.api.registration.IRecipeRegistration registration)
	{
		BinRecipe recipe = BinRecipe.create();
		registration.addRecipes(TYPE, List.of(recipe));
		registration.addIngredientInfo(catalyst(), VanillaTypes.ITEM_STACK,
				Component.translatable("jei.immersivefarming.composting_bin.info", recipe.matter(), recipe.output(), recipe.turns()));
	}
}
