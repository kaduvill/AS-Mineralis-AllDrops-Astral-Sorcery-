package com.kaduvill.asjeialldrops.ore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

/** Read-only adapter for AS 1.10.27's OreTypes. Never invokes its random/mutating selector. */
public final class AstralOreTableReader {
    private static final String AS = "hellfirepvp.astralsorcery.common.";
    private final Class<?> oreTypes = Class.forName(AS + "base.OreTypes");
    private final Field entries = field(oreTypes, "oreDictWeights");
    private final Field total = field(oreTypes, "totalWeight");
    private final Field name = Class.forName(AS + "base.sets.OreEntry").getField("oreName");
    private final Field weight = Class.forName(AS + "base.sets.OreEntry").getField("weight");
    private final Field blacklist = Class.forName(AS + "data.config.Config").getField("modidOreGenBlacklist");
    private final Method blockState = Class.forName(AS + "util.ItemUtils").getMethod("createBlockState", ItemStack.class);

    public AstralOreTableReader() throws ReflectiveOperationException {}

    public OreTable read(String registryName, boolean placedBlocks) throws ReflectiveOperationException {
        Object registry = oreTypes.getField(registryName).get(null);
        List<?> loaded = new ArrayList<>((List<?>) entries.get(registry));
        Set<String> excluded = new HashSet<>();
        for (Object modId : (List<?>) blacklist.get(null)) excluded.add((String) modId);
        int[] weights = new int[loaded.size()];
        boolean[] valid = new boolean[loaded.size()];
        List<ItemStack> stacks = new ArrayList<>(loaded.size());
        for (int i = 0; i < loaded.size(); i++) {
            Object entry = loaded.get(i);
            weights[i] = weight.getInt(entry);
            ItemStack selected = resolve((String) name.get(entry), excluded);
            // A nonempty selected stack ends AS's retries, even if block placement then fails.
            valid[i] = !selected.isEmpty();
            stacks.add(placedBlocks && valid[i] ? placedOutput(selected) : selected);
        }
        OreProbabilities.Result chances = OreProbabilities.calculate(
                OreProbabilities.selectionProbabilities(weights, total.getDouble(registry)), valid, 0, 150);
        double[] probabilities = chances.getProbabilities();
        double failed = chances.getNoResultProbability();
        OreTable.Builder outputs = new OreTable.Builder();
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isEmpty()) failed += probabilities[i];
            else outputs.add(stacks.get(i), probabilities[i]);
        }
        return outputs.build(Math.min(1, failed));
    }

    private static ItemStack resolve(String oreName, Set<String> excluded) {
        List<ItemStack> candidates = OreDictionary.getOres(oreName, false);
        // AS walks forward and keeps the last eligible stack. Work on a copy of that stack.
        for (int i = candidates.size() - 1; i >= 0; i--) {
            ItemStack candidate = candidates.get(i);
            if (candidate == null || candidate.isEmpty()) continue;
            ResourceLocation id = candidate.getItem().getRegistryName();
            if (id == null || excluded.contains(id.getNamespace())
                    || Block.getBlockFromItem(candidate.getItem()) == Blocks.AIR
                    || candidate.getItem().getClass().getName().toLowerCase(Locale.ROOT).contains("greg")) continue;
            ItemStack copy = candidate.copy();
            if (copy.getItemDamage() == OreDictionary.WILDCARD_VALUE) copy.setItemDamage(0);
            copy.setCount(1);
            return copy;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack placedOutput(ItemStack selected) throws ReflectiveOperationException {
        IBlockState state = (IBlockState) blockState.invoke(null, selected);
        if (state == null) return ItemStack.EMPTY;
        ItemStack display = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));
        if (display.isEmpty()) display = selected.copy();
        display.setTagCompound(null);
        return display;
    }

    static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
