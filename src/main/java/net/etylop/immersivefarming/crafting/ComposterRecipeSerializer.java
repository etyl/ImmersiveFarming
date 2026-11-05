package net.etylop.immersivefarming.crafting;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import net.etylop.immersivefarming.api.crafting.ComposterRecipe;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class ComposterRecipeSerializer extends IERecipeSerializer<ComposterRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return new ItemStack(IFMultiblockLogic.COMPOSTER.block().get());
	}

	@Override
	public ComposterRecipe readFromJson(ResourceLocation recipeId, JsonObject json, IContext context)
	{
		boolean fluidProduct = GsonHelper.getAsBoolean(json, "fluidProduct");
		ComposterRecipe recipe;
		if (fluidProduct) {

			FluidStack[] fluidOutputs = new FluidStack[2];
			fluidOutputs[0] = ApiUtils.jsonDeserializeFluidStack(GsonHelper.getAsJsonObject(json, "output0"));
			fluidOutputs[1] = ApiUtils.jsonDeserializeFluidStack(GsonHelper.getAsJsonObject(json, "output1"));
			int energy = GsonHelper.getAsInt(json, "energy");
			if (json.has("input")) {
				IngredientWithSize input = IngredientWithSize.deserialize(json.get("input"));
				recipe = new ComposterRecipe(recipeId, fluidOutputs[0], fluidOutputs[1], input, energy);
			}
			else {
				String inputString = GsonHelper.getAsJsonObject(json, "inputTag").get("tag").getAsString();
				TagKey<Item> input = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(inputString));
				recipe = new ComposterRecipe(recipeId, fluidOutputs[0], fluidOutputs[1], input, energy);
			}

		}
		else {
			FluidTagInput[] fluidInputs = new FluidTagInput[3];
			fluidInputs[0] = FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "input0"));
			fluidInputs[1] = FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "input1"));
			fluidInputs[2] = FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "input2"));
			IngredientWithSize output = IngredientWithSize.deserialize(json.get("result"));
			int energy = GsonHelper.getAsInt(json, "energy");
			recipe = new ComposterRecipe(recipeId, fluidInputs[0], fluidInputs[1], fluidInputs[2], output, energy);
		}
		return recipe;
	}

	@Nullable
	@Override
	public ComposterRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer)
	{
		boolean fluidProduct = buffer.readBoolean();
		int energy = buffer.readInt();
		if (fluidProduct) {
			FluidStack wetMatter = buffer.readFluidStack();
			FluidStack dryMatter = buffer.readFluidStack();
			IngredientWithSize input = IngredientWithSize.read(buffer);
			return new ComposterRecipe(recipeId, wetMatter, dryMatter, input, energy);
		}
		FluidTagInput water = FluidTagInput.read(buffer);
		FluidTagInput wetMatter = FluidTagInput.read(buffer);
		FluidTagInput dryMatter = FluidTagInput.read(buffer);
		IngredientWithSize output = IngredientWithSize.read(buffer);
		return new ComposterRecipe(recipeId, water, wetMatter, dryMatter, output, energy);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, ComposterRecipe recipe)
	{
		buffer.writeBoolean(recipe.fluidProduct);
		buffer.writeInt(recipe.energy);
		if (recipe.fluidProduct) {
			buffer.writeFluidStack(recipe.getFluidOutput()[0]);
			buffer.writeFluidStack(recipe.getFluidOutput()[1]);
			recipe.itemInput.write(buffer);
		}
		else {
			for (FluidTagInput input : recipe.getFluidInput())
				input.write(buffer);
			recipe.itemOutput.write(buffer);
		}
	}
}
