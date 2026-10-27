package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ore.OrePage;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

final class OreTableCategory implements IRecipeCategory<OreTableRecipe> {
    private final OrePage.Kind kind;
    private final OreTableLayout layout;
    private final IDrawable background, slot;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private IIngredientRenderer<ItemStack> renderer;

    OreTableCategory(IGuiHelper helper, OrePage page) {
        kind = page.kind;
        layout = new OreTableLayout(page.table.outputs.size(),
                kind == OrePage.Kind.ENRICHMENT || kind == OrePage.Kind.TRASH);
        background = helper.createBlankDrawable(OreTableLayout.WIDTH, layout.height);
        slot = helper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        arrowBackground = helper.createDrawable(texture, 24, 132, 24, 17);
        arrow = helper.createAnimatedDrawable(helper.createDrawable(texture, 82, 128, 24, 17),
                50, IDrawableAnimated.StartDirection.LEFT, false);
    }
    void setRenderer(IIngredientRenderer<ItemStack> renderer) { this.renderer = renderer; }
    @Override public String getUid() { return kind.uid(); }
    @Override public String getTitle() { return I18n.format("jei.asjeialldrops.category." + kind.key); }
    @Override public String getModName() { return I18n.format("jei.asjeialldrops.mod_name"); }
    @Override public IDrawable getBackground() { return background; }

    @Override public void drawExtras(Minecraft minecraft) {
        if (kind == OrePage.Kind.SHRINE) {
            drawLabel(minecraft, "shrine_source", 19, layout.inputY + 5);
        } else slot.draw(minecraft, OreTableLayout.INPUT_X, layout.inputY);
        arrowBackground.draw(minecraft, OreTableLayout.ARROW_X, layout.arrowY);
        arrow.draw(minecraft, OreTableLayout.ARROW_X, layout.arrowY);
        if (layout.perkY >= 0) drawLabel(minecraft, "perk", OreTableLayout.ARROW_X + 12, layout.perkY);
    }

    private static void drawLabel(Minecraft minecraft, String key, int centerX, int y) {
        FontRenderer font = minecraft.fontRenderer;
        String text = I18n.format("jei.asjeialldrops.ore." + key);
        try {
            font.drawString(text, centerX - font.getStringWidth(text) / 2, y, 0x606060);
        } finally {
            // FontRenderer leaves its text color active; subsequent JEI textures need white.
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }
    @Override public void setRecipe(IRecipeLayout recipeLayout, OreTableRecipe recipe, IIngredients ingredients) {
        recipe.bind(recipeLayout.getItemStacks(), recipeLayout.getFocus(), layout, slot, renderer);
    }
}
