package com.kaduvill.asmineralisall.mineralis;

import net.minecraft.item.ItemStack;

public final class MineralisDrop {
    private final ItemStack stack;
    private final double probability;

    public MineralisDrop(ItemStack stack, double probability) {
        this.stack = stack.copy();
        this.stack.setCount(1);
        this.probability = probability;
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    public double getProbability() {
        return probability;
    }
}
