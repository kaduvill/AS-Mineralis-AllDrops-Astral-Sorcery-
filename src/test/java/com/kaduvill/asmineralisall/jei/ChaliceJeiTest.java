package com.kaduvill.asmineralisall.jei;

import com.kaduvill.asmineralisall.chalice.ChaliceRecipes;
import hellfirepvp.astralsorcery.common.base.LiquidInteraction;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChaliceJeiTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Test
    public void indexesAllConcreteOutputsAndPagesWithoutEmptySlots() {
        ChaliceRecipeWrapper wrapper = wrapper(19);
        ChaliceRecipeLayout layout = new ChaliceRecipeLayout(19);
        List<List<ItemStack>> exports = new ArrayList<>();
        IIngredients ingredients = proxy(IIngredients.class, (method, args) -> {
            if (method.equals("setOutputs")) exports.add((List<ItemStack>) args[1]);
            return null;
        });
        wrapper.getIngredients(ingredients);
        assertEquals(19, exports.get(0).size());
        exports.get(0).get(0).setCount(50);
        wrapper.getIngredients(ingredients);
        assertEquals(1, exports.get(1).get(0).getCount());
        Map<Integer, int[]> positions = new HashMap<>();
        Map<Integer, ItemStack> stacks = new HashMap<>();
        Set<Integer> fluids = new LinkedHashSet<>();
        bind(wrapper, positions, stacks, fluids, null);
        assertEquals(19, stacks.size());
        assertEquals(16, visible(positions));
        assertEquals(new LinkedHashSet<>(Arrays.asList(0, 1)), fluids);
        assertTrue(wrapper.handleClick(null, 161, layout.footerY, 0));
        assertEquals(3, visible(positions));
        assertEquals(19, stacks.size());
        assertEquals(ChaliceRecipeLayout.HIDDEN, positions.get(0)[0]);
        assertEquals(ChaliceRecipeLayout.OUTPUT_X, positions.get(16)[0]);
        assertFalse(wrapper.handleClick(null, 161, layout.footerY, 1));
        assertTrue(wrapper.handleClick(null, 1, layout.footerY, 0));
        assertEquals(16, visible(positions));
    }

    @Test
    public void focusedLaterOutputOpensItsPageAndSparseGridHasNoPhantomSlots() {
        for (int count : new int[]{1, 2, 3, 5, 16}) {
            Map<Integer, int[]> positions = new HashMap<>();
            bind(wrapper(count), positions, new HashMap<>(), new LinkedHashSet<>(), null);
            assertEquals(count, positions.size());
            assertEquals(count, visible(positions));
        }
        IFocus<ItemStack> focus = new IFocus<ItemStack>() {
            @Override public ItemStack getValue() { return new ItemStack(Items.DIAMOND, 1, 18); }
            @Override public Mode getMode() { return Mode.OUTPUT; }
        };
        Map<Integer, int[]> positions = new HashMap<>();
        bind(wrapper(19), positions, new HashMap<>(), new LinkedHashSet<>(), focus);
        assertEquals(3, visible(positions));
        assertEquals(ChaliceRecipeLayout.HIDDEN, positions.get(0)[0]);
        assertTrue(positions.get(18)[0] >= 0);
    }

    @Test
    public void onlyVisibleOutputsIssueItemRenderingCalls() {
        ChaliceRecipeWrapper wrapper = wrapper(200);
        ChaliceRecipeLayout layout = new ChaliceRecipeLayout(200);
        Map<Integer, IIngredientRenderer<ItemStack>> renderers = new HashMap<>();
        int[] draws = {0};
        IIngredientRenderer<ItemStack> delegate = new IIngredientRenderer<ItemStack>() {
            @Override public void render(Minecraft minecraft, int x, int y, ItemStack ingredient) { draws[0]++; }
        };
        IGuiItemStackGroup items = proxy(IGuiItemStackGroup.class, (method, args) -> {
            if (method.equals("init")) renderers.put((Integer) args[0], (IIngredientRenderer<ItemStack>) args[2]);
            return null;
        });
        IGuiFluidStackGroup fluids = proxy(IGuiFluidStackGroup.class, (method, args) -> null);
        wrapper.bindLayout(items, fluids, null, null, null, delegate);
        assertEquals(200, renderers.size());
        for (IIngredientRenderer<ItemStack> renderer : renderers.values()) renderer.render(null, 0, 0, ItemStack.EMPTY);
        assertEquals(16, draws[0]);
        for (int i = 0; i < 12; i++) wrapper.handleClick(null, 161, layout.footerY, 0);
        draws[0] = 0;
        for (IIngredientRenderer<ItemStack> renderer : renderers.values()) renderer.render(null, 0, 0, ItemStack.EMPTY);
        assertEquals(8, draws[0]);
    }

    @Test
    public void registrationRebuildsFromCurrentRegistryWithoutRetainingOldRecipes() throws Exception {
        Field field = LiquidInteraction.class.getDeclaredField("registeredInteractions");
        field.setAccessible(true);
        Object previous = field.get(null);
        List<LiquidInteraction> scripted = LiquidInteraction.mtInteractions;
        try {
            field.set(null, new ArrayList<LiquidInteraction>());
            LiquidInteraction.mtInteractions = new ArrayList<>();
            add(Items.DIAMOND);
            ChaliceJeiPlugin plugin = new ChaliceJeiPlugin();
            List<ChaliceRecipeWrapper> initial = registerRecipes(plugin);
            assertEquals(1, initial.size());
            assertSame(Items.DIAMOND, outputs(initial.get(0)).get(0).getItem());

            // A same-size script replacement must be visible on the next JEI rebuild.
            field.set(null, new ArrayList<LiquidInteraction>());
            add(Items.EMERALD);
            assertSame(Items.DIAMOND, outputs(initial.get(0)).get(0).getItem());
            List<ChaliceRecipeWrapper> replaced = registerRecipes(plugin);
            assertEquals(1, replaced.size());
            assertSame(Items.EMERALD, outputs(replaced.get(0)).get(0).getItem());

            field.set(null, new ArrayList<LiquidInteraction>());
            assertTrue(registerRecipes(plugin).isEmpty());

            // A restored pool gets a fresh wrapper, with no cache carried across JEI builds.
            add(Items.DIAMOND);
            List<ChaliceRecipeWrapper> restored = registerRecipes(plugin);
            assertEquals(1, restored.size());
            assertNotSame(initial.get(0), restored.get(0));
            assertSame(Items.DIAMOND, outputs(restored.get(0)).get(0).getItem());

            // Malformed data disables this registration instead of publishing a partial/stale view.
            ((List<Object>) field.get(null)).add(null);
            assertTrue(registerRecipes(plugin).isEmpty());
            field.set(null, new ArrayList<LiquidInteraction>());
            add(Items.APPLE);
            List<ChaliceRecipeWrapper> recovered = registerRecipes(plugin);
            assertEquals(1, recovered.size());
            assertSame(Items.APPLE, outputs(recovered.get(0)).get(0).getItem());
        } finally {
            field.set(null, previous);
            LiquidInteraction.mtInteractions = scripted;
        }
    }

    @Test
    public void rebuildsGrowAndShrinkCategoryWithoutClippingOrPrematurePaging() throws Exception {
        Field locale = net.minecraft.client.resources.I18n.class.getDeclaredField("i18nLocale");
        locale.setAccessible(true);
        Object previousLocale = locale.get(null);
        Field field = LiquidInteraction.class.getDeclaredField("registeredInteractions");
        field.setAccessible(true);
        Object previous = field.get(null);
        List<LiquidInteraction> scripted = LiquidInteraction.mtInteractions;
        List<LiquidInteraction> ordinary = new ArrayList<>();
        try {
            // Minecraft normally installs this before opening JEI; this test has no game client.
            locale.set(null, new net.minecraft.client.resources.Locale());
            field.set(null, ordinary);
            LiquidInteraction.mtInteractions = new ArrayList<>();
            ChaliceJeiPlugin plugin = new ChaliceJeiPlugin();
            int[] counts =  {0, 1, 4, 5, 8, 9, 12, 13, 16, 17, 33, 16, 9, 5, 1, 0};
            int[] heights = {44,44,44,44,44,58,58, 76, 76, 89, 89, 76,58,44,44,44};
            for (int step = 0; step < counts.length; step++) {
                int count = counts[step];
                ordinary.clear();
                addOutputs(count);
                if (count > 0) {
                    // Many alternative costs for one output must not enlarge the category.
                    for (int amount = 1; amount <= 20; amount++) {
                        new LiquidInteraction(1, new FluidStack(FluidRegistry.LAVA, amount),
                                new FluidStack(FluidRegistry.WATER, amount),
                                LiquidInteraction.createItemDropAction(1, 1, new ItemStack(Items.APPLE)));
                    }
                }
                Registration registration = register(plugin, () -> {});
                int height = registration.category.getBackground().getHeight();
                assertEquals("Height for " + count + " outputs", heights[step], height);
                assertEquals(count == 0 ? 0 : 2, registration.recipes.size());
                int bodyBottom = height - (count > 16 ? 13 : 0);
                for (ChaliceRecipeWrapper wrapper : registration.recipes) {
                    Map<Integer, int[]> positions = new HashMap<>();
                    Map<Integer, int[]> fluidPositions = new HashMap<>();
                    IGuiItemStackGroup items = proxy(IGuiItemStackGroup.class, (method, args) -> {
                        if (method.equals("init")) positions.put((Integer) args[0], new int[]{(Integer) args[3], (Integer) args[4]});
                        return null;
                    });
                    IGuiFluidStackGroup fluids = proxy(IGuiFluidStackGroup.class, (method, args) -> {
                        if (method.equals("init")) fluidPositions.put((Integer) args[0], new int[]{(Integer) args[3], (Integer) args[4]});
                        return null;
                    });
                    registration.category.setRecipe(proxy(IRecipeLayout.class, (method, args) -> {
                        if (method.equals("getItemStacks")) return items;
                        if (method.equals("getFluidStacks")) return fluids;
                        return null;
                    }), wrapper, null);
                    int outputs = outputs(wrapper).size();
                    assertEquals(outputs, positions.size());
                    assertEquals(Math.min(16, outputs), visible(positions));
                    Set<Integer> rows = new LinkedHashSet<>();
                    for (int[] position : positions.values()) {
                        if (position[0] < 0) continue;
                        assertRectangleInside(position, 18, bodyBottom);
                        rows.add(position[1]);
                    }
                    assertEquals(Math.min(4, (outputs + 3) / 4), rows.size());
                    assertEquals(2, fluidPositions.size());
                    for (int[] position : fluidPositions.values()) assertRectangleInside(position, 16, bodyBottom);
                    assertEquals(2, wrapper.getTooltipStrings(47, (bodyBottom - 17) / 2).size());
                    assertFalse(wrapper.handleClick(null, 161, bodyBottom - 1, 0));
                    assertFalse(wrapper.handleClick(null, 161, height, 0));
                    if (outputs <= 16) {
                        assertFalse(wrapper.handleClick(null, 161, bodyBottom, 0));
                    } else {
                        int pages = (outputs + 15) / 16;
                        for (int page = 1; page < pages; page++) {
                            assertTrue(wrapper.handleClick(null, 161, bodyBottom, 0));
                            assertEquals(Math.min(16, outputs - page * 16), visible(positions));
                            for (int[] position : positions.values()) {
                                if (position[0] >= 0) assertRectangleInside(position, 18, bodyBottom);
                            }
                        }
                    }
                    // Setting different recipes or changing output pages never resizes shared bounds.
                    assertEquals(height, registration.category.getBackground().getHeight());
                }
            }
        } finally {
            locale.set(null, previousLocale);
            field.set(null, previous);
            LiquidInteraction.mtInteractions = scripted;
        }
    }

    @Test
    public void dimensionsAndRegisteredRecipesUseTheSameSnapshot() throws Exception {
        Field field = LiquidInteraction.class.getDeclaredField("registeredInteractions");
        field.setAccessible(true);
        Object previous = field.get(null);
        List<LiquidInteraction> scripted = LiquidInteraction.mtInteractions;
        List<LiquidInteraction> ordinary = new ArrayList<>();
        try {
            field.set(null, ordinary);
            LiquidInteraction.mtInteractions = new ArrayList<>();
            addOutputs(17);
            ChaliceJeiPlugin plugin = new ChaliceJeiPlugin();
            Registration first = register(plugin, () -> {
                ordinary.clear();
                add(Items.APPLE);
            });
            assertEquals(89, first.category.getBackground().getHeight());
            assertEquals(17, outputs(first.recipes.get(0)).size());
            Registration rebuilt = register(plugin, () -> {});
            assertEquals(44, rebuilt.category.getBackground().getHeight());
            assertEquals(1, outputs(rebuilt.recipes.get(0)).size());
            assertSame(Items.APPLE, outputs(rebuilt.recipes.get(0)).get(0).getItem());
        } finally {
            field.set(null, previous);
            LiquidInteraction.mtInteractions = scripted;
        }
    }

    @Test
    public void localizedTooltipsHaveValidFormatStrings() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(
                "/assets/asmineralisall/lang/en_us.lang"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("jei.asmineralisall.chalice.")) {
                    String.format(java.util.Locale.ROOT, line.substring(line.indexOf('=') + 1), "x", "x", "x", "x");
                }
            }
        }
    }

    private static void add(net.minecraft.item.Item item) {
        new LiquidInteraction(1, new FluidStack(FluidRegistry.WATER, 10), new FluidStack(FluidRegistry.LAVA, 10),
                LiquidInteraction.createItemDropAction(1, 1, new ItemStack(item)));
    }

    private static void addOutputs(int count) {
        for (int i = 0; i < count; i++) {
            new LiquidInteraction(1, new FluidStack(FluidRegistry.WATER, 10), new FluidStack(FluidRegistry.LAVA, 10),
                    LiquidInteraction.createItemDropAction(1, 1, new ItemStack(Items.DIAMOND, 1, i)));
        }
    }

    private static void assertRectangleInside(int[] position, int size, int height) {
        assertTrue(position[0] >= 0);
        assertTrue(position[1] >= 0);
        assertTrue(position[0] + size <= 162);
        assertTrue(position[1] + size <= height);
    }

    private static List<ChaliceRecipeWrapper> registerRecipes(ChaliceJeiPlugin plugin) {
        return register(plugin, () -> {}).recipes;
    }

    private static Registration register(ChaliceJeiPlugin plugin, Runnable afterCategories) {
        Registration result = new Registration();
        IGuiHelper gui = proxy(IGuiHelper.class, (method, args) -> {
            if (!method.equals("createBlankDrawable")) return null;
            int width = (Integer) args[0];
            int height = (Integer) args[1];
            return proxy(IDrawableStatic.class, (drawMethod, drawArgs) -> {
                if (drawMethod.equals("getWidth")) return width;
                if (drawMethod.equals("getHeight")) return height;
                return null;
            });
        });
        IJeiHelpers helpers = proxy(IJeiHelpers.class, (method, args) -> method.equals("getGuiHelper") ? gui : null);
        plugin.registerCategories(proxy(IRecipeCategoryRegistration.class, (method, args) -> {
            if (method.equals("getJeiHelpers")) return helpers;
            if (method.equals("addRecipeCategories")) result.category = (ChaliceRecipeCategory) ((IRecipeCategory<?>[]) args[0])[0];
            return null;
        }));
        afterCategories.run();
        IIngredientRegistry ingredients = proxy(IIngredientRegistry.class, (method, args) -> null);
        plugin.register(proxy(IModRegistry.class, (method, args) -> {
            if (method.equals("getIngredientRegistry")) return ingredients;
            if (method.equals("addRecipes")) {
                assertEquals(ChaliceRecipeCategory.UID, args[1]);
                result.recipes.addAll((List<ChaliceRecipeWrapper>) args[0]);
            }
            return null;
        }));
        return result;
    }

    private static final class Registration {
        ChaliceRecipeCategory category;
        final List<ChaliceRecipeWrapper> recipes = new ArrayList<>();
    }

    private static List<ItemStack> outputs(ChaliceRecipeWrapper wrapper) {
        List<ItemStack> outputs = new ArrayList<>();
        wrapper.getIngredients(proxy(IIngredients.class, (method, args) -> {
            if (method.equals("setOutputs")) outputs.addAll((List<ItemStack>) args[1]);
            return null;
        }));
        return outputs;
    }

    private static ChaliceRecipeWrapper wrapper(int count) {
        List<ChaliceRecipes.Reaction> reactions = new ArrayList<>();
        for (int i = 0; i < count; i++) reactions.add(new ChaliceRecipes.Reaction(
                new FluidStack(FluidRegistry.WATER, 10), new FluidStack(FluidRegistry.LAVA, 10),
                new ItemStack(Items.DIAMOND, 1, i), 1, 1, 1, false, false));
        return new ChaliceRecipeWrapper(new ChaliceRecipes.Snapshot(reactions, 16).recipes().get(0),
                new ChaliceRecipeLayout(count));
    }

    private static void bind(ChaliceRecipeWrapper wrapper, Map<Integer, int[]> positions,
                             Map<Integer, ItemStack> stacks, Set<Integer> fluids, IFocus<?> focus) {
        IGuiItemStackGroup items = proxy(IGuiItemStackGroup.class, (method, args) -> {
            if (method.equals("init")) positions.put((Integer) args[0], new int[]{(Integer) args[3], (Integer) args[4]});
            if (method.equals("set") && args[1] instanceof ItemStack) stacks.put((Integer) args[0], (ItemStack) args[1]);
            return null;
        });
        IGuiFluidStackGroup fluidGroup = proxy(IGuiFluidStackGroup.class, (method, args) -> {
            if (method.equals("set") && args[1] instanceof FluidStack) fluids.add((Integer) args[0]);
            return null;
        });
        wrapper.bindLayout(items, fluidGroup, focus, null, null, null);
    }

    private static int visible(Map<Integer, int[]> positions) {
        int count = 0;
        for (int[] position : positions.values()) if (position[0] >= 0) count++;
        return count;
    }

    private interface Call { Object invoke(String method, Object[] arguments); }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> call.invoke(method.getName(), args)));
    }
}
