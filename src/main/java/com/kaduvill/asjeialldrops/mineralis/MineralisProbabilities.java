package com.kaduvill.asjeialldrops.mineralis;

import com.kaduvill.asjeialldrops.ore.OreProbabilities;

/** Mineralis entry point for the shared AS weighted-selection calculations. */
public final class MineralisProbabilities {
    private MineralisProbabilities() {}

    public static Result calculate(double[] draws, boolean[] valid, double bonus, int attempts) {
        return new Result(OreProbabilities.calculate(draws, valid, bonus, attempts));
    }

    public static double[] selectionProbabilities(int[] weights, double totalWeight) {
        return OreProbabilities.selectionProbabilities(weights, totalWeight);
    }

    public static final class Result {
        private final OreProbabilities.Result result;
        private Result(OreProbabilities.Result result) { this.result = result; }
        public double[] getProbabilities() { return result.getProbabilities(); }
        public double getBonusProbability() { return result.getBonusProbability(); }
        public double getNoResultProbability() { return result.getNoResultProbability(); }
    }
}
