package com.kaduvill.asjeialldrops.chalice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Immutable, read-only descriptions of the selection pools used by TileChalice. */
public final class ChaliceRecipes {
    private ChaliceRecipes() {
    }

    public static final class Reaction {
        private final FluidStack first;
        private final FluidStack second;
        private final ItemStack output;
        public final int weight;
        public final double consumeFirst;
        public final double consumeSecond;
        public final boolean customAction;
        public final boolean randomCrystal;

        public Reaction(FluidStack first, FluidStack second, ItemStack output, int weight,
                        double consumeFirst, double consumeSecond, boolean customAction, boolean randomCrystal) {
            this.first = first.copy();
            this.second = second.copy();
            this.output = output.isEmpty() ? ItemStack.EMPTY : output.copy();
            this.weight = weight;
            this.consumeFirst = consumeFirst;
            this.consumeSecond = consumeSecond;
            this.customAction = customAction;
            this.randomCrystal = randomCrystal;
        }

        public FluidStack getInput(int index) {
            return (index == 0 ? first : second).copy();
        }

        public ItemStack getOutput() {
            return output.copy();
        }

        public double getConsumption(int index) {
            return index == 0 ? consumeFirst : consumeSecond;
        }

        private boolean sameCost(Reaction other) {
            return first.amount == other.first.amount && second.amount == other.second.amount
                    && Double.compare(consumeFirst, other.consumeFirst) == 0
                    && Double.compare(consumeSecond, other.consumeSecond) == 0
                    && customAction == other.customAction && randomCrystal == other.randomCrystal;
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof Reaction)) return false;
            Reaction other = (Reaction) value;
            return weight == other.weight && sameCost(other)
                    && first.isFluidEqual(other.first) && second.isFluidEqual(other.second)
                    && ItemStack.areItemStacksEqual(output, other.output);
        }

        @Override
        public int hashCode() {
            return Objects.hash(new FluidKey(first), new FluidKey(second), first.amount, second.amount,
                    new ItemKey(output), weight, consumeFirst, consumeSecond, customAction, randomCrystal);
        }
    }

    public static final class Snapshot {
        private final List<Reaction> reactions;
        public final float range;

        public Snapshot(List<Reaction> reactions, float range) {
            this.reactions = Collections.unmodifiableList(new ArrayList<>(reactions));
            this.range = range;
        }

        public List<Recipe> recipes() {
            // The order is intentional: A->B and B->A are independent selection pools in AS.
            Map<PairKey, List<Reaction>> pools = new LinkedHashMap<>();
            for (Reaction reaction : reactions) {
                PairKey key = new PairKey(reaction.first, reaction.second);
                pools.computeIfAbsent(key, ignored -> new ArrayList<>()).add(reaction);
            }
            List<Recipe> recipes = new ArrayList<>(pools.size());
            for (List<Reaction> pool : pools.values()) recipes.add(new Recipe(pool, range));
            return Collections.unmodifiableList(recipes);
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof Snapshot)) return false;
            Snapshot other = (Snapshot) value;
            return Float.compare(range, other.range) == 0 && reactions.equals(other.reactions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reactions, range);
        }
    }

    public static final class Branch {
        public final Reaction reaction;
        public final double probability;

        private Branch(Reaction reaction, double probability) {
            this.reaction = reaction;
            this.probability = probability;
        }
    }

    public static final class Outcome {
        private final ItemStack output;
        public final List<Branch> branches;
        public final double probability;

        private Outcome(ItemStack output, List<Branch> branches) {
            this.output = output.copy();
            this.branches = Collections.unmodifiableList(new ArrayList<>(branches));
            double sum = 0;
            for (Branch branch : branches) sum += branch.probability;
            this.probability = sum;
        }

        public ItemStack getOutput() {
            return output.copy();
        }
    }

    public static final class Recipe {
        private final List<Reaction> selections;
        public final List<Outcome> outcomes;
        public final float range;
        public final boolean validWeights;
        public final double otherProbability;
        private final int[] commonAmounts = new int[2];
        private final double[] commonConsumption = new double[2];

        private Recipe(List<Reaction> pool, float range) {
            this.range = range;
            long total = 0;
            boolean valid = true;
            for (Reaction reaction : pool) {
                total += reaction.weight;
                if (reaction.weight < 0) valid = false;
            }
            // Vanilla's selector sums into a signed int. Never invent percentages after overflow
            // or silently remove negative weights from a pool that AS itself cannot use normally.
            this.validWeights = valid && total > 0 && total <= Integer.MAX_VALUE;
            Map<ItemKey, Map<CostKey, Branch>> grouped = new LinkedHashMap<>();
            List<Reaction> selectable = new ArrayList<>();
            double other = 0;
            for (Reaction reaction : pool) {
                double probability = validWeights ? reaction.weight / (double) total : Double.NaN;
                if (probability == 0) continue;
                selectable.add(reaction);
                if (reaction.output.isEmpty()) {
                    other += probability;
                    continue;
                }
                ItemKey key = new ItemKey(reaction.output);
                Map<CostKey, Branch> branches = grouped.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
                CostKey cost = new CostKey(reaction);
                Branch previous = branches.get(cost);
                branches.put(cost, new Branch(reaction, previous == null ? probability : previous.probability + probability));
            }
            this.selections = Collections.unmodifiableList(selectable.isEmpty() ? new ArrayList<>(pool) : selectable);
            for (int i = 0; i < 2; i++) {
                int amount = i == 0 ? selections.get(0).first.amount : selections.get(0).second.amount;
                double consumption = selections.get(0).getConsumption(i);
                for (Reaction reaction : selections) {
                    int required = i == 0 ? reaction.first.amount : reaction.second.amount;
                    if (required != amount) commonAmounts[i] = -1;
                    if (Double.compare(consumption, reaction.getConsumption(i)) != 0) consumption = Double.NaN;
                }
                if (commonAmounts[i] != -1) commonAmounts[i] = amount;
                commonConsumption[i] = consumption;
            }
            this.otherProbability = other;
            List<Outcome> outputs = new ArrayList<>(grouped.size());
            for (Map.Entry<ItemKey, Map<CostKey, Branch>> entry : grouped.entrySet()) {
                outputs.add(new Outcome(entry.getKey().stack, new ArrayList<>(entry.getValue().values())));
            }
            // Stable ties retain the registry order. Each count/NBT variant remains a concrete slot.
            if (validWeights) outputs.sort((left, right) -> Double.compare(right.probability, left.probability));
            this.outcomes = Collections.unmodifiableList(outputs);
        }

        public FluidStack getInput(int index) {
            FluidStack fluid = selections.get(0).getInput(index);
            int amount = getCommonAmount(index);
            fluid.amount = amount >= 0 ? amount : 1;
            return fluid;
        }

        /** -1 means the amount depends on the selected outcome. */
        public int getCommonAmount(int index) {
            return commonAmounts[index];
        }

        /** NaN means variable or unavailable, never an assumed 100% drain. */
        public double getCommonConsumption(int index) {
            return commonConsumption[index];
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof Recipe)) return false;
            Recipe other = (Recipe) value;
            return Float.compare(range, other.range) == 0 && selections.equals(other.selections);
        }

        @Override
        public int hashCode() { return Objects.hash(selections, range); }
    }

    /** Probability of Random.nextFloat() < chance, including AS's out-of-range float behavior. */
    public static double consumptionProbability(float chance) {
        if (!(chance > 0)) return 0;
        if (chance >= 1) return 1;
        return Math.ceil((double) chance * (1 << 24)) / (1 << 24);
    }

    private static final class FluidKey {
        final Fluid fluid;
        final NBTTagCompound tag;

        FluidKey(FluidStack stack) {
            fluid = stack.getFluid();
            tag = stack.tag == null ? null : stack.tag.copy();
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof FluidKey)) return false;
            FluidKey other = (FluidKey) value;
            return fluid == other.fluid && Objects.equals(tag, other.tag);
        }

        @Override
        public int hashCode() { return Objects.hash(fluid, tag); }
    }

    private static final class CostKey {
        final Reaction reaction;

        CostKey(Reaction reaction) { this.reaction = reaction; }

        @Override
        public boolean equals(Object value) {
            return value instanceof CostKey && reaction.sameCost(((CostKey) value).reaction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reaction.first.amount, reaction.second.amount, reaction.consumeFirst,
                    reaction.consumeSecond, reaction.customAction, reaction.randomCrystal);
        }
    }

    private static final class PairKey {
        final FluidKey first;
        final FluidKey second;

        PairKey(FluidStack first, FluidStack second) {
            this.first = new FluidKey(first);
            this.second = new FluidKey(second);
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof PairKey)) return false;
            PairKey other = (PairKey) value;
            return first.equals(other.first) && second.equals(other.second);
        }

        @Override
        public int hashCode() { return Objects.hash(first, second); }
    }

    private static final class ItemKey {
        final ItemStack stack;

        ItemKey(ItemStack stack) { this.stack = stack.copy(); }

        @Override
        public boolean equals(Object value) {
            return value instanceof ItemKey && ItemStack.areItemStacksEqual(stack, ((ItemKey) value).stack);
        }

        @Override
        public int hashCode() {
            return stack.isEmpty() ? 0 : Objects.hash(stack.getItem(), stack.getMetadata(),
                    stack.getCount(), stack.getTagCompound());
        }
    }
}
