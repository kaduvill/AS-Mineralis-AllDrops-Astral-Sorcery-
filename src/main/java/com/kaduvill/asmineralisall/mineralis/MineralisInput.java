package com.kaduvill.asmineralisall.mineralis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/** One configured replacement entry, with all of its displayable variants. */
public final class MineralisInput {
    private final List<ItemStack> variants;

    public MineralisInput(List<ItemStack> variants) {
        if (variants.isEmpty()) throw new IllegalArgumentException("An input needs at least one variant");
        List<ItemStack> copies = new ArrayList<>(variants.size());
        for (ItemStack variant : variants) copies.add(variant.copy());
        this.variants = Collections.unmodifiableList(copies);
    }

    public List<ItemStack> getVariants() {
        List<ItemStack> copies = new ArrayList<>(variants.size());
        for (ItemStack variant : variants) copies.add(variant.copy());
        return copies;
    }
}
