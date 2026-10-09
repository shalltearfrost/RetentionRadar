package auk.sdt202.team4.data;

import java.util.Locale;
import java.util.Random;
import auk.sdt202.team4.model.LogisticObjective;

public final class SyntheticCohort {
    private SyntheticCohort() {
    }

    public static Cohort generate(int size, long seed) {
        if (size < 2) {
            throw new IllegalArgumentException("A synthetic cohort must contain at least two students.");
        }
        Random random = new Random(seed);
        double[] plantedWeights = {-0.5, -1.2, 0.8, -0.7};
        String[] featureNames = {
            "bias", "studyEngagement", "financialPressure", "supportAccess"
        };
        double[][] features = new double[size][4];
        int[] labels = new int[size];
        String[] studentIds = new String[size];
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            features[rowIndex][0] = 1.0;
            double logit = plantedWeights[0];
            for (int featureIndex = 1; featureIndex < plantedWeights.length; featureIndex++) {
                features[rowIndex][featureIndex] = random.nextDouble();
                logit += plantedWeights[featureIndex] * features[rowIndex][featureIndex];
            }
            double outcomeDraw = random.nextDouble();
            double positiveProbability = LogisticObjective.sigmoid(logit);
            if (outcomeDraw < positiveProbability) {
                labels[rowIndex] = 1;
            } else {
                labels[rowIndex] = 0;
            }
            studentIds[rowIndex] = String.format(Locale.ROOT, "SYN-%04d", rowIndex + 1);
        }
        return new Cohort(features, labels, studentIds, featureNames, plantedWeights);
    }

    public record Cohort(double[][] features, int[] labels, String[] studentIds,
            String[] featureNames, double[] plantedWeights) {
        public Cohort {
            validateDimensions(features, labels, studentIds, featureNames, plantedWeights);
            double[][] featureCopy = copyValidatedFeatures(features, labels, studentIds, featureNames.length);
            validateFeatureMetadata(featureNames, plantedWeights);
            features = featureCopy;
            labels = labels.clone();
            studentIds = studentIds.clone();
            featureNames = featureNames.clone();
            plantedWeights = plantedWeights.clone();
        }

        private static void validateDimensions(double[][] features, int[] labels, String[] studentIds,
                String[] featureNames, double[] plantedWeights) {
            if (features == null || labels == null || studentIds == null
                    || featureNames == null || plantedWeights == null
                    || features.length < 2 || labels.length != features.length
                    || studentIds.length != features.length || featureNames.length < 2
                    || plantedWeights.length != featureNames.length) {
                throw new IllegalArgumentException(
                        "Cohort arrays must have consistent dimensions and at least two students.");
            }
        }

        private static double[][] copyValidatedFeatures(double[][] features, int[] labels,
                String[] studentIds, int featureCount) {
            double[][] featureCopy = new double[features.length][];
            for (int rowIndex = 0; rowIndex < features.length; rowIndex++) {
                if (features[rowIndex] == null || features[rowIndex].length != featureCount
                        || features[rowIndex][0] != 1.0 || (labels[rowIndex] != 0 && labels[rowIndex] != 1)
                        || studentIds[rowIndex] == null || studentIds[rowIndex].isBlank()) {
                    throw new IllegalArgumentException(
                            "Each student must have a bias, valid features, a binary label and an identifier.");
                }
                for (double feature : features[rowIndex]) {
                    if (!Double.isFinite(feature)) {
                        throw new IllegalArgumentException("Cohort features must be finite.");
                    }
                }
                featureCopy[rowIndex] = features[rowIndex].clone();
            }
            return featureCopy;
        }

        private static void validateFeatureMetadata(String[] featureNames, double[] plantedWeights) {
            for (int featureIndex = 0; featureIndex < featureNames.length; featureIndex++) {
                if (featureNames[featureIndex] == null || featureNames[featureIndex].isBlank()
                        || !Double.isFinite(plantedWeights[featureIndex])) {
                    throw new IllegalArgumentException(
                            "Feature names must be nonempty and planted weights must be finite.");
                }
            }
        }

        @Override
        public double[][] features() {
            double[][] copy = new double[features.length][];
            for (int rowIndex = 0; rowIndex < features.length; rowIndex++) {
                copy[rowIndex] = features[rowIndex].clone();
            }
            return copy;
        }

        @Override
        public int[] labels() {
            return labels.clone();
        }

        @Override
        public String[] studentIds() {
            return studentIds.clone();
        }

        @Override
        public String[] featureNames() {
            return featureNames.clone();
        }

        @Override
        public double[] plantedWeights() {
            return plantedWeights.clone();
        }
    }
}
