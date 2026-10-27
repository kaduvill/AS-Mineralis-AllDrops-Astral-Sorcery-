package com.kaduvill.asjeialldrops.ore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.item.ItemStack;

/** Concrete outputs and unconditional probabilities for one call to the ore selector. */
public final class OreTable {
    public final List<Output> outputs;
    public final double noResult;

    private OreTable(List<Output> outputs, double noResult) {
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
        this.noResult = noResult;
    }

    public static final class Output {
        private final ItemStack stack;
        public final double probability;
        private Output(ItemStack stack, double probability) {
            this.stack = stack.copy();
            this.probability = probability;
        }
        public ItemStack getStack() { return stack.copy(); }
    }

    public static final class Builder {
        private final Map<Key, Double> grouped = new LinkedHashMap<>();
        public void add(ItemStack stack, double probability) {
            if (!Double.isFinite(probability) || probability < 0) throw new IllegalArgumentException("Invalid ore probability");
            if (probability == 0) return;
            if (stack.isEmpty()) throw new IllegalArgumentException("Empty ore output");
            grouped.merge(new Key(stack), probability, Double::sum);
        }
        public OreTable build(double noResult) {
            if (!Double.isFinite(noResult) || noResult < 0 || noResult > 1) throw new IllegalArgumentException("Invalid failure probability");
            List<Output> outputs = new ArrayList<>(grouped.size());
            double sum = noResult;
            for (Map.Entry<Key, Double> entry : grouped.entrySet()) {
                outputs.add(new Output(entry.getKey().stack, entry.getValue()));
                sum += entry.getValue();
            }
            if (Math.abs(sum - 1) > 1E-10) throw new IllegalArgumentException("Ore probabilities do not sum to one");
            return new OreTable(outputs, noResult);
        }
    }

    private static final class Key {
        final ItemStack stack;
        Key(ItemStack stack) { this.stack = stack.copy(); }
        @Override public boolean equals(Object value) {
            return value instanceof Key && ItemStack.areItemStacksEqual(stack, ((Key) value).stack);
        }
        @Override public int hashCode() {
            return Objects.hash(stack.getItem(), stack.getItemDamage(), stack.getCount(), stack.getTagCompound());
        }
    }
}
