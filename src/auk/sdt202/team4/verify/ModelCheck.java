package auk.sdt202.team4.verify;

import auk.sdt202.team4.data.SyntheticCohort;
import auk.sdt202.team4.data.SyntheticCohort.Cohort;
import auk.sdt202.team4.model.LogisticObjective;
import auk.sdt202.team4.optimization.GradientDescent;
import auk.sdt202.team4.optimization.OptimizationResult;
import auk.sdt202.team4.optimization.OptimizerConfig;
import auk.sdt202.team4.optimization.StopReason;
import java.util.Arrays;

public final class ModelCheck {
    private ModelCheck() {
    }

    public static void runAll() {
        Cohort cohort = SyntheticCohort.generate(600, 202604L);
        Cohort repeatedCohort = SyntheticCohort.generate(600, 202604L);
        checkReproducibleGeneration(cohort, repeatedCohort);
        double[][] features = cohort.features();
        int[] labels = cohort.labels();
        LogisticObjective objective = new LogisticObjective(features, labels, 0.01);
        double[] testWeights = {0.35, -0.6, 0.25, 0.8};

        checkGradientCoordinates(objective, testWeights);
        checkExtremeLogits(objective);
        OptimizationResult result = trainSyntheticModel(objective);
        checkBaseRateIdentity(objective, result, features, labels);
        checkObjectiveCopy(objective, testWeights, features, labels);
        checkCohortCopy(cohort);
        System.out.println("Model checks passed.");
    }

    private static void checkReproducibleGeneration(Cohort cohort, Cohort repeatedCohort) {
        require(Arrays.deepEquals(cohort.features(), repeatedCohort.features())
                && Arrays.equals(cohort.labels(), repeatedCohort.labels()), "seeded synthetic cohort");
    }

    private static void checkGradientCoordinates(LogisticObjective objective, double[] testWeights) {
        double[] analyticGradient = objective.gradient(testWeights);
        for (int featureIndex = 0; featureIndex < testWeights.length; featureIndex++) {
            double machinePrecisionStep = Math.cbrt(Math.ulp(1.0));
            double coordinateScale = Math.max(1.0, Math.abs(testWeights[featureIndex]));
            double step = machinePrecisionStep * coordinateScale;
            double[] plusWeights = testWeights.clone();
            double[] minusWeights = testWeights.clone();
            plusWeights[featureIndex] += step;
            minusWeights[featureIndex] -= step;

            double numericalGradient =
                    (objective.value(plusWeights) - objective.value(minusWeights)) / (2.0 * step);
            double largestGradientMagnitude = Math.max(
                    Math.abs(numericalGradient), Math.abs(analyticGradient[featureIndex]));
            double comparisonScale = Math.max(1.0, largestGradientMagnitude);
            double relativeError =
                    Math.abs(numericalGradient - analyticGradient[featureIndex]) / comparisonScale;
            require(relativeError < 1e-6, "logistic gradient coordinate " + featureIndex);
        }
    }

    private static void checkExtremeLogits(LogisticObjective objective) {
        require(Double.isFinite(objective.value(new double[] {800.0, 0.0, 0.0, 0.0}))
                && Double.isFinite(objective.value(new double[] {-800.0, 0.0, 0.0, 0.0})),
                "stable loss at extreme logits");
        require(LogisticObjective.sigmoid(800.0) == 1.0
                && LogisticObjective.sigmoid(-800.0) == 0.0, "stable sigmoid at extreme logits");
    }

    private static OptimizationResult trainSyntheticModel(LogisticObjective objective) {
        double initialLoss = objective.value(new double[objective.dimension()]);
        double learningRate = 1.0 / objective.smoothnessUpperBound();
        OptimizerConfig configuration = new OptimizerConfig(learningRate, 1e-8, 20000, 0);
        OptimizationResult result = GradientDescent.minimize(objective,
                new double[objective.dimension()], configuration);
        require(result.stopReason() == StopReason.CONVERGED && result.value() < initialLoss,
                "synthetic logistic training converges");
        return result;
    }

    private static void checkBaseRateIdentity(LogisticObjective objective, OptimizationResult result,
            double[][] features, int[] labels) {
        double predictedProbabilitySum = 0.0;
        double observedPositiveSum = 0.0;
        for (int rowIndex = 0; rowIndex < labels.length; rowIndex++) {
            predictedProbabilitySum += objective.probability(result.point(), features[rowIndex]);
            observedPositiveSum += labels[rowIndex];
        }
        double predictedMean = predictedProbabilitySum / labels.length;
        double observedMean = observedPositiveSum / labels.length;
        require(Math.abs(predictedMean - observedMean) < 1e-7,
                "unregularized bias base-rate identity");
    }

    private static void checkObjectiveCopy(LogisticObjective objective, double[] testWeights,
            double[][] features, int[] labels) {
        double savedLoss = objective.value(testWeights);
        features[0][1] += 100.0;
        labels[0] = 1 - labels[0];
        require(objective.value(testWeights) == savedLoss, "objective owns a copy of its input data");
    }

    private static void checkCohortCopy(Cohort cohort) {
        double originalFeature = cohort.features()[0][1];
        double[][] featureCopy = cohort.features();
        featureCopy[0][1] += 100.0;
        require(cohort.features()[0][1] == originalFeature, "cohort accessors protect stored data");
    }

    private static void require(boolean passed, String name) {
        if (!passed) {
            throw new AssertionError("FAIL: " + name);
        }
        System.out.println("PASS: " + name);
    }
}
