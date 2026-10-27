package com.kaduvill.asmineralisall.chalice;

import hellfirepvp.astralsorcery.common.base.LiquidInteraction;
import hellfirepvp.astralsorcery.common.base.LiquidInteraction.FluidInteractionAction;
import hellfirepvp.astralsorcery.common.tile.TileChalice.ConfigEntryChalice;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class AstralChaliceReaderTest {
    private Object previousOrdinary;
    private List<LiquidInteraction> previousScripted;
    private float previousRange;
    private final AstralChaliceReader reader = new AstralChaliceReader();

    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Before
    public void isolateRegistry() throws Exception {
        previousOrdinary = registryField().get(null);
        previousScripted = LiquidInteraction.mtInteractions;
        previousRange = ConfigEntryChalice.chaliceRange;
        registryField().set(null, new ArrayList<LiquidInteraction>());
        LiquidInteraction.mtInteractions = new ArrayList<>();
        ConfigEntryChalice.chaliceRange = 16;
    }

    @After
    public void restoreRegistry() throws Exception {
        registryField().set(null, previousOrdinary);
        LiquidInteraction.mtInteractions = previousScripted;
        ConfigEntryChalice.chaliceRange = previousRange;
    }

    @Test
    public void matchesActualSelectorMultiplicityWithoutMutatingStacksOrRng() throws Exception {
        add(3, 10, 10, 0, 1, new ItemStack(Items.DIAMOND));
        LiquidInteraction scripted = add(1, 70, 80, .8F, .4F, new ItemStack(Items.EMERALD));
        LiquidInteraction.mtInteractions.add(scripted);
        // AS deliberately sees the scripted object in both lists. Its effective weight is two.
        assertEquals(3, LiquidInteraction.getPossibleInteractions(new FluidStack(FluidRegistry.WATER, 1)).size());
        Field randomField = LiquidInteraction.class.getDeclaredField("rand");
        randomField.setAccessible(true);
        Random random = (Random) randomField.get(null);
        random.setSeed(123456);
        Random expected = new Random(123456);
        ChaliceRecipes.Recipe recipe = only(reader.read());
        assertEquals(expected.nextLong(), random.nextLong());
        assertEquals(2, recipe.outcomes.size());
        assertEquals(.6, outcome(recipe, Items.DIAMOND).probability, 1E-14);
        assertEquals(.4, outcome(recipe, Items.EMERALD).probability, 1E-14);
        assertEquals(-1, recipe.getCommonAmount(0));
        assertEquals(-1, recipe.getCommonAmount(1));
        assertTrue(Double.isNaN(recipe.getCommonConsumption(0)));
        assertEquals(70, scripted.getComponent1().amount);
        assertEquals(80, scripted.getComponent2().amount);
        assertEquals(1, outcome(recipe, Items.EMERALD).branches.size());
        assertEquals(ChaliceRecipes.consumptionProbability(.8F),
                outcome(recipe, Items.EMERALD).branches.get(0).reaction.consumeFirst, 0);
    }

    @Test
    public void preservesPerOutcomeCostsAndCountsAndItemNbt() throws Exception {
        ItemStack diamond = new ItemStack(Items.DIAMOND, 2);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", 7);
        diamond.setTagCompound(tag);
        LiquidInteraction a = add(2, 10, 10, 0, 1, diamond);
        add(1, 70, 70, 1, 0, diamond);
        add(1, 10, 10, 1, 1, new ItemStack(Items.DIAMOND, 1));
        ChaliceRecipes.Snapshot before = reader.read();
        ChaliceRecipes.Recipe recipe = only(before);
        assertEquals(2, recipe.outcomes.size());
        ChaliceRecipes.Outcome grouped = recipe.outcomes.get(0);
        assertEquals(2, grouped.getOutput().getCount());
        assertEquals(2, grouped.branches.size());
        assertEquals(.75, grouped.probability, 0);
        assertEquals(.5, grouped.branches.get(0).probability, 0);
        assertEquals(0, grouped.branches.get(0).reaction.consumeFirst, 0);
        assertEquals(10, grouped.branches.get(0).reaction.getInput(0).amount);
        a.getComponent1().amount = 500;
        assertNotEquals(before, reader.read());
        assertEquals(10, grouped.branches.get(0).reaction.getInput(0).amount);
        ItemStack exposed = grouped.getOutput();
        exposed.setCount(63);
        exposed.getTagCompound().setInteger("variant", 99);
        assertEquals(2, grouped.getOutput().getCount());
        assertEquals(7, grouped.getOutput().getTagCompound().getInteger("variant"));
    }

    @Test
    public void keepsUnknownActionsInDenominatorAndDoesNotExecuteThem() throws Exception {
        add(3, 10, 10, 1, 1, new ItemStack(Items.DIAMOND));
        new LiquidInteraction(1, new FluidStack(FluidRegistry.WATER, 5),
                new FluidStack(FluidRegistry.LAVA, 8), new PreviewAction(ItemStack.EMPTY));
        new LiquidInteraction(2, new FluidStack(FluidRegistry.WATER, 5),
                new FluidStack(FluidRegistry.LAVA, 8), new PreviewAction(new ItemStack(Items.EMERALD)));
        ChaliceRecipes.Recipe recipe = only(reader.read());
        assertEquals(.5, outcome(recipe, Items.DIAMOND).probability, 0);
        assertEquals(1.0 / 6, recipe.otherProbability, 1E-14);
        assertTrue(outcome(recipe, Items.EMERALD).branches.get(0).reaction.customAction);
        assertTrue(Double.isNaN(outcome(recipe, Items.EMERALD).branches.get(0).reaction.consumeFirst));
    }

    @Test
    public void separatesReverseSelectionPoolsAndTaggedFluids() throws Exception {
        add(3, 10, 10, 1, 1, new ItemStack(Items.DIAMOND));
        new LiquidInteraction(7, new FluidStack(FluidRegistry.LAVA, 10),
                new FluidStack(FluidRegistry.WATER, 10),
                LiquidInteraction.createItemDropAction(1, 1, new ItemStack(Items.EMERALD)));
        FluidStack tagged = new FluidStack(FluidRegistry.WATER, 10);
        tagged.tag = new NBTTagCompound();
        tagged.tag.setString("grade", "special");
        new LiquidInteraction(1, tagged, new FluidStack(FluidRegistry.LAVA, 10),
                LiquidInteraction.createItemDropAction(1, 1, new ItemStack(Items.APPLE)));
        List<ChaliceRecipes.Recipe> recipes = reader.read().recipes();
        assertEquals(3, recipes.size());
        for (ChaliceRecipes.Recipe recipe : recipes) assertEquals(1, recipe.outcomes.get(0).probability, 0);
    }

    @Test
    public void doesNotNormalizeInvalidWeightsOrOfferZeroWeightOutcomes() throws Exception {
        add(3, 10, 10, 1, 1, new ItemStack(Items.DIAMOND));
        add(0, 70, 70, 1, 1, new ItemStack(Items.EMERALD));
        ChaliceRecipes.Recipe recipe = only(reader.read());
        assertEquals(1, recipe.outcomes.size());
        assertEquals(10, recipe.getCommonAmount(0));
        add(-1, 10, 10, 1, 1, new ItemStack(Items.APPLE));
        recipe = only(reader.read());
        assertFalse(recipe.validWeights);
        for (ChaliceRecipes.Outcome output : recipe.outcomes) assertTrue(Double.isNaN(output.probability));
        registryField().set(null, new ArrayList<LiquidInteraction>());
        add(Integer.MAX_VALUE, 10, 10, 1, 1, new ItemStack(Items.DIAMOND));
        add(1, 10, 10, 1, 1, new ItemStack(Items.EMERALD));
        assertFalse(only(reader.read()).validWeights);
    }

    @Test
    public void detectsSameSizedReloadRemovalAndRangeChanges() throws Exception {
        add(1, 10, 10, 1, 1, new ItemStack(Items.DIAMOND));
        ChaliceRecipes.Snapshot before = reader.read();
        assertEquals(before, reader.read());
        assertEquals(before.hashCode(), reader.read().hashCode());
        registryField().set(null, new ArrayList<LiquidInteraction>());
        add(1, 10, 10, 1, 1, new ItemStack(Items.EMERALD));
        assertNotEquals(before, reader.read());
        ChaliceRecipes.Snapshot replacement = reader.read();
        ConfigEntryChalice.chaliceRange = 32;
        assertNotEquals(replacement, reader.read());
        registryField().set(null, new ArrayList<LiquidInteraction>());
        assertTrue(reader.read().recipes().isEmpty());
    }

    @Test
    public void removalUsesWhatRemainsInBothRuntimeLists() throws Exception {
        add(3, 10, 10, 1, 1, new ItemStack(Items.DIAMOND));
        LiquidInteraction scripted = add(1, 10, 10, 1, 1, new ItemStack(Items.EMERALD));
        LiquidInteraction.mtInteractions.add(scripted);
        LiquidInteraction.removeInteraction(FluidRegistry.WATER, FluidRegistry.LAVA, new ItemStack(Items.EMERALD));
        assertEquals(.25, outcome(only(reader.read()), Items.EMERALD).probability, 0);
    }

    @Test(timeout = 10000)
    public void manyAlternativeCostsGroupWithoutQuadraticBranchSearch() {
        List<ChaliceRecipes.Reaction> reactions = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) reactions.add(new ChaliceRecipes.Reaction(
                new FluidStack(FluidRegistry.WATER, i), new FluidStack(FluidRegistry.LAVA, 10),
                new ItemStack(Items.DIAMOND), 1, 1, 1, false, false));
        ChaliceRecipes.Recipe recipe = only(new ChaliceRecipes.Snapshot(reactions, 16));
        assertEquals(1, recipe.outcomes.size());
        assertEquals(10000, recipe.outcomes.get(0).branches.size());
        assertEquals(1, recipe.outcomes.get(0).probability, 1E-12);
    }

    @Test
    public void consumptionMatchesAsFloatComparisonEdges() {
        assertEquals(0, ChaliceRecipes.consumptionProbability(Float.NaN), 0);
        assertEquals(0, ChaliceRecipes.consumptionProbability(-2), 0);
        assertEquals(1, ChaliceRecipes.consumptionProbability(2), 0);
        assertEquals(1.0 / (1 << 24), ChaliceRecipes.consumptionProbability(Float.MIN_VALUE), 0);
    }

    private static Field registryField() throws Exception {
        Field field = LiquidInteraction.class.getDeclaredField("registeredInteractions");
        field.setAccessible(true);
        return field;
    }

    public static LiquidInteraction add(int weight, int first, int second, float chance1, float chance2, ItemStack output) {
        return new LiquidInteraction(weight, new FluidStack(FluidRegistry.WATER, first),
                new FluidStack(FluidRegistry.LAVA, second), LiquidInteraction.createItemDropAction(chance1, chance2, output));
    }

    private static ChaliceRecipes.Recipe only(ChaliceRecipes.Snapshot snapshot) {
        List<ChaliceRecipes.Recipe> recipes = snapshot.recipes();
        assertEquals(1, recipes.size());
        return recipes.get(0);
    }

    private static ChaliceRecipes.Outcome outcome(ChaliceRecipes.Recipe recipe, net.minecraft.item.Item item) {
        for (ChaliceRecipes.Outcome outcome : recipe.outcomes) if (outcome.getOutput().getItem() == item) return outcome;
        throw new AssertionError("Missing output " + item);
    }

    private static final class PreviewAction extends FluidInteractionAction {
        private final ItemStack output;
        PreviewAction(ItemStack output) { this.output = output; }
        @Override public ItemStack getOutputForMatching() { return output; }
        @Override public boolean drainComponent1(FluidStack input, IFluidHandler tank) { throw new AssertionError("Reader drained fluid"); }
        @Override public boolean drainComponent2(FluidStack input, IFluidHandler tank) { throw new AssertionError("Reader drained fluid"); }
        @Override public void doInteraction(World world, Vector3 pos) { throw new AssertionError("Reader executed interaction"); }
    }
}
