package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import com.kaduvill.asjeialldrops.chalice.AstralChaliceReader;
import com.kaduvill.asjeialldrops.chalice.ChaliceRecipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@JEIPlugin
public final class ChaliceJeiPlugin implements IModPlugin {
    private ChaliceRecipeCategory category;
    private List<ChaliceRecipeWrapper> recipes = Collections.emptyList();

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        category = null;
        recipes = Collections.emptyList();
        if (!ASJeiAllDropsConfig.containmentChalice) return;
        List<ChaliceRecipes.Recipe> pools = readRecipes();
        int maxOutputs = 0;
        for (ChaliceRecipes.Recipe pool : pools) maxOutputs = Math.max(maxOutputs, pool.outcomes.size());
        ChaliceRecipeLayout layout = new ChaliceRecipeLayout(maxOutputs);
        List<ChaliceRecipeWrapper> wrappers = new ArrayList<>(pools.size());
        for (ChaliceRecipes.Recipe pool : pools) wrappers.add(new ChaliceRecipeWrapper(pool, layout));
        recipes = Collections.unmodifiableList(wrappers);
        category = new ChaliceRecipeCategory(registry.getJeiHelpers().getGuiHelper(), layout);
        registry.addRecipeCategories(category);
    }

    @Override
    public void register(IModRegistry registry) {
        if (category == null) return;
        category.setRenderers(registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.FLUID),
                registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.ITEM));
        registry.addRecipes(recipes, ChaliceRecipeCategory.UID);
        Item chalice = Item.getByNameOrId("astralsorcery:blockchalice");
        if (chalice != null) registry.addRecipeCatalyst(new ItemStack(chalice), ChaliceRecipeCategory.UID);
    }

    private static List<ChaliceRecipes.Recipe> readRecipes() {
        try {
            // Rebuild dimensions and wrappers from one fresh snapshot during category registration.
            return new AstralChaliceReader().read().recipes();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Chalice JEI pages disabled: cannot read the installed Astral Sorcery reactions", failure);
            return Collections.emptyList();
        }
    }
}
