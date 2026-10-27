package com.kaduvill.asjeialldrops.chalice;

import hellfirepvp.astralsorcery.common.base.LiquidInteraction;
import hellfirepvp.astralsorcery.common.base.LiquidInteraction.FluidInteractionAction;
import hellfirepvp.astralsorcery.common.base.LiquidInteraction.InteractionFormBlock;
import hellfirepvp.astralsorcery.common.tile.TileChalice.ConfigEntryChalice;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Inspects the installed AS registry without constructing reactions, draining tanks, or using RNG. */
public final class AstralChaliceReader {
    private final Field registered = field(LiquidInteraction.class, "registeredInteractions");
    private final Field weight = field(LiquidInteraction.class, "probability");
    private final Field action = field(LiquidInteraction.class, "action");
    private final Field chanceFirst = field(InteractionFormBlock.class, "c1");
    private final Field chanceSecond = field(InteractionFormBlock.class, "c2");
    private final Map<Class<?>, ActionKind> actionKinds = new HashMap<>();

    public ChaliceRecipes.Snapshot read() throws IllegalAccessException {
        Object[] ordinary = ((List<?>) registered.get(null)).toArray();
        Object[] scripted = LiquidInteraction.mtInteractions.toArray();
        List<ChaliceRecipes.Reaction> reactions = new ArrayList<>(ordinary.length + scripted.length);
        IdentityHashMap<LiquidInteraction, ChaliceRecipes.Reaction> descriptions = new IdentityHashMap<>();
        append(ordinary, reactions, descriptions);
        append(scripted, reactions, descriptions);
        return new ChaliceRecipes.Snapshot(reactions, ConfigEntryChalice.chaliceRange);
    }

    private void append(Object[] entries, List<ChaliceRecipes.Reaction> output,
                        IdentityHashMap<LiquidInteraction, ChaliceRecipes.Reaction> descriptions)
            throws IllegalAccessException {
        for (Object entry : entries) {
            LiquidInteraction interaction = (LiquidInteraction) entry;
            if (interaction == null) throw new IllegalStateException("Null Chalice interaction in AS registry");
            ChaliceRecipes.Reaction description = descriptions.get(interaction);
            if (description == null) {
                FluidStack first = interaction.getComponent1();
                FluidStack second = interaction.getComponent2();
                if (first == null || second == null || first.getFluid() == null || second.getFluid() == null) {
                    throw new IllegalStateException("Chalice interaction has an unavailable fluid");
                }
                FluidInteractionAction handler = (FluidInteractionAction) action.get(interaction);
                if (handler == null) throw new IllegalStateException("Chalice interaction has no action");
                ActionKind kind = actionKinds.computeIfAbsent(handler.getClass(), ActionKind::new);
                ItemStack item;
                try {
                    // This is AS's output-inspection API, not the action that creates/drops an item.
                    item = handler.getOutputForMatching();
                } catch (RuntimeException | LinkageError unavailable) {
                    item = ItemStack.EMPTY;
                }
                if (item == null) item = ItemStack.EMPTY;
                double firstChance = kind.standardDrainFirst
                        ? ChaliceRecipes.consumptionProbability(chanceFirst.getFloat(handler)) : Double.NaN;
                double secondChance = kind.standardDrainSecond
                        ? ChaliceRecipes.consumptionProbability(chanceSecond.getFloat(handler)) : Double.NaN;
                description = new ChaliceRecipes.Reaction(first, second, item, weight.getInt(interaction),
                        firstChance, secondChance, kind.custom, kind.crystal);
                descriptions.put(interaction, description);
            }
            // Both AS lists participate, in order. Repeated identities are repeated weighted tickets.
            // Collapse only their visual descriptions, never their selection weight.
            output.add(description);
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field result = owner.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported AS Chalice registry: " + name, failure);
        }
    }

    private static final class ActionKind {
        final boolean standardDrainFirst;
        final boolean standardDrainSecond;
        final boolean crystal;
        final boolean custom;

        ActionKind(Class<?> type) {
            try {
                standardDrainFirst = type.getMethod("drainComponent1", FluidStack.class, IFluidHandler.class)
                        .getDeclaringClass() == InteractionFormBlock.class;
                standardDrainSecond = type.getMethod("drainComponent2", FluidStack.class, IFluidHandler.class)
                        .getDeclaringClass() == InteractionFormBlock.class;
                Class<?> producer = type.getMethod("doInteraction", net.minecraft.world.World.class,
                        hellfirepvp.astralsorcery.common.util.data.Vector3.class).getDeclaringClass();
                crystal = producer.getName().equals(LiquidInteraction.class.getName() + "$1");
                custom = producer != InteractionFormBlock.class && !crystal;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Unsupported Chalice action", failure);
            }
        }
    }
}
