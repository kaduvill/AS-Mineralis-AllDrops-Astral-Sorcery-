package com.kaduvill.asmineralisall.mineralis;

/**
 * Probability calculations for a bounded, retrying weighted selection followed by
 * an independent bonus replacement. This class has no game dependencies.
 */
public final strictfp class MineralisProbabilities {
    private static final int FLOAT_DRAW_COUNT = 1 << 24;
    private static final double SUM_TOLERANCE = 1.0e-12;

    private MineralisProbabilities() {
    }

    /**
     * Calculates unconditional probabilities per conversion attempt. A false
     * validity flag means that selecting that bin consumes a retry. Probability
     * not assigned to a bin also consumes a retry. The bonus replaces the result
     * after these retries, including an exhausted selection.
     *
     * @param drawProbabilities probability of each bin on one weighted draw
     * @param valid whether each bin resolves to a usable result
     * @param starmetalBonus independent probability of the bonus replacement
     * @param maxAttempts maximum number of weighted draws
     */
    public static Result calculate(double[] drawProbabilities, boolean[] valid,
                                   double starmetalBonus, int maxAttempts) {
        if (drawProbabilities == null || valid == null
                || drawProbabilities.length != valid.length) {
            throw new IllegalArgumentException("Probabilities and validity must have matching lengths");
        }
        requireProbability(starmetalBonus, "Bonus probability");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("At least one selection attempt is required");
        }

        double total = 0.0;
        double compensation = 0.0;
        for (double probability : drawProbabilities) {
            requireProbability(probability, "Draw probability");
            double adjusted = probability - compensation;
            double next = total + adjusted;
            compensation = (next - total) - adjusted;
            total = next;
        }
        if (total > 1.0 + SUM_TOLERANCE) {
            throw new IllegalArgumentException("Draw probabilities exceed one");
        }

        // Accommodate only floating-point accumulation error, never an
        // over-subscribed table. Keep the returned distribution normalized.
        double normalization = total > 1.0 ? 1.0 / total : 1.0;
        double accepted = 0.0;
        compensation = 0.0;
        for (int i = 0; i < drawProbabilities.length; i++) {
            if (valid[i]) {
                double adjusted = drawProbabilities[i] * normalization - compensation;
                double next = accepted + adjusted;
                compensation = (next - accepted) - adjusted;
                accepted = next;
            }
        }
        accepted = Math.min(1.0, accepted);

        double[] probabilities = new double[drawProbabilities.length];
        double ordinary = 1.0 - starmetalBonus;
        if (accepted == 0.0) {
            return new Result(probabilities, starmetalBonus, ordinary);
        }

        double exhausted;
        double succeeded;
        if (accepted == 1.0) {
            exhausted = 0.0;
            succeeded = 1.0;
        } else {
            double logExhausted = maxAttempts * Math.log1p(-accepted);
            exhausted = Math.exp(logExhausted);
            succeeded = -Math.expm1(logExhausted);
        }
        double factor = ordinary * succeeded / accepted;
        for (int i = 0; i < probabilities.length; i++) {
            if (valid[i]) {
                probabilities[i] = drawProbabilities[i] * normalization * factor;
            }
        }
        return new Result(probabilities, starmetalBonus, ordinary * exhausted);
    }

    /**
     * Returns exact one-draw probabilities over Random.nextFloat's 2^24 possible
     * values. The selector uses double multiplication, successive integer-weight
     * subtraction, and an inclusive zero endpoint. Signed and zero weights are
     * supported; an earlier reachable boundary takes precedence over later ones.
     * Remaining mass, if any, has no selected bin.
     */
    public static double[] selectionProbabilities(int[] weights, double totalWeight) {
        int[] ends = bucketEnds(weights, totalWeight);
        double[] probabilities = new double[ends.length];
        int previous = 0;
        for (int i = 0; i < ends.length; i++) {
            probabilities[i] = (ends[i] - previous) / (double) FLOAT_DRAW_COUNT;
            previous = ends[i];
        }
        return probabilities;
    }

    private static int[] bucketEnds(int[] weights, double totalWeight) {
        if (weights == null) {
            throw new IllegalArgumentException("Weights are required");
        }
        if (!Double.isFinite(totalWeight)) {
            throw new IllegalArgumentException("Total weight must be finite");
        }
        int[] ends = new int[weights.length];
        int previous = 0;
        for (int i = 0; i < weights.length; i++) {
            boolean descending = totalWeight < 0.0;
            int low = descending ? 0 : previous;
            int high = descending ? FLOAT_DRAW_COUNT - previous : FLOAT_DRAW_COUNT;
            // The residual is monotone in the initial draw. Evaluate the actual
            // arithmetic at each boundary: summing the weights first could
            // change a boundary through floating-point rounding.
            while (low < high) {
                int middle = low + (high - low) / 2;
                double residual = (middle / (double) FLOAT_DRAW_COUNT) * totalWeight;
                for (int j = 0; j <= i; j++) {
                    residual -= weights[j];
                }
                if ((residual <= 0.0) == descending) {
                    high = middle;
                } else {
                    low = middle + 1;
                }
            }
            previous = descending ? FLOAT_DRAW_COUNT - low : low;
            ends[i] = previous;
        }
        return ends;
    }

    private static void requireProbability(double probability, String label) {
        if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException(label + " must be finite and between zero and one");
        }
    }

    public static final class Result {
        private final double[] probabilities;
        private final double bonusProbability;
        private final double noResultProbability;

        private Result(double[] probabilities, double bonusProbability, double noResultProbability) {
            this.probabilities = probabilities;
            this.bonusProbability = bonusProbability;
            this.noResultProbability = noResultProbability;
        }

        public double[] getProbabilities() {
            return probabilities.clone();
        }

        public double getBonusProbability() {
            return bonusProbability;
        }

        public double getNoResultProbability() {
            return noResultProbability;
        }
    }
}
