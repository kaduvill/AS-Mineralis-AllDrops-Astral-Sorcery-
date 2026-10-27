package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.ore.AstralOrePages;
import com.kaduvill.asjeialldrops.ore.OrePage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Collections;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@JEIPlugin
public final class OreTablesJeiPlugin implements IModPlugin {
    private final Map<OrePage.Kind, OrePage> pages = new EnumMap<>(OrePage.Kind.class);
    private final Map<OrePage.Kind, OreTableCategory> categories = new EnumMap<>(OrePage.Kind.class);
    private AstralOrePages preparedReader;

    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        pages.clear();
        categories.clear();
        preparedReader = null;
        try {
            preparedReader = new AstralOrePages();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Cannot read the installed AS ore tables", failure);
            return;
        }
        for (OrePage.Kind kind : OrePage.Kind.values()) {
            try {
                OrePage page;
                switch (kind) {
                    case ENRICHMENT: page = preparedReader.enrichment(); break;
                    case TRASH: page = preparedReader.trash(); break;
                    case SHRINE: page = preparedReader.shrine(); break;
                    default: throw new IllegalStateException("Unknown ore mechanic");
                }
                if (page == null || page.table.outputs.isEmpty()) continue;
                OreTableCategory category = new OreTableCategory(registry.getJeiHelpers().getGuiHelper(), page);
                pages.put(kind, page);
                categories.put(kind, category);
                registry.addRecipeCategories(category);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("AS JEI page disabled: " + kind.key, failure);
            }
        }
    }

    @Override public void register(IModRegistry registry) {
        try {
            IIngredientRenderer<ItemStack> renderer = registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.ITEM);
            Item journal = Item.getByNameOrId("astralsorcery:itemjournal");
            for (Map.Entry<OrePage.Kind, OrePage> entry : pages.entrySet()) {
                try {
                    OrePage page = entry.getValue();
                    if (page.kind == OrePage.Kind.TRASH && preparedReader != null) {
                        page = preparedReader.resolveTrashInputs(page, registry.getIngredientRegistry().getAllIngredients(VanillaTypes.ITEM));
                        if (page == null) continue;
                        entry.setValue(page);
                    }
                    if (page.kind == OrePage.Kind.TRASH && !page.hasInputs()) continue;
                    categories.get(page.kind).setRenderer(renderer);
                    registry.addRecipes(Collections.singletonList(new OreTableRecipe(page)), page.kind.uid());
                    if (journal != null) registry.addRecipeCatalyst(new ItemStack(journal), page.kind.uid());
                } catch (RuntimeException | LinkageError failure) {
                    ASJeiAllDrops.LOGGER.warn("Cannot register AS JEI page: " + entry.getKey().key, failure);
                }
            }
        } finally {
            // Reflection adapters and native drop predicates are needed only while preparing JEI.
            preparedReader = null;
        }
    }
}
