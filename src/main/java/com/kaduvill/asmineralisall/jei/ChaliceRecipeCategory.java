package com.kaduvill.asmineralisall.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public final class ChaliceRecipeCategory implements IRecipeCategory<ChaliceRecipeWrapper> {
    public static final String UID = "asmineralisall.chalice";
    private final ChaliceRecipeLayout layout;
    private final IDrawable background;
    private final IDrawable slot;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private IIngredientRenderer<FluidStack> fluidRenderer;
    private IIngredientRenderer<ItemStack> itemRenderer;

    ChaliceRecipeCategory(IGuiHelper helper, ChaliceRecipeLayout layout) {
        this.layout = layout;
        background = helper.createBlankDrawable(ChaliceRecipeLayout.WIDTH, layout.height);
        slot = helper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        arrowBackground = helper.createDrawable(texture, 24, 132, 24, 17);
        arrow = helper.createAnimatedDrawable(helper.createDrawable(texture, 82, 128, 24, 17),
                50, IDrawableAnimated.StartDirection.LEFT, false);
    }

    void setRenderers(IIngredientRenderer<FluidStack> fluids, IIngredientRenderer<ItemStack> items) {
        fluidRenderer = fluids;
        itemRenderer = items;
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("jei.asmineralisall.category.chalice"); }
    @Override public String getModName() { return I18n.format("jei.asmineralisall.mod_name"); }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        for (int i = 0; i < 2; i++) slot.draw(minecraft, ChaliceRecipeLayout.INPUT_X, layout.inputY(i));
        arrowBackground.draw(minecraft, ChaliceRecipeLayout.ARROW_X, layout.arrowY);
        arrow.draw(minecraft, ChaliceRecipeLayout.ARROW_X, layout.arrowY);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, ChaliceRecipeWrapper wrapper, IIngredients ingredients) {
        // Conditional output backgrounds belong to this wrapper, not the shared category instance.
        wrapper.bindLayout(layout.getItemStacks(), layout.getFluidStacks(), layout.getFocus(), slot, fluidRenderer, itemRenderer);
    }
}
