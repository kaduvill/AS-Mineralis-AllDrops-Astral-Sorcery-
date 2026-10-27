package com.kaduvill.asmineralisall.jei;

import com.kaduvill.asmineralisall.chalice.ChaliceRecipes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public final class ChaliceRecipeWrapper implements IRecipeWrapper {
    private final ChaliceRecipes.Recipe recipe;
    private final ChaliceRecipeLayout layout;
    private final int pageCount;
    private IGuiItemStackGroup itemStacks;
    private IDrawable slot;
    private List<IIngredientRenderer<ItemStack>> outputRenderers;
    private int page;

    ChaliceRecipeWrapper(ChaliceRecipes.Recipe recipe, ChaliceRecipeLayout layout) {
        if (recipe.outcomes.size() > layout.maxOutputs) throw new IllegalArgumentException("Chalice outputs exceed category bounds");
        this.recipe = recipe;
        this.layout = layout;
        this.pageCount = ChaliceRecipeLayout.pageCount(recipe.outcomes.size());
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.FLUID, Arrays.asList(recipe.getInput(0), recipe.getInput(1)));
        List<ItemStack> outputs = new ArrayList<>(recipe.outcomes.size());
        for (ChaliceRecipes.Outcome outcome : recipe.outcomes) outputs.add(outcome.getOutput());
        ingredients.setOutputs(VanillaTypes.ITEM, outputs);
    }

    void bindLayout(IGuiItemStackGroup items, IGuiFluidStackGroup fluids, IFocus<?> focus,
                    IDrawable slot, IIngredientRenderer<FluidStack> renderer, IIngredientRenderer<ItemStack> itemRenderer) {
        this.itemStacks = items;
        this.slot = slot;
        page = Math.min(page, pageCount - 1);
        if (focus != null && focus.getMode() == IFocus.Mode.OUTPUT && focus.getValue() instanceof ItemStack) {
            ItemStack searched = (ItemStack) focus.getValue();
            for (int i = 0; i < recipe.outcomes.size(); i++) {
                ItemStack output = recipe.outcomes.get(i).getOutput();
                if (ItemStack.areItemsEqual(output, searched) && ItemStack.areItemStackTagsEqual(output, searched)) {
                    page = i / ChaliceRecipeLayout.PAGE_SIZE;
                    break;
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            FluidStack input = recipe.getInput(i);
            fluids.init(i, true, new FluidRenderer(renderer, input), ChaliceRecipeLayout.INPUT_X + 1,
                    layout.inputY(i) + 1, 16, 16, 0, 0);
            fluids.set(i, input);
        }
        fluids.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (index < 0 || index > 1) return;
            int amount = recipe.getCommonAmount(index);
            tooltip.add(text(amount < 0 ? "amount_varies" : "required", amount));
        });
        items.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (!input && index >= 0 && index < recipe.outcomes.size()) addOutputTooltip(index, tooltip);
        });
        outputRenderers = new ArrayList<>(recipe.outcomes.size());
        for (int i = 0; i < recipe.outcomes.size(); i++) outputRenderers.add(new OutputRenderer(i, itemRenderer));
        // Hidden outputs retain separate real JEI slots for lookup and entire-filter handlers.
        for (int i = 0; i < recipe.outcomes.size(); i++) initOutput(i);
    }

    private void initOutput(int index) {
        int relative = index - page * ChaliceRecipeLayout.PAGE_SIZE;
        boolean visible = relative >= 0 && relative < ChaliceRecipeLayout.PAGE_SIZE;
        itemStacks.init(index, false, outputRenderers.get(index),
                visible ? layout.outputX(relative) : ChaliceRecipeLayout.HIDDEN,
                visible ? layout.outputY(recipe.outcomes.size(), relative) : ChaliceRecipeLayout.HIDDEN, 18, 18, 1, 1);
        itemStacks.set(index, recipe.outcomes.get(index).getOutput());
    }

    @Override
    public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        if (slot == null) return;
        for (int i = 0; i < ChaliceRecipeLayout.visibleCount(recipe.outcomes.size(), page); i++) {
            slot.draw(minecraft, layout.outputX(i), layout.outputY(recipe.outcomes.size(), i));
        }
        if (pageCount > 1) {
            FontRenderer font = minecraft.fontRenderer;
            String counter = I18n.format("jei.asmineralisall.page", page + 1, pageCount);
            font.drawString(counter, (width - font.getStringWidth(counter)) / 2, layout.footerY + 2, 0x606060);
            font.drawString("<", 3, layout.footerY + 2, page > 0 ? 0x404040 : 0xA0A0A0);
            font.drawString(">", width - 9, layout.footerY + 2,
                    page + 1 < pageCount ? 0x404040 : 0xA0A0A0);
        }
    }

    @Override
    public boolean handleClick(Minecraft minecraft, int x, int y, int button) {
        if (button != 0 || itemStacks == null || pageCount < 2
                || y < layout.footerY || y >= layout.height) return false;
        int next = page;
        if (x >= 0 && x < 18) next--;
        else if (x >= ChaliceRecipeLayout.WIDTH - 18 && x < ChaliceRecipeLayout.WIDTH) next++;
        else return false;
        if (next < 0 || next >= pageCount) return true;
        int old = page;
        page = next;
        positionPage(old);
        positionPage(page);
        return true;
    }

    private void positionPage(int target) {
        int first = target * ChaliceRecipeLayout.PAGE_SIZE;
        int end = first + ChaliceRecipeLayout.visibleCount(recipe.outcomes.size(), target);
        for (int i = first; i < end; i++) initOutput(i);
    }

    @Override
    public List<String> getTooltipStrings(int x, int y) {
        if (x < ChaliceRecipeLayout.ARROW_X || x >= ChaliceRecipeLayout.ARROW_X + 24
                || y < layout.arrowY || y >= layout.arrowY + 17) return Collections.emptyList();
        List<String> tooltip = new ArrayList<>();
        tooltip.add(text("one_outcome"));
        tooltip.add(text("range", recipe.range));
        return tooltip;
    }

    private void addOutputTooltip(int index, List<String> tooltip) {
        ChaliceRecipes.Outcome outcome = recipe.outcomes.get(index);
        tooltip.add(text("selection", percent(outcome.probability)));
        boolean multiple = outcome.branches.size() > 1;
        for (ChaliceRecipes.Branch branch : outcome.branches) {
            if (multiple) {
                tooltip.add("");
                tooltip.add(text("branch", percent(branch.probability)));
            }
            for (int i = 0; i < 2; i++) {
                FluidStack input = branch.reaction.getInput(i);
                double consumption = branch.reaction.getConsumption(i);
                tooltip.add(Double.isNaN(consumption)
                        ? text("fluid_unknown", input.getLocalizedName(), input.amount)
                        : text("fluid_cost", input.getLocalizedName(), input.amount, percent(consumption)));
            }
            if (branch.reaction.randomCrystal) tooltip.add(text("random_crystal"));
            if (branch.reaction.customAction) tooltip.add(text("custom_action"));
        }
    }

    static String text(String key, Object... values) { return I18n.format("jei.asmineralisall.chalice." + key, values); }
    static String percent(double value) {
        return Double.isNaN(value) ? text("unknown") : JeiFormats.percentage(value) + "%";
    }

    private final class OutputRenderer implements IIngredientRenderer<ItemStack> {
        private final int index;
        private final IIngredientRenderer<ItemStack> delegate;

        OutputRenderer(int index, IIngredientRenderer<ItemStack> delegate) {
            this.index = index;
            this.delegate = delegate;
        }

        @Override
        public void render(Minecraft minecraft, int x, int y, ItemStack ingredient) {
            // Offscreen ingredients remain available to helpers without issuing item draw calls.
            if (index / ChaliceRecipeLayout.PAGE_SIZE == page) delegate.render(minecraft, x, y, ingredient);
        }

        @Override
        public List<String> getTooltip(Minecraft minecraft, ItemStack ingredient, ITooltipFlag flag) {
            return delegate.getTooltip(minecraft, ingredient, flag);
        }

        @Override
        public FontRenderer getFontRenderer(Minecraft minecraft, ItemStack ingredient) {
            return delegate.getFontRenderer(minecraft, ingredient);
        }
    }

    private static final class FluidRenderer implements IIngredientRenderer<FluidStack> {
        private final IIngredientRenderer<FluidStack> delegate;
        private final FluidStack full;

        FluidRenderer(IIngredientRenderer<FluidStack> delegate, FluidStack input) {
            this.delegate = delegate;
            full = input.copy();
            full.amount = 1000;
        }

        @Override
        public void render(Minecraft minecraft, int x, int y, FluidStack ingredient) {
            delegate.render(minecraft, x, y, full);
        }

        @Override
        public List<String> getTooltip(Minecraft minecraft, FluidStack ingredient, ITooltipFlag flag) {
            // The renderer's full icon is cosmetic. Never label a variable input as "1,000 mB".
            return new ArrayList<>(Collections.singletonList(ingredient.getLocalizedName()));
        }
    }
}
