package com.kaduvill.asjeialldrops.ore;

import hellfirepvp.astralsorcery.common.block.BlockCustomOre.OreType;
import hellfirepvp.astralsorcery.common.constellation.perk.AbstractPerk;
import hellfirepvp.astralsorcery.common.constellation.perk.tree.PerkTree;
import hellfirepvp.astralsorcery.common.constellation.perk.tree.nodes.key.KeyStoneEnrichment;
import hellfirepvp.astralsorcery.common.constellation.perk.tree.nodes.key.KeyVoidTrash;
import hellfirepvp.astralsorcery.common.tile.TileOreGenerator.ConfigEntryMultiOre;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

/** Mechanic-specific rules applied to the loaded ore tables, only during JEI registration. */
public final class AstralOrePages {
    private final AstralOreTableReader tables = new AstralOreTableReader();
    private List<?> trashFilters = Collections.emptyList();
    public AstralOrePages() throws ReflectiveOperationException {}

    public OrePage enrichment() throws ReflectiveOperationException {
        AbstractPerk perk = PerkTree.PERK_TREE.getAstralSorceryPerk("key_stone_enrichment");
        if (!(perk instanceof KeyStoneEnrichment)) return null;
        int interval = AstralOreTableReader.field(KeyStoneEnrichment.class, "chanceToEnrich").getInt(perk);
        int radius = AstralOreTableReader.field(KeyStoneEnrichment.class, "enrichmentRadius").getInt(perk);
        return new OrePage(OrePage.Kind.ENRICHMENT, tables.read("AEVITAS_ORE_PERK", true),
                Collections.singletonList(new ItemStack(Blocks.STONE)), Math.max(1, interval), radius, 0, 0);
    }

    public OrePage trash() throws ReflectiveOperationException {
        if (!(PerkTree.PERK_TREE.getAstralSorceryPerk("key_void_trash") instanceof KeyVoidTrash)) return null;
        trashFilters = new ArrayList<>((List<?>) AstralOreTableReader.field(KeyVoidTrash.class, "dropFilter").get(null));
        float chance = AstralOreTableReader.field(KeyVoidTrash.class, "chanceOre").getFloat(null);
        double probability = !(chance > 0) ? 0 : chance >= 1 ? 1 : Math.ceil((double) chance * (1 << 24)) / (1 << 24);
        return new OrePage(OrePage.Kind.TRASH, tables.read("PERK_VOID_TRASH_REPLACEMENT", false),
                Collections.emptyList(), 0, 0, probability, 0);
    }

    public OrePage resolveTrashInputs(OrePage page, Collection<ItemStack> ingredients) {
        List<ItemStack> matching = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            ItemStack copy = ingredient.copy();
            copy.setCount(1);
            for (Object filter : trashFilters) {
                if (((Predicate<ItemStack>) filter).test(copy)) {
                    matching.add(copy.copy());
                    break;
                }
            }
        }
        if (matching.isEmpty()) return null;
        return page.withInputs(matching);
    }

    public OrePage shrine() throws ReflectiveOperationException {
        int oreChance = AstralOreTableReader.field(ConfigEntryMultiOre.class, "oreChance").getInt(null);
        int regenerations = AstralOreTableReader.field(ConfigEntryMultiOre.class, "guaranteedOres").getInt(null);
        if (oreChance < 0 || regenerations < 0) throw new IllegalStateException("Invalid Treasure Shrine configuration");
        ItemStack crystal = oreChance == 0 ? ItemStack.EMPTY : OreType.ROCK_CRYSTAL.asStack();
        if (crystal.isEmpty() && oreChance > 0) throw new IllegalStateException("Rock crystal ore is unavailable");
        OreTable ores = oreChance == 0 ? new OreTable.Builder().build(1) : tables.read("TREASURE_SHRINE_GEN", true);
        return new OrePage(OrePage.Kind.SHRINE, shrineTable(ores, oreChance, crystal),
                Collections.emptyList(), 0, 0, 0, regenerations);
    }

    public static OreTable shrineTable(OreTable ores, int oreChance, ItemStack crystal) {
        if (oreChance < 0) throw new IllegalArgumentException("Negative shrine ore chance");
        double oreRoll = oreChance == 0 ? 0 : 1.0 / oreChance;
        double tableRoll = oreRoll * (199.0 / 200);
        OreTable.Builder result = new OreTable.Builder();
        result.add(new ItemStack(Blocks.STONE), 1 - oreRoll + tableRoll * ores.noResult);
        if (oreRoll > 0) result.add(crystal, oreRoll / 200);
        for (OreTable.Output output : ores.outputs) result.add(output.getStack(), tableRoll * output.probability);
        return result.build(0);
    }
}
