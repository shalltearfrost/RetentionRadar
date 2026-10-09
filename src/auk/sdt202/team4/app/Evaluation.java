package auk.sdt202.team4.app;

import auk.sdt202.team4.model.LogisticObjective;
import java.util.Arrays;
import java.util.Comparator;

public final class Evaluation {
    private Evaluation() {
    }

    public static Metrics evaluate(LogisticObjective model, double[] weights,
            double[][] features, int[] labels, double threshold) {
        validateInputs(model, weights, features, labels, threshold);

        MetricTotals totals = new MetricTotals();
        RankedScore[] rankedScores = new RankedScore[labels.length];
        for (int row = 0; row < labels.length; row++) {
            double[] featureRow = features[row];
            int label = labels[row];
            validateFeatureRow(featureRow, model.dimension(), label);
            double logit = calculateLogit(weights, featureRow);
            double probability = LogisticObjective.sigmoid(logit);

            recordPrediction(totals, label, probability, threshold);
            double rowLoss = logisticLoss(logit, label);
            totals.logLoss += rowLoss / labels.length;
            double predictionError = probability - label;
            totals.brierScore += predictionError * predictionError / labels.length;
            rankedScores[row] = new RankedScore(logit, label);
        }
        return summarize(totals, rankedScores, labels.length, threshold);
    }

    private static void validateInputs(LogisticObjective model, double[] weights,
            double[][] features, int[] labels, double threshold) {
        if (model == null || weights == null || features == null || labels == null) {
            throw new IllegalArgumentException("Model, weights, features, and labels are required.");
        }
        if (features.length == 0 || features.length != labels.length) {
            throw new IllegalArgumentException("Evaluation needs matching nonempty features and labels.");
        }
        if (weights.length != model.dimension()) {
            throw new IllegalArgumentException("Weights must match the model dimension.");
        }
        if (!Double.isFinite(threshold) || threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be finite and between zero and one.");
        }
        for (double weight : weights) {
            if (!Double.isFinite(weight)) {
                throw new IllegalArgumentException("Weights must be finite.");
            }
        }
    }

    private static void validateFeatureRow(double[] featureRow, int dimension, int label) {
        if (featureRow == null || featureRow.length != dimension || featureRow[0] != 1.0) {
            throw new IllegalArgumentException("Feature rows must match the model and start with a bias of one.");
        }
        if (label != 0 && label != 1) {
            throw new IllegalArgumentException("Labels must be zero or one.");
        }
    }

    private static double calculateLogit(double[] weights, double[] featureRow) {
        double logit = 0.0;
        for (int column = 0; column < weights.length; column++) {
            if (!Double.isFinite(featureRow[column])) {
                throw new IllegalArgumentException("Features must be finite.");
            }
            logit += weights[column] * featureRow[column];
        }
        if (!Double.isFinite(logit)) {
            throw new IllegalArgumentException("Evaluation logits must be finite.");
        }
        return logit;
    }

    private static void recordPrediction(MetricTotals totals, int label,
            double probability, double threshold) {
        boolean predictedPositive = probability >= threshold;
        if (label == 1) {
            if (predictedPositive) {
                totals.truePositive++;
            } else {
                totals.falseNegative++;
            }
        } else if (predictedPositive) {
            totals.falsePositive++;
        } else {
            totals.trueNegative++;
        }
    }

    private static double logisticLoss(double logit, int label) {
        double loss;
        if (logit >= 0.0) {
            loss = Math.log1p(Math.exp(-logit));
            if (label == 0) {
                loss += logit;
            }
        } else {
            loss = Math.log1p(Math.exp(logit));
            if (label == 1) {
                loss -= logit;
            }
        }
        return loss;
    }

    private static Metrics summarize(MetricTotals totals, RankedScore[] rankedScores,
            int count, double threshold) {
        double accuracy = (totals.truePositive + totals.trueNegative) / (double) count;
        double precision = divideOrNaN(totals.truePositive,
                totals.truePositive + totals.falsePositive);
        double recall = divideOrNaN(totals.truePositive,
                totals.truePositive + totals.falseNegative);
        double f1 = divideOrNaN(2.0 * totals.truePositive,
                2.0 * totals.truePositive + totals.falsePositive + totals.falseNegative);
        double auc = calculateAuc(rankedScores, totals.truePositive + totals.falseNegative,
                totals.trueNegative + totals.falsePositive);
        return new Metrics(count, totals.truePositive, totals.falsePositive,
                totals.trueNegative, totals.falseNegative, accuracy, precision, recall,
                f1, auc, totals.logLoss, totals.brierScore, threshold);
    }

    private static double calculateAuc(RankedScore[] rankedScores,
            int positiveCount, int negativeCount) {
        if (positiveCount == 0 || negativeCount == 0) {
            return Double.NaN;
        }

        Arrays.sort(rankedScores, Comparator.comparingDouble(RankedScore::logit));
        int negativesSeen = 0;
        double correctlyOrderedPairs = 0.0;
        int groupStart = 0;
        while (groupStart < rankedScores.length) {
            int groupEnd = groupStart + 1;
            while (groupEnd < rankedScores.length
                    && rankedScores[groupEnd].logit() == rankedScores[groupStart].logit()) {
                groupEnd++;
            }

            int groupPositives = 0;
            for (int row = groupStart; row < groupEnd; row++) {
                groupPositives += rankedScores[row].label();
            }
            int groupNegatives = groupEnd - groupStart - groupPositives;
            correctlyOrderedPairs += groupPositives * (negativesSeen + 0.5 * groupNegatives);
            negativesSeen += groupNegatives;
            groupStart = groupEnd;
        }
        return correctlyOrderedPairs / ((double) positiveCount * negativeCount);
    }

    private static double divideOrNaN(double numerator, double denominator) {
        if (denominator == 0.0) {
            return Double.NaN;
        }
        return numerator / denominator;
    }

    private static final class MetricTotals {
        private int truePositive;
        private int falsePositive;
        private int trueNegative;
        private int falseNegative;
        private double logLoss;
        private double brierScore;
    }

    private record RankedScore(double logit, int label) {
    }

    public record Metrics(int count, int truePositive, int falsePositive, int trueNegative,
            int falseNegative, double accuracy, double precision, double recall, double f1,
            double auc, double logLoss, double brierScore, double threshold) {
    }
}
