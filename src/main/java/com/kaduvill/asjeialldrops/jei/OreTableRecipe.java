package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ore.OrePage;
import com.kaduvill.asjeialldrops.ore.OreTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IDrawable;
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

final class OreTableRecipe implements IRecipeWrapper {
    private final OrePage data;
    private IGuiItemStackGroup items;
    private OreTableLayout layout;
    private IDrawable slot;
    private List<IIngredientRenderer<ItemStack>> renderers;
    private int page;

    OreTableRecipe(OrePage data) { this.data = data; }

    @Override public void getIngredients(IIngredients ingredients) {
        List<ItemStack> inputs = data.getInputs();
        ingredients.setInputLists(VanillaTypes.ITEM, inputs.isEmpty() ? Collections.emptyList() : Collections.singletonList(inputs));
        List<ItemStack> outputs = new ArrayList<>(data.table.outputs.size());
        for (OreTable.Output output : data.table.outputs) outputs.add(output.getStack());
        ingredients.setOutputs(VanillaTypes.ITEM, outputs);
    }

    void bind(IGuiItemStackGroup items, IFocus<?> focus, OreTableLayout layout, IDrawable slot,
            IIngredientRenderer<ItemStack> renderer) {
        this.items = items;
        this.layout = layout;
        this.slot = slot;
        if (focus != null && focus.getMode() == IFocus.Mode.OUTPUT && focus.getValue() instanceof ItemStack) {
            ItemStack searched = (ItemStack) focus.getValue();
            for (int i = 0; i < data.table.outputs.size(); i++) {
                ItemStack output = data.table.outputs.get(i).getStack();
                if (ItemStack.areItemsEqual(searched, output) && ItemStack.areItemStackTagsEqual(searched, output)) {
                    page = i / OreTableLayout.PAGE_SIZE;
                    break;
                }
            }
        }
        page = Math.max(0, Math.min(page, layout.pages - 1));
        List<ItemStack> inputs = data.getInputs();
        if (!inputs.isEmpty()) {
            items.init(0, true, OreTableLayout.INPUT_X, layout.inputY);
            items.set(0, inputs);
        }
        items.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (input) {
                if (data.kind == OrePage.Kind.TRASH) tooltip.add(text("harvest_drop"));
            } else if (index > 0 && index <= data.table.outputs.size()) {
                tooltip.add(I18n.format("jei.asjeialldrops.chance",
                        JeiFormats.percentage(data.table.outputs.get(index - 1).probability)));
            }
        });
        renderers = new ArrayList<>(data.table.outputs.size());
        for (int i = 0; i < data.table.outputs.size(); i++) renderers.add(new OutputRenderer(i, renderer));
        for (int i = 0; i < data.table.outputs.size(); i++) initOutput(i);
    }

    private void initOutput(int index) {
        int relative = index - page * OreTableLayout.PAGE_SIZE;
        boolean visible = relative >= 0 && relative < OreTableLayout.PAGE_SIZE;
        items.init(index + 1, false, renderers.get(index), visible ? layout.outputX(relative) : OreTableLayout.HIDDEN,
                visible ? layout.outputY(relative) : OreTableLayout.HIDDEN, 18, 18, 1, 1);
        items.set(index + 1, data.table.outputs.get(index).getStack());
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        if (slot == null) return;
        int visible = OreTableLayout.visible(data.table.outputs.size(), page);
        for (int i = 0; i < visible; i++) slot.draw(minecraft, layout.outputX(i), layout.outputY(i));
        if (layout.pages > 1) {
            FontRenderer font = minecraft.fontRenderer;
            String counter = I18n.format("jei.asjeialldrops.page", page + 1, layout.pages);
            font.drawString(counter, (width - font.getStringWidth(counter)) / 2, layout.footerY + 2, 0x606060);
            font.drawString("<", 3, layout.footerY + 2, page > 0 ? 0x404040 : 0xA0A0A0);
            font.drawString(">", width - 9, layout.footerY + 2, page + 1 < layout.pages ? 0x404040 : 0xA0A0A0);
        }
    }

    @Override public boolean handleClick(Minecraft minecraft, int x, int y, int button) {
        if (button != 0 || items == null || layout.pages < 2 || y < layout.footerY || y >= layout.height) return false;
        int next = page;
        if (x >= 0 && x < 18) next--;
        else if (x >= OreTableLayout.WIDTH - 18 && x < OreTableLayout.WIDTH) next++;
        else return false;
        if (next < 0 || next >= layout.pages) return true;
        int old = page;
        page = next;
        positionPage(old);
        positionPage(page);
        return true;
    }
    private void positionPage(int target) {
        int first = target * OreTableLayout.PAGE_SIZE;
        int end = first + OreTableLayout.visible(data.table.outputs.size(), target);
        for (int i = first; i < end; i++) initOutput(i);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (layout == null) return Collections.emptyList();
        if (data.kind == OrePage.Kind.SHRINE && x >= 0 && x < 38 && y >= layout.inputY && y < layout.inputY + 18) {
            return Collections.singletonList(text("shrine_generator"));
        }
        if (x < OreTableLayout.ARROW_X || x >= OreTableLayout.ARROW_X + 24
                || y < layout.arrowY || y >= layout.arrowY + 17) return Collections.emptyList();
        List<String> tooltip = new ArrayList<>();
        switch (data.kind) {
            case ENRICHMENT:
                tooltip.add(text("base_attempt", data.baseInterval));
                tooltip.add(text("base_range", data.baseRange));
                tooltip.add(text("perk_modifiers"));
                break;
            case TRASH:
                tooltip.add(text("base_replacement", JeiFormats.percentage(data.baseReplacement)));
                tooltip.add(text("per_stack"));
                tooltip.add(text("perk_modifiers"));
                break;
            case SHRINE:
                tooltip.add(text("regenerations", data.regenerations));
                tooltip.add(text("real_player"));
                break;
        }
        if (data.table.noResult >= .0005) tooltip.add(text("no_result", JeiFormats.percentage(data.table.noResult)));
        return tooltip;
    }
    private static String text(String key, Object... values) { return I18n.format("jei.asjeialldrops.ore." + key, values); }

    private final class OutputRenderer implements IIngredientRenderer<ItemStack> {
        private final int index;
        private final IIngredientRenderer<ItemStack> delegate;
        OutputRenderer(int index, IIngredientRenderer<ItemStack> delegate) { this.index = index; this.delegate = delegate; }
        @Override public void render(Minecraft minecraft, int x, int y, ItemStack ingredient) {
            if (index / OreTableLayout.PAGE_SIZE == page) delegate.render(minecraft, x, y, ingredient);
        }
        @Override public List<String> getTooltip(Minecraft minecraft, ItemStack ingredient, ITooltipFlag flag) {
            return delegate.getTooltip(minecraft, ingredient, flag);
        }
        @Override public FontRenderer getFontRenderer(Minecraft minecraft, ItemStack ingredient) {
            return delegate.getFontRenderer(minecraft, ingredient);
        }
    }
}
