package com.kaduvill.asmineralisall.loot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

/** Read-only interoperability adapter for the inspected Astral Sorcery 1.10.27 release. */
public final class AstralMineralisReader {
    private static final String AS = "hellfirepvp.astralsorcery.common.";
    private static final double STARMETAL_BONUS = 1.0 / 2_000_000;
    private static final int MAX_ATTEMPTS = 150;
    private final Object registry;
    private final Field entriesField;
    private final Field totalWeightField;
    private final Field nameField;
    private final Field weightField;
    private final Field blacklistField;
    private final Field enabledField;
    private final Field searchRangeField;
    private final Field inputEntriesField;
    private final Method createBlockState;
    private final ItemStack starmetal;

    public AstralMineralisReader() throws ReflectiveOperationException {
        Class<?> oreTypes = Class.forName(AS + "base.OreTypes");
        registry = oreTypes.getField("RITUAL_MINERALIS").get(null);
        entriesField = privateField(oreTypes, "oreDictWeights");
        totalWeightField = privateField(oreTypes, "totalWeight");
        Class<?> entry = Class.forName(AS + "base.sets.OreEntry");
        nameField = entry.getField("oreName");
        weightField = entry.getField("weight");
        blacklistField = Class.forName(AS + "data.config.Config").getField("modidOreGenBlacklist");
        Class<?> effect = Class.forName(AS + "constellation.effect.aoe.CEffectMineralis");
        enabledField = effect.getField("enabled");
        searchRangeField = effect.getField("searchRange");
        inputEntriesField = privateField(effect, "replaceableStatesSerialized");
        createBlockState = Class.forName(AS + "util.ItemUtils").getMethod("createBlockState", ItemStack.class);
        Block block = (Block) Class.forName(AS + "lib.BlocksAS").getField("customOre").get(null);
        Enum<?> type = (Enum<?>) Class.forName(AS + "block.BlockCustomOre$OreType").getField("STARMETAL").get(null);
        starmetal = new ItemStack(block, 1, type.ordinal());
        if (starmetal.isEmpty()) throw new IllegalStateException("Starmetal ore is unavailable");
    }

    public Snapshot read() throws ReflectiveOperationException {
        if (!enabledField.getBoolean(null)) return Snapshot.empty();
        // Copy the registry structure, never its contents back into Astral Sorcery.
        List<?> entries = new ArrayList<>((List<?>) entriesField.get(registry));
        double totalWeight = totalWeightField.getDouble(registry);
        Set<String> blacklist = new HashSet<>();
        for (Object modId : (List<?>) blacklistField.get(null)) blacklist.add((String) modId);
        int[] weights = new int[entries.size()];
        List<ResolvedOre> resolved = new ArrayList<>(entries.size());
        boolean[] valid = new boolean[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            Object entry = entries.get(index);
            weights[index] = weightField.getInt(entry);
            ResolvedOre ore = resolve((String) nameField.get(entry), blacklist);
            resolved.add(ore);
            valid[index] = ore != null;
        }

        double[] draws = MineralisProbabilities.selectionProbabilities(weights, totalWeight);
        MineralisProbabilities.Result result = MineralisProbabilities.calculate(draws, valid, STARMETAL_BONUS, MAX_ATTEMPTS);
        double[] chances = result.getProbabilities();
        List<MutableDrop> grouped = new ArrayList<>();
        double noResultProbability = result.getNoResultProbability();
        for (int index = 0; index < chances.length; index++) {
            if (!valid[index] || chances[index] <= 0) continue;
            ResolvedOre ore = resolved.get(index);
            if (ore.state == null) noResultProbability += chances[index];
            else add(grouped, ore, chances[index]);
        }
        ResolvedOre bonus = placedOre(starmetal.copy());
        if (bonus.state == null) throw new IllegalStateException("Starmetal ore cannot be represented as a block");
        add(grouped, bonus, result.getBonusProbability());
        List<MineralisDrop> drops = new ArrayList<>(grouped.size());
        for (MutableDrop drop : grouped) drops.add(new MineralisDrop(drop.ore.stack, drop.probability));
        return new Snapshot(readInputs(), drops, noResultProbability, searchRangeField.getInt(null));
    }

