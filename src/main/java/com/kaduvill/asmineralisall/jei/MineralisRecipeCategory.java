package com.kaduvill.asmineralisall.jei;

import com.kaduvill.asmineralisall.mineralis.MineralisDrop;
import java.util.List;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public final class MineralisRecipeCategory implements IRecipeCategory<MineralisRecipeWrapper> {
    public static final String UID = "asmineralisall.mineralis";
    private final IGuiHelper guiHelper;
    private final IDrawable slot;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private IDrawable background;
    private MineralisRecipeLayout layout;

    public MineralisRecipeCategory(IGuiHelper guiHelper, int inputCount, int maxDrops) {
        this.guiHelper = guiHelper;
        this.slot = guiHelper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        this.arrowBackground = guiHelper.createDrawable(texture, 24, 132, 24, 17);
        IDrawableStatic arrowFill = guiHelper.createDrawable(texture, 82, 128, 24, 17);
        this.arrow = guiHelper.createAnimatedDrawable(arrowFill, 200, IDrawableAnimated.StartDirection.LEFT, false);
        updateLayout(inputCount, maxDrops);
    }

    void updateLayout(int inputCount, int maxDrops) {
        if (layout != null && layout.inputCount == inputCount && layout.dropCount == maxDrops) return;
        layout = new MineralisRecipeLayout(inputCount, maxDrops);
        background = guiHelper.createBlankDrawable(MineralisRecipeLayout.WIDTH, layout.height);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("jei.asmineralisall.category.mineralis");
    }

    @Override
    public String getModName() {
        return I18n.format("jei.asmineralisall.mod_name");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        for (int index = 0; index < layout.inputCount; index++)
            slot.draw(minecraft, layout.inputX(index), layout.inputY(index));
        for (int index = 0; index < layout.pageSize; index++)
            slot.draw(minecraft, layout.outputX(index), layout.outputY(index));
        if (layout.inputCount > 0) drawArrow(minecraft);
    }

    private void drawArrow(Minecraft minecraft) {
        int arrowWidth = arrowBackground.getHeight();
        int arrowHeight = arrowBackground.getWidth();
        float x = (MineralisRecipeLayout.WIDTH + arrowWidth) / 2.0F;
        float y = layout.outputY - (MineralisRecipeLayout.INPUT_OUTPUT_GAP + arrowHeight) / 2.0F;
        GlStateManager.pushMatrix();
        try {
            // Rotate the background and fill together, so the animation runs downward.
            GlStateManager.translate(x, y, 0);
            GlStateManager.rotate(90, 0, 0, 1);
            arrowBackground.draw(minecraft, 0, 0);
            arrow.draw(minecraft, 0, 0);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, MineralisRecipeWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        updateLayout(recipeWrapper.getInputs().size(), recipeWrapper.getDrops().size());
        recipeWrapper.bindLayout(itemStacks, layout);
        List<MineralisDrop> drops = recipeWrapper.getDrops();
        int outputOffset = recipeWrapper.getInputs().size();
        itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            int dropIndex = slotIndex - outputOffset;
            if (input || dropIndex < 0 || dropIndex >= drops.size()) return;
            double probability = drops.get(dropIndex).getProbability();
            tooltip.add(I18n.format("jei.asmineralisall.chance", formatPercentage(probability)));
        });
    }

    static String formatPercentage(double probability) {
        return JeiFormats.percentage(probability);
    }
}
