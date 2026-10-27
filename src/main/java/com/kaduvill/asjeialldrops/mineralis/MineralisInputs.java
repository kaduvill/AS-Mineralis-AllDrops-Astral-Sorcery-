package com.kaduvill.asjeialldrops.mineralis;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

final class MineralisInputs {
    private MineralisInputs() {
    }

    static List<MineralisInput> resolve(List<String> entries) {
        List<MineralisInput> inputs = new ArrayList<>(entries.size());
        for (String entry : entries) {
            MineralisInput input = resolve(entry);
            if (input != null) inputs.add(input);
        }
        return Collections.unmodifiableList(inputs);
    }

    static MineralisInput resolve(String entry) {
        try {
            String[] parts = entry.split(":", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Expected modid:block:metadata");
            int metadata = Integer.parseInt(parts[2]);
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(parts[0], parts[1]));
            if (block == null || block == Blocks.AIR) throw new IllegalArgumentException("Block is unavailable");
            List<ItemStack> variants = new ArrayList<>();
            if (metadata == -1) {
                for (IBlockState state : block.getBlockState().getValidStates()) addVariant(variants, state);
            } else {
                addVariant(variants, block.getStateFromMeta(metadata));
            }
            if (variants.isEmpty()) throw new IllegalArgumentException("Block has no displayable item");
            return new MineralisInput(variants);
        } catch (RuntimeException failure) {
            ASJeiAllDrops.LOGGER.warn("Cannot display Mineralis input '{}': {}", entry, failure.getMessage());
            return null;
        }
    }

    private static void addVariant(List<ItemStack> variants, IBlockState state) {
        if (state == null) return;
        Block block = state.getBlock();
        ItemStack stack = new ItemStack(block, 1, block.getMetaFromState(state));
        if (stack.isEmpty()) return;
        for (ItemStack existing : variants) if (ItemStack.areItemsEqual(existing, stack)) return;
        variants.add(stack);
    }
}
