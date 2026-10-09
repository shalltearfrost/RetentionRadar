package auk.sdt202.team4.verify;

import auk.sdt202.team4.app.Evaluation;
import auk.sdt202.team4.model.LogisticObjective;
import java.util.List;

public final class EvaluationCheck {
    private EvaluationCheck() {
    }

    public static void main(String[] arguments) {
        runAll();
    }

    public static void runAll() {
        List<NamedCheck> checks = List.of(
                new NamedCheck("Threshold confusion matrix and summary metrics",
                        EvaluationCheck::confusionMatrix),
                new NamedCheck("Unpenalized log-loss and Brier score", EvaluationCheck::properScores),
                new NamedCheck("Perfect AUC ranking", EvaluationCheck::perfectRanking),
                new NamedCheck("Reversed AUC ranking", EvaluationCheck::reversedRanking),
                new NamedCheck("AUC gives equal scores half credit", EvaluationCheck::allTies),
                new NamedCheck("Mixed AUC ties match pairwise counting", EvaluationCheck::mixedTies),
                new NamedCheck("Prediction includes the threshold boundary", EvaluationCheck::thresholdBoundary),
                new NamedCheck("No predicted positives has undefined precision",
                        EvaluationCheck::noPredictedPositives),
                new NamedCheck("Single-class evaluation has undefined AUC", EvaluationCheck::singleClass),
                new NamedCheck("Extreme logits retain finite unpenalized loss and ordering",
                        EvaluationCheck::extremeLogits),
                new NamedCheck("Evaluation leaves input arrays unchanged", EvaluationCheck::inputIsolation),
                new NamedCheck("Invalid evaluation inputs are rejected", EvaluationCheck::inputValidation));
        System.out.println("Evaluation self-check");
        for (NamedCheck check : checks) {
            check.action().run();
            System.out.println("PASS: " + check.name());
        }
        System.out.println("All " + checks.size() + " evaluation checks passed.");
    }

    private static void confusionMatrix() {
        Evaluation.Metrics metrics = probabilityFixture(new double[] {0.2, 0.4, 0.6, 0.8},
                new int[] {0, 1, 0, 1}, 0.5);
        require(metrics.count() == 4 && metrics.truePositive() == 1 && metrics.falsePositive() == 1
                && metrics.trueNegative() == 1 && metrics.falseNegative() == 1, "Confusion counts");
        close(metrics.accuracy(), 0.5, "Accuracy");
        close(metrics.precision(), 0.5, "Precision");
        close(metrics.recall(), 0.5, "Recall");
        close(metrics.f1(), 0.5, "F1");
        close(metrics.auc(), 0.75, "AUC");
        close(metrics.threshold(), 0.5, "Threshold");
    }

    private static void properScores() {
        double[][] features = {{1.0, 1.0}, {1.0, 1.0}};
        int[] labels = {0, 1};
        LogisticObjective model = new LogisticObjective(features, labels, 7.0);
        double[] weights = {0.0, Math.log(4.0)};
        Evaluation.Metrics metrics = Evaluation.evaluate(model, weights, features, labels, 0.5);
        close(metrics.logLoss(), -0.5 * (Math.log(0.2) + Math.log(0.8)), "Unpenalized log-loss");
        close(metrics.brierScore(), 0.34, "Brier score");
        require(metrics.logLoss() < model.value(weights), "Evaluation excludes regularization");
    }

    private static void perfectRanking() {
        Evaluation.Metrics metrics = probabilityFixture(new double[] {0.1, 0.2, 0.8, 0.9},
                new int[] {0, 0, 1, 1}, 0.5);
        close(metrics.auc(), 1.0, "Perfect AUC");
        close(metrics.accuracy(), 1.0, "Perfect accuracy");
    }

    private static void reversedRanking() {
        Evaluation.Metrics metrics = probabilityFixture(new double[] {0.1, 0.2, 0.8, 0.9},
                new int[] {1, 1, 0, 0}, 0.5);
        close(metrics.auc(), 0.0, "Reversed AUC");
    }

    private static void allTies() {
        Evaluation.Metrics metrics = logitFixture(new double[] {0.0, 0.0, -0.0, 0.0},
                new int[] {1, 0, 1, 0}, 0.5);
        close(metrics.auc(), 0.5, "Tied AUC");
    }

    private static void mixedTies() {
        Evaluation.Metrics metrics = logitFixture(new double[] {-1.0, 0.0, 0.0, 1.0},
                new int[] {0, 0, 1, 1}, 0.5);
        close(metrics.auc(), 0.875, "Three wins and one tie among four pairs");
    }

    private static void thresholdBoundary() {
        Evaluation.Metrics metrics = logitFixture(new double[] {0.0, 0.0}, new int[] {0, 1}, 0.5);
        require(metrics.truePositive() == 1 && metrics.falsePositive() == 1
                && metrics.trueNegative() == 0 && metrics.falseNegative() == 0, "Inclusive threshold");
        Evaluation.Metrics allPositivePredictions = logitFixture(
                new double[] {-1.0, 1.0}, new int[] {0, 1}, 0.0);
        require(allPositivePredictions.truePositive() == 1
                && allPositivePredictions.falsePositive() == 1, "Zero threshold");
        Evaluation.Metrics allNegativePredictions = logitFixture(
                new double[] {-1.0, 1.0}, new int[] {0, 1}, 1.0);
        require(allNegativePredictions.trueNegative() == 1
                && allNegativePredictions.falseNegative() == 1, "Unit threshold");
    }

