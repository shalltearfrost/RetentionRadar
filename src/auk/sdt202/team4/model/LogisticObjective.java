package auk.sdt202.team4.model;

import auk.sdt202.team4.optimization.ObjectiveFunction;

public final class LogisticObjective implements ObjectiveFunction {
    private final double[][] features;
    private final int[] labels;
    private final double regularization;
    private final int dimension;
    private final double smoothnessUpperBound;

    public LogisticObjective(double[][] features, int[] labels, double regularization) {
        validateDataset(features, labels, regularization);
        dimension = features[0].length;
        this.features = new double[features.length][];
        this.labels = labels.clone();
        this.regularization = regularization;

        double squaredFeatureSum = 0.0;
        for (int rowIndex = 0; rowIndex < features.length; rowIndex++) {
            requireFeatureRow(features[rowIndex]);
            requireBinaryLabel(labels[rowIndex]);
            this.features[rowIndex] = features[rowIndex].clone();

            for (double feature : features[rowIndex]) {
                double squaredFeature = feature * feature;
                double roundedSquaredFeature = Math.nextUp(squaredFeature);
                double accumulatedSquares = squaredFeatureSum + roundedSquaredFeature;
                squaredFeatureSum = Math.nextUp(accumulatedSquares);
            }
        }
        smoothnessUpperBound = calculateSmoothnessBound(squaredFeatureSum, features.length, regularization);
    }

    private static void validateDataset(double[][] features, int[] labels, double regularization) {
        if (features == null || labels == null || features.length < 2
                || features.length != labels.length) {
            throw new IllegalArgumentException(
                    "Features and labels must contain the same number of rows, at least two.");
        }
        validateRegularization(regularization);
        if (features[0] == null || features[0].length < 2) {
            throw new IllegalArgumentException("Each row must contain a bias and at least one feature.");
        }
    }

    private static void validateRegularization(double regularization) {
        if (!Double.isFinite(regularization) || regularization < 0.0) {
            throw new IllegalArgumentException("Regularization must be finite and nonnegative.");
        }
    }

    private static void requireBinaryLabel(int label) {
        if (label != 0 && label != 1) {
            throw new IllegalArgumentException("Labels must be either zero or one.");
        }
    }

    private static double calculateSmoothnessBound(
            double squaredFeatureSum, int rowCount, double regularization) {
        double averageCurvature = squaredFeatureSum / (4.0 * rowCount);
        double roundedAverageCurvature = Math.nextUp(averageCurvature);
        double bound = Math.nextUp(roundedAverageCurvature + regularization);
        if (!Double.isFinite(bound)) {
            throw new IllegalArgumentException("Feature magnitudes are too large for a finite smoothness bound.");
        }
        return bound;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public double value(double[] weights) {
        requireWeights(weights);
        double loss = 0.0;
        for (int rowIndex = 0; rowIndex < features.length; rowIndex++) {
            double logit = dot(weights, features[rowIndex]);
            double rowLoss = crossEntropy(logit, labels[rowIndex]);
            loss += rowLoss / features.length;
        }
        if (regularization > 0.0) {
            for (int featureIndex = 1; featureIndex < dimension; featureIndex++) {
                loss += (regularization / 2.0) * weights[featureIndex] * weights[featureIndex];
            }
        }
        return loss;
    }

    @Override
    public double[] gradient(double[] weights) {
        requireWeights(weights);
        double[] gradient = new double[dimension];
        for (int rowIndex = 0; rowIndex < features.length; rowIndex++) {
            double logit = dot(weights, features[rowIndex]);
            double predictedProbability = sigmoid(logit);
            double averagePredictionError = (predictedProbability - labels[rowIndex]) / features.length;
            for (int featureIndex = 0; featureIndex < dimension; featureIndex++) {
                gradient[featureIndex] += averagePredictionError * features[rowIndex][featureIndex];
            }
        }
        for (int featureIndex = 1; featureIndex < dimension; featureIndex++) {
            gradient[featureIndex] += regularization * weights[featureIndex];
        }
        return gradient;
    }

    public double probability(double[] weights, double[] featureRow) {
        requireWeights(weights);
        requireFeatureRow(featureRow);
        double logit = dot(weights, featureRow);
        return sigmoid(logit);
    }

    public double smoothnessUpperBound() {
        return smoothnessUpperBound;
    }

    public static double sigmoid(double logit) {
        if (logit >= 0.0) {
            double exponential = Math.exp(-logit);
            return 1.0 / (1.0 + exponential);
        }
        double exponential = Math.exp(logit);
        return exponential / (1.0 + exponential);
    }

    private static double crossEntropy(double logit, int label) {
        double loss;
        if (logit >= 0.0) {
            double exponential = Math.exp(-logit);
            loss = Math.log1p(exponential);
            if (label == 0) {
                loss += logit;
            }
        } else {
            double exponential = Math.exp(logit);
            loss = Math.log1p(exponential);
            if (label == 1) {
                loss -= logit;
            }
        }
        return loss;
    }

    private void requireWeights(double[] weights) {
        if (weights == null || weights.length != dimension) {
            throw new IllegalArgumentException("Weight dimension must match the feature dimension.");
        }
        for (double weight : weights) {
            if (!Double.isFinite(weight)) {
                throw new IllegalArgumentException("Weights must be finite.");
            }
        }
    }

    private void requireFeatureRow(double[] featureRow) {
        if (featureRow == null || featureRow.length != dimension || featureRow[0] != 1.0) {
            throw new IllegalArgumentException(
                    "Every feature row must match the dimension and begin with a bias of one.");
        }
        for (double feature : featureRow) {
            if (!Double.isFinite(feature)) {
                throw new IllegalArgumentException("Features must be finite.");
            }
        }
    }

    private static double dot(double[] left, double[] right) {
        double result = 0.0;
        for (int featureIndex = 0; featureIndex < left.length; featureIndex++) {
            result += left[featureIndex] * right[featureIndex];
        }
        return result;
    }
}