    private List<MineralisInput> readInputs() throws IllegalAccessException {
        List<String> loaded = new ArrayList<>();
        for (Object entry : (List<?>) inputEntriesField.get(null)) loaded.add((String) entry);
        return MineralisInputs.resolve(loaded);
    }

    private ResolvedOre resolve(String oreName, Set<String> blacklist) throws ReflectiveOperationException {
        // AS selects the last eligible registration. Reverse lookup also avoids its selector's
        // mutation of wildcard stacks: normalize only our private copy.
        List<ItemStack> candidates = OreDictionary.getOres(oreName, false);
        for (int index = candidates.size() - 1; index >= 0; index--) {
            ItemStack candidate = candidates.get(index);
            if (candidate == null || candidate.isEmpty()) continue;
            Block block = Block.getBlockFromItem(candidate.getItem());
            ResourceLocation id = candidate.getItem().getRegistryName();
            if (block == Blocks.AIR || id == null || blacklist.contains(id.getNamespace())) continue;
            if (candidate.getItem().getClass().getName().toLowerCase(Locale.ROOT).contains("greg")) continue;
            ItemStack copy = candidate.copy();
            if (copy.getItemDamage() == OreDictionary.WILDCARD_VALUE) copy.setItemDamage(0);
            return placedOre(copy);
        }
        return null;
    }

    private ResolvedOre placedOre(ItemStack candidate) throws ReflectiveOperationException {
        // Ask the installed mod to interpret metadata, including its fallback for invalid metadata.
        IBlockState state = (IBlockState) createBlockState.invoke(null, candidate);
        // A selected stack with no state does not reroll in AS; retain it as a failed placement.
        if (state == null) return new ResolvedOre(null, candidate);
        ItemStack display = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));
        if (display.isEmpty()) display = candidate.copy();
        display.setCount(1);
        display.setTagCompound(null);
        return new ResolvedOre(state, display);
    }

    private static void add(List<MutableDrop> drops, ResolvedOre ore, double probability) {
        for (MutableDrop drop : drops) {
            if (drop.ore.state.equals(ore.state) || ItemStack.areItemsEqual(drop.ore.stack, ore.stack)) {
                drop.probability += probability;
                return;
            }
        }
        drops.add(new MutableDrop(ore, probability));
    }

    private static Field privateField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class ResolvedOre {
        final IBlockState state;
        final ItemStack stack;

        ResolvedOre(IBlockState state, ItemStack stack) {
            this.state = state;
            this.stack = stack;
        }
    }

    private static final class MutableDrop {
        final ResolvedOre ore;
        double probability;

        MutableDrop(ResolvedOre ore, double probability) {
            this.ore = ore;
            this.probability = probability;
        }
    }

    public static final class Snapshot {
        private final List<MineralisInput> inputs;
        private final List<MineralisDrop> drops;
        private final double noResultProbability;
        private final int searchRange;

        Snapshot(List<MineralisInput> inputs, List<MineralisDrop> drops, double noResultProbability, int searchRange) {
            this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
            this.drops = Collections.unmodifiableList(new ArrayList<>(drops));
            this.noResultProbability = noResultProbability;
            this.searchRange = searchRange;
        }

        public static Snapshot empty() {
            return new Snapshot(Collections.emptyList(), Collections.emptyList(), 0, 0);
        }

        public List<MineralisInput> getInputs() {
            return inputs;
        }

        public List<MineralisDrop> getDrops() {
            return drops;
        }

        public double getNoResultProbability() {
            return noResultProbability;
        }

        public int getSearchRange() {
            return searchRange;
        }
    }
}