    private static void noPredictedPositives() {
        Evaluation.Metrics metrics = probabilityFixture(new double[] {0.1, 0.2}, new int[] {0, 1}, 0.5);
        require(Double.isNaN(metrics.precision()), "Undefined precision");
        close(metrics.recall(), 0.0, "Zero recall");
        close(metrics.f1(), 0.0, "Zero F1 with a false negative");
    }

    private static void singleClass() {
        Evaluation.Metrics negativeClassMetrics = probabilityFixture(
                new double[] {0.1, 0.2}, new int[] {0, 0}, 0.5);
        require(Double.isNaN(negativeClassMetrics.auc())
                && Double.isNaN(negativeClassMetrics.recall())
                && Double.isNaN(negativeClassMetrics.precision())
                && Double.isNaN(negativeClassMetrics.f1()),
                "Undefined single-negative-class metrics");
        Evaluation.Metrics positiveClassMetrics = probabilityFixture(
                new double[] {0.8, 0.9}, new int[] {1, 1}, 0.5);
        require(Double.isNaN(positiveClassMetrics.auc()), "Single-positive-class AUC");
        close(positiveClassMetrics.recall(), 1.0, "Single-positive-class recall");
    }

    private static void extremeLogits() {
        Evaluation.Metrics wrongPredictions = logitFixture(
                new double[] {800.0, -800.0}, new int[] {0, 1}, 0.5);
        close(wrongPredictions.logLoss(), 800.0, "Extreme wrong-prediction log-loss");
        close(wrongPredictions.brierScore(), 1.0, "Extreme wrong-prediction Brier score");
        Evaluation.Metrics saturatedProbabilities = logitFixture(
                new double[] {800.0, 801.0}, new int[] {0, 1}, 0.5);
        close(saturatedProbabilities.auc(), 1.0,
                "Distinct logits remain distinct despite rounded probabilities");
    }

    private static void inputIsolation() {
        double[][] features = {{1.0, 1.0}, {1.0, -1.0}};
        int[] labels = {1, 0};
        double[] weights = {0.0, 2.0};
        LogisticObjective model = new LogisticObjective(features, labels, 0.0);
        Evaluation.evaluate(model, weights, features, labels, 0.5);
        require(features[0][1] == 1.0 && features[1][1] == -1.0 && labels[0] == 1
                && labels[1] == 0 && weights[0] == 0.0 && weights[1] == 2.0, "Input preservation");
    }

    private static void inputValidation() {
        double[][] features = {{1.0, 0.0}, {1.0, 1.0}};
        int[] labels = {0, 1};
        LogisticObjective model = new LogisticObjective(features, labels, 0.0);
        double[] weights = {0.0, 1.0};
        rejectInvalidThresholds(model, weights, features, labels);
        rejectMissingInputs(model, weights, features, labels);
        rejectInvalidWeightsAndLabels(model, weights, features, labels);
        rejectInvalidFeatureRows(model, weights);
    }

    private static void rejectInvalidThresholds(LogisticObjective model, double[] weights,
            double[][] features, int[] labels) {
        for (double threshold : new double[] {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            expectInvalid(() -> Evaluation.evaluate(model, weights, features, labels, threshold));
        }
    }

    private static void rejectMissingInputs(LogisticObjective model, double[] weights,
            double[][] features, int[] labels) {
        expectInvalid(() -> Evaluation.evaluate(null, weights, features, labels, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, null, features, labels, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, null, labels, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, features, null, 0.5));
    }

    private static void rejectInvalidWeightsAndLabels(LogisticObjective model, double[] weights,
            double[][] features, int[] labels) {
        expectInvalid(() -> Evaluation.evaluate(model, new double[] {1.0}, features, labels, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model,
                new double[] {Double.NaN, 1.0}, features, labels, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, features, new int[] {0}, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, features, new int[] {0, 2}, 0.5));
    }

    private static void rejectInvalidFeatureRows(LogisticObjective model, double[] weights) {
        expectInvalid(() -> Evaluation.evaluate(model, weights, new double[0][], new int[0], 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, new double[][] {null}, new int[] {0}, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, new double[][] {{1.0}}, new int[] {0}, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights, new double[][] {{0.0, 1.0}}, new int[] {0}, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model, weights,
                new double[][] {{1.0, Double.POSITIVE_INFINITY}}, new int[] {0}, 0.5));
        expectInvalid(() -> Evaluation.evaluate(model,
                new double[] {Double.MAX_VALUE, Double.MAX_VALUE},
                new double[][] {{1.0, 1.0}}, new int[] {0}, 0.5));
    }

    private static Evaluation.Metrics probabilityFixture(
            double[] probabilities, int[] labels, double threshold) {
        double[] logits = new double[probabilities.length];
        for (int row = 0; row < probabilities.length; row++) {
            logits[row] = Math.log(probabilities[row] / (1.0 - probabilities[row]));
        }
        return logitFixture(logits, labels, threshold);
    }

    private static Evaluation.Metrics logitFixture(
            double[] logits, int[] labels, double threshold) {
        double[][] features = new double[logits.length][2];
        for (int row = 0; row < logits.length; row++) {
            features[row][0] = 1.0;
            features[row][1] = logits[row];
        }
        LogisticObjective model = new LogisticObjective(features, labels, 0.0);
        return Evaluation.evaluate(model, new double[] {0.0, 1.0}, features, labels, threshold);
    }

    private static void close(double actual, double expected, String label) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) <= 1e-12,
                label + ": expected " + expected + ", received " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private record NamedCheck(String name, Runnable action) {
    }
}
