package com.kaduvill.asjeialldrops.ore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/** Loaded mechanic data; no player, world, config, or registry references survive registration. */
public final class OrePage {
    public enum Kind {
        ENRICHMENT("stone_enrichment"), TRASH("trash_to_treasure"), SHRINE("treasure_shrine");
        public final String key;
        Kind(String key) { this.key = key; }
        public String uid() { return "asjeialldrops." + key; }
    }

    public final Kind kind;
    public final OreTable table;
    public final int baseInterval;
    public final int baseRange;
    public final double baseReplacement;
    public final int regenerations;
    private final List<ItemStack> inputs;

    OrePage(Kind kind, OreTable table, List<ItemStack> inputs, int interval, int range,
            double replacement, int regenerations) {
        this.kind = kind;
        this.table = table;
        this.inputs = Collections.unmodifiableList(copy(inputs));
        this.baseInterval = interval;
        this.baseRange = range;
        this.baseReplacement = replacement;
        this.regenerations = regenerations;
    }

    public List<ItemStack> getInputs() { return copy(inputs); }
    public boolean hasInputs() { return !inputs.isEmpty(); }
    OrePage withInputs(List<ItemStack> inputs) {
        return new OrePage(kind, table, inputs, baseInterval, baseRange, baseReplacement, regenerations);
    }
    private static List<ItemStack> copy(List<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack stack : source) copy.add(stack.copy());
        return copy;
    }
}
