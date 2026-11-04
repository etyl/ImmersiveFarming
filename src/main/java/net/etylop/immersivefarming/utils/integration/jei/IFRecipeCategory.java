package net.etylop.immersivefarming.utils.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public abstract class IFRecipeCategory<T> implements IRecipeCategory<T>
{
    public final ResourceLocation uid;
    protected final IGuiHelper guiHelper;
    private final Class<? extends T> recipeClass;
    public MutableComponent title;
    private IDrawableStatic background;
    private IDrawable icon;
    private final RecipeType<T> recipeType;

    public IFRecipeCategory(RecipeType<T> recipeType, IGuiHelper guiHelper, String localKey)
    {
        this.recipeClass = recipeType.getRecipeClass();
        this.guiHelper = guiHelper;
        this.uid = recipeType.getUid();
        this.title = Component.translatable(localKey);
        this.recipeType = RecipeType.create(uid.getNamespace(), uid.getPath(), recipeClass);
    }

    @Override
    public IDrawable getBackground()
    {
        return this.background;
    }

    protected void setBackground(IDrawableStatic background)
    {
        this.background = background;
    }

    @Nullable
    @Override
    public IDrawable getIcon()
    {
        return this.icon;
    }

    protected void setIcon(ItemStack stack)
    {
        this.setIcon(this.guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, stack));
    }

    protected void setIcon(IDrawable icon)
    {
        this.icon = icon;
    }

    @Override
    public Component getTitle()
    {
        return this.title;
    }

    @Override
    public RecipeType<T> getRecipeType()
    {
        return recipeType;
    }

    public ResourceLocation getUid() {
        return uid;
    }

    public Class<? extends T> getRecipeClass() {
        return recipeClass;
    }
}