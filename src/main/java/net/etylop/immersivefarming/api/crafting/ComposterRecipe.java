package net.etylop.immersivefarming.api.crafting;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.common.collect.Lists;
import net.etylop.immersivefarming.crafting.IFRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;


public class ComposterRecipe extends IFMultiblockRecipe
{
	public static RecipeType<ComposterRecipe> TYPE;
	public static final RegistryObject<IERecipeSerializer<ComposterRecipe>> SERIALIZER = IFRecipeSerializer.COMPOSTER_SERIALIZER;
	public static final IFCachedRecipeList<ComposterRecipe> RECIPES = new IFCachedRecipeList<>(() -> TYPE, ComposterRecipe.class);

	public final boolean fluidProduct; // if the recipe produces fluids

	private FluidStack[] fluidOutputs = new FluidStack[2];
	private final FluidTagInput[] fluidInputs = new FluidTagInput[3];
	public final IngredientWithSize itemInput;
	public final IngredientWithSize itemOutput;
	public final int energy; // value from the recipe json, needed to sync the recipe to clients

	public ComposterRecipe(ResourceLocation id, FluidStack fluidNitrogen, FluidStack fluidCarbon, TagKey<Item> itemInput, int energy)
	{
		this(id, fluidNitrogen, fluidCarbon, new IngredientWithSize(itemInput, 1), energy);
	}

	public ComposterRecipe(ResourceLocation id, FluidStack fluidNitrogen, FluidStack fluidCarbon, IngredientWithSize itemInput, int energy)
	{
		super(ItemStack.EMPTY, IFRecipeTypes.COMPOSTER, id);
		this.fluidProduct = true;

		this.fluidOutputs[0] = fluidNitrogen;
		this.fluidOutputs[1] = fluidCarbon;
		this.itemInput = itemInput;
		this.itemOutput = null;
		this.energy = energy;

		timeAndEnergy(energy, energy);
		this.fluidOutputList = Lists.newArrayList(fluidNitrogen, fluidCarbon);
		setInputListWithSizes(Lists.newArrayList(this.itemInput));
	}

	public ComposterRecipe(ResourceLocation id, FluidTagInput fluidWater, FluidTagInput fluidNitrogen, FluidTagInput fluidCarbon, IngredientWithSize itemOutput, int energy) {
		super(ItemStack.EMPTY, IFRecipeTypes.COMPOSTER, id);
		this.fluidProduct = false;

		this.fluidInputs[0] = fluidWater;
		this.fluidInputs[1] = fluidNitrogen;
		this.fluidInputs[2] = fluidCarbon;
		this.itemOutput = itemOutput;
		this.itemInput = null;
		this.energy = energy;

		timeAndEnergy(100, energy);
		this.fluidInputList = Lists.newArrayList(fluidWater, fluidNitrogen, fluidCarbon);
	}

	@Override
	protected IERecipeSerializer<ComposterRecipe> getIESerializer()
	{
		return SERIALIZER.get();
	}

	// TODO rewrite this
	public static ComposterRecipe findRecipe(Level level, FluidStack[] fluids, ItemStack component)
	{
		if (fluids.length != 3)
			return null;

		for(ComposterRecipe recipe : RECIPES.getRecipes(level)) {
			if (!recipe.fluidProduct)
				continue;
			if (recipe.matches(component) || recipe.matches(fluids))
				return recipe;
		}
		for(ComposterRecipe recipe : RECIPES.getRecipes(level)) {
			if (recipe.fluidProduct)
				continue;
			if (recipe.matches(fluids) || recipe.matches(component))
				return recipe;
		}
		return null;
	}

	public FluidStack[] getFluidOutput()
	{
		return this.fluidOutputs;
	}

	public FluidTagInput[] getFluidInput() {
		return this.fluidInputs;
	}

	public boolean matches(FluidStack[] fluids) {
		if (this.fluidProduct)
			return false;
		for (int i=0; i<3; i++) {
			if (fluids[i].getAmount() < this.fluidInputs[i].getAmount())
				return false;
		}
		return true;
	}

	public boolean matches(ItemStack component) {
		if (!this.fluidProduct)
			return false;
        return itemInput.test(component);
    }

	@Override
	public int getMultipleProcessTicks()
	{
		return 0;
	}

	@Override
	public boolean shouldCheckItemAvailability()
	{
		return false;
	}
}