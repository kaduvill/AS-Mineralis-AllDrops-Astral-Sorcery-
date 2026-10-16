package com.kaduvill.asmineralisall.jei;

import com.kaduvill.asmineralisall.ASMineralisAllDrops;
import com.kaduvill.asmineralisall.loot.AstralMineralisReader;
import com.kaduvill.asmineralisall.loot.AstralMineralisReader.Snapshot;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.item.crystal.CrystalProperties;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import java.util.Collections;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@JEIPlugin
public final class MineralisJeiPlugin implements IModPlugin {
    private Snapshot snapshot;

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        if (ItemsAS.tunedCelestialCrystal == null
                || registry.hasSubtypeInterpreter(new ItemStack(ItemsAS.tunedCelestialCrystal))) return;
        registry.registerSubtypeInterpreter(ItemsAS.tunedCelestialCrystal, stack -> {
            // AS's getter can create missing NBT; inspect a copy to keep JEI's stacks unchanged.
            IWeakConstellation constellation = ItemTunedCrystalBase.getMainConstellation(stack.copy());
            return constellation == null ? ISubtypeRegistry.ISubtypeInterpreter.NONE
                    : constellation.getUnlocalizedName();
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        snapshot = readSafely();
        registry.addRecipeCategories(new MineralisRecipeCategory(registry.getJeiHelpers().getGuiHelper(),
                snapshot.getInputs().size(), snapshot.getDrops().size()));
    }

    @Override
    public void register(IModRegistry registry) {
        MineralisRecipeWrapper recipe = wrap(snapshot);
        if (recipe != null) registry.addRecipes(Collections.singletonList(recipe), MineralisRecipeCategory.UID);
        Item pedestal = Item.getByNameOrId("astralsorcery:blockritualpedestal");
        if (pedestal != null) registry.addRecipeCatalyst(new ItemStack(pedestal), MineralisRecipeCategory.UID);
        if (ItemsAS.tunedCelestialCrystal != null && Constellations.mineralis != null) {
            ItemStack crystal = new ItemStack(ItemsAS.tunedCelestialCrystal);
            CrystalProperties.applyCrystalProperties(crystal, CrystalProperties.getMaxCelestialProperties());
            ItemTunedCrystalBase.applyMainConstellation(crystal, Constellations.mineralis);
            registry.addRecipeCatalyst(crystal, MineralisRecipeCategory.UID);
        }
    }

    private static Snapshot readSafely() {
        try {
            return new AstralMineralisReader().read();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ASMineralisAllDrops.LOGGER.warn("Mineralis JEI page disabled: cannot read the installed Astral Sorcery table", failure);
            return Snapshot.empty();
        }
    }

    private static MineralisRecipeWrapper wrap(Snapshot snapshot) {
        return snapshot.getDrops().isEmpty() ? null
                : new MineralisRecipeWrapper(snapshot.getInputs(), snapshot.getDrops(),
                        snapshot.getNoResultProbability(), snapshot.getSearchRange());
    }
}
