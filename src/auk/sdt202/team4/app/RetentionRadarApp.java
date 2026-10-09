package auk.sdt202.team4.app;

import auk.sdt202.team4.data.DataSplit;
import auk.sdt202.team4.data.DataSplit.Split;
import auk.sdt202.team4.data.Standardizer;
import auk.sdt202.team4.data.StudentCsvLoader;
import auk.sdt202.team4.data.StudentCsvLoader.LoadedCohort;
import auk.sdt202.team4.data.SyntheticCohort;
import auk.sdt202.team4.data.SyntheticCohort.Cohort;
import auk.sdt202.team4.model.LogisticObjective;
import auk.sdt202.team4.optimization.GradientDescent;
import auk.sdt202.team4.optimization.NesterovGradientDescent;
import auk.sdt202.team4.optimization.OptimizationResult;
import auk.sdt202.team4.optimization.OptimizerConfig;
import auk.sdt202.team4.optimization.StopReason;
import auk.sdt202.team4.verify.AccelerationCheck;
import auk.sdt202.team4.verify.DataCheck;
import auk.sdt202.team4.verify.EvaluationCheck;
import auk.sdt202.team4.verify.ModelCheck;
import auk.sdt202.team4.verify.SelfCheck;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public final class RetentionRadarApp {
    public static final String COURSE =
            "SDT 202 - Design and Analysis of Data Structures and Algorithms";
    public static final String TEAM = "Team 4";
    public static final String MEMBERS = "Uliana Matviienko; Andrii Moroz";
    public static final String TOPIC = "Gradient descent algorithm";

    private static final Path DEFAULT_CSV = Path.of("data", "students-first-semester.csv");
    private static final long DATA_SEED = 202604L;
    private static final int SYNTHETIC_RECORD_COUNT = 600;
    private static final double TEST_FRACTION = 0.2;
    private static final double REGULARIZATION_STRENGTH = 0.01;
    private static final double GRADIENT_TOLERANCE = 1e-6;
    private static final int MAXIMUM_TRAINING_UPDATES = 50_000;
    private static final int TRAINING_HISTORY_STRIDE = 25;
    private static final double EVALUATION_THRESHOLD = 0.5;
    private static final int RANKING_LIMIT = 25;

    private record TrainingSession(
            String name,
            Cohort trainingCohort,
            Cohort testCohort,
            LogisticObjective objective,
            OptimizerConfig optimizerConfig) {
    }

    private RetentionRadarApp() {
    }

    public static void main(String[] arguments) {
        printHeader();

        try {
            runRequestedMode(arguments);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Could not complete requested mode: " + exception.getMessage());
            System.err.println(
                    "Run --synthetic or --verify without a data file, or select a valid CSV path.");
            System.exit(2);
        }
    }

    private static void runRequestedMode(String[] arguments) throws IOException {
        if (arguments.length == 0) {
            runInteractiveMenu();
            return;
        }

        boolean hasCustomCsvPath = arguments.length == 2 && arguments[0].equals("--real");
        if (hasCustomCsvPath) {
            TrainingSession session = createRealSession(Path.of(arguments[1]));
            compareCohortModels(session, new DecisionCosts(1.0, 1.0));
            return;
        }

        if (arguments.length != 1) {
            throw new IllegalArgumentException(usage());
        }

        switch (arguments[0]) {
            case "--verify" -> runVerification();
            case "--quadratic" -> runQuadraticDemo();
            case "--compare" -> runQuadraticComparison();
            case "--synthetic" -> {
                TrainingSession session = createSyntheticSession();
                compareCohortModels(session, new DecisionCosts(1.0, 1.0));
            }
            case "--real" -> {
                TrainingSession session = createRealSession(DEFAULT_CSV);
                compareCohortModels(session, new DecisionCosts(1.0, 1.0));
            }
            default -> throw new IllegalArgumentException(usage());
        }
    }

    private static String usage() {
        return "Use --verify, --quadratic, --compare, --synthetic, --real [CSV path], or no argument.";
    }

    private static void printHeader() {
        System.out.println("RetentionRadar - gradient descent and Nesterov educational prototype");
        System.out.println(COURSE);
        System.out.println(TEAM + " | " + MEMBERS);
        System.out.println("Topic: " + TOPIC);
        System.out.println(
                "Real-data task: eventual dropout versus graduation using first-semester information.");
    }

    private static void runInteractiveMenu() {
        TrainingSession session = createSyntheticSession();
        OptimizationResult latestTrainingResult = null;
        DecisionCosts decisionCosts = new DecisionCosts(1.0, 1.0);
        Scanner input = new Scanner(System.in);

        while (true) {
            printMenu(session.name());
            if (!input.hasNextLine()) {
                return;
            }
            String menuChoice = input.nextLine().trim();

            try {
                switch (menuChoice) {
                    case "1" -> {
                        session = createRealSession(DEFAULT_CSV);
                        latestTrainingResult = null;
                    }
                    case "2" -> {
                        session = createSyntheticSession();
                        latestTrainingResult = null;
                    }
                    case "3" -> latestTrainingResult = trainClassicalModel(session);
                    case "4" -> latestTrainingResult = trainAcceleratedModel(session);
                    case "5" -> latestTrainingResult = compareCohortModels(session, decisionCosts);
                    case "6" -> printRanking(session, latestTrainingResult, decisionCosts);
                    case "7" -> {
                        DecisionCosts enteredCosts = readDecisionCosts(input);
                        if (enteredCosts == null) {
                            return;
                        }
                        decisionCosts = enteredCosts;
                        System.out.printf(Locale.ROOT,
                                "Threshold is now %.6f; model weights were not changed.%n",
                                decisionCosts.threshold());
                        printRanking(session, latestTrainingResult, decisionCosts);
                    }
                    case "8" -> runQuadraticComparison();
                    case "9" -> runVerification();
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Enter a choice between 0 and 9.");
                }
            } catch (IOException | IllegalArgumentException exception) {
                System.out.println("Action could not be completed: " + exception.getMessage());
            }
        }
    }

    private static void printMenu(String cohortName) {
        System.out.println("\nCurrent cohort: " + cohortName);
        System.out.println("1  Load bundled UCI cohort");
        System.out.println("2  Load synthetic cohort");
        System.out.println("3  Train classical gradient descent");
        System.out.println("4  Train Nesterov accelerated gradient");
        System.out.println("5  Compare both and evaluate held-out records");
        System.out.println("6  Show held-out ranking (top 25)");
        System.out.println("7  Change costs and threshold without retraining");
        System.out.println("8  Quadratic comparison and ASCII contours");
        System.out.println("9  Run verification checks");
        System.out.println("0  Exit");
        System.out.print("Choice: ");
    }

    private static DecisionCosts readDecisionCosts(Scanner input) {
        System.out.print("Cost of missed positive outcome (positive decimal): ");
        if (!input.hasNextLine()) {
            return null;
        }
        double missedPositiveCost = Double.parseDouble(input.nextLine().trim());

        System.out.print("Cost of unnecessary outreach (positive decimal): ");
        if (!input.hasNextLine()) {
            return null;
        }
        double unnecessaryOutreachCost = Double.parseDouble(input.nextLine().trim());

        return new DecisionCosts(missedPositiveCost, unnecessaryOutreachCost);
    }

    private static TrainingSession createSyntheticSession() {
        System.out.println("\nSynthetic data: 600 fictional records, generation seed 202604.");
        Cohort cohort = SyntheticCohort.generate(SYNTHETIC_RECORD_COUNT, DATA_SEED);
        return prepareTrainingSession("Synthetic demonstration", cohort);
    }

    private static TrainingSession createRealSession(Path csvPath) throws IOException {
        LoadedCohort loadedCohort = StudentCsvLoader.load(csvPath);
        System.out.printf(Locale.ROOT,
                "\nUCI source rows: %d; omitted Enrolled: %d; retained: %d%n",
                loadedCohort.sourceRows(),
                loadedCohort.omittedEnrolled(),
                loadedCohort.cohort().labels().length);
        System.out.println(
                "Academic features are end-of-first-semester measurements; second-semester variables are excluded.");
        System.out.println(
                "Resolved Dropout/Graduate outcomes only: probabilities are conditional on this selected cohort.");
        return prepareTrainingSession("UCI first-semester selected cohort", loadedCohort.cohort());
    }

    private static TrainingSession prepareTrainingSession(String name, Cohort rawCohort) {
        Split dataSplit = DataSplit.stratified(rawCohort, TEST_FRACTION, DATA_SEED);
        Standardizer standardizer = Standardizer.fit(dataSplit.training().features());

        Cohort trainingCohort = standardizeCohort(dataSplit.training(), standardizer);
        Cohort testCohort = standardizeCohort(dataSplit.test(), standardizer);
        LogisticObjective objective = new LogisticObjective(
                trainingCohort.features(), trainingCohort.labels(), REGULARIZATION_STRENGTH);

        double learningRate = 1.0 / objective.smoothnessUpperBound();
        OptimizerConfig optimizerConfig = new OptimizerConfig(
                learningRate,
                GRADIENT_TOLERANCE,
                MAXIMUM_TRAINING_UPDATES,
                TRAINING_HISTORY_STRIDE);

        printDataPreparation(trainingCohort, testCohort, objective, optimizerConfig);
        return new TrainingSession(name, trainingCohort, testCohort, objective, optimizerConfig);
    }

    private static Cohort standardizeCohort(Cohort rawCohort, Standardizer standardizer) {
        double[][] standardizedFeatures = standardizer.transform(rawCohort.features());
        double[] unusedPlantedWeights = new double[rawCohort.featureNames().length];
        return new Cohort(
                standardizedFeatures,
                rawCohort.labels(),
                rawCohort.studentIds(),
                rawCohort.featureNames(),
                unusedPlantedWeights);
    }

    private static void printDataPreparation(
            Cohort trainingCohort,
            Cohort testCohort,
            LogisticObjective objective,
            OptimizerConfig optimizerConfig) {
        System.out.printf(Locale.ROOT,
                "Stratified split: training=%d; test=%d; split seed=%d%n",
                trainingCohort.labels().length, testCohort.labels().length, DATA_SEED);
        System.out.printf(Locale.ROOT,
                "Training positive-label rate=%.4f; held-out rate=%.4f%n",
                positiveLabelRate(trainingCohort), positiveLabelRate(testCohort));
        System.out.println("Standardization fitted only on training rows; bias remains one.");
        System.out.printf(Locale.ROOT,
                "Slope lambda=%.3f; conservative smoothness bound B=%.8f; alpha=%.8f%n",
                REGULARIZATION_STRENGTH,
                objective.smoothnessUpperBound(),
                optimizerConfig.learningRate());
    }

    private static double positiveLabelRate(Cohort cohort) {
        int[] labels = cohort.labels();
        int positiveCount = 0;
        for (int label : labels) {
            positiveCount += label;
        }
        return positiveCount / (double) labels.length;
    }

    private static double[] initialWeights(TrainingSession session) {
        return new double[session.objective().dimension()];
    }

    private static OptimizationResult trainClassicalModel(TrainingSession session) {
        OptimizationResult result = GradientDescent.minimize(
                session.objective(), initialWeights(session), session.optimizerConfig());
        printTrainingResult("Classical GD", result);
        evaluateHeldOutRecords(session, "Classical GD", result);
        return result;
    }

    private static OptimizationResult trainAcceleratedModel(TrainingSession session) {
        OptimizationResult result = NesterovGradientDescent.minimize(
                session.objective(), initialWeights(session), session.optimizerConfig());
        printTrainingResult("Nesterov", result);
        evaluateHeldOutRecords(session, "Nesterov", result);
        return result;
    }

    private static OptimizationResult compareCohortModels(
            TrainingSession session, DecisionCosts decisionCosts) {
        ComparisonReport.Comparison comparison = ComparisonReport.compare(
                session.objective(), initialWeights(session), session.optimizerConfig());
        ComparisonReport.print(comparison, session.optimizerConfig());

        System.out.println(
                "\nHeld-out evaluation at fixed threshold 0.5; test rows did not participate in fitting.");
        evaluateHeldOutRecords(session, "Classical GD", comparison.classical().result());
        evaluateHeldOutRecords(session, "Nesterov", comparison.accelerated().result());
        ComparisonReport.printProjectedContour(
                session.objective(), comparison, session.trainingCohort().featureNames());

        OptimizationResult rankingModel = selectRankingModel(comparison);
        printRanking(session, rankingModel, decisionCosts);
        return rankingModel;
    }

    private static OptimizationResult selectRankingModel(ComparisonReport.Comparison comparison) {
        OptimizationResult acceleratedResult = comparison.accelerated().result();
        if (acceleratedResult.stopReason() == StopReason.CONVERGED) {
            return acceleratedResult;
        }
        return comparison.classical().result();
    }

    private static void evaluateHeldOutRecords(
            TrainingSession session, String methodName, OptimizationResult result) {
        if (result.stopReason() == StopReason.DIVERGED) {
            System.out.println(methodName + ": no usable result; evaluation skipped.");
            return;
        }

        Evaluation.Metrics metrics = Evaluation.evaluate(
                session.objective(),
                result.point(),
                session.testCohort().features(),
                session.testCohort().labels(),
                EVALUATION_THRESHOLD);
        System.out.printf(Locale.ROOT,
                "%s: n=%d; accuracy=%s; precision=%s; recall=%s; F1=%s; AUC=%s%n",
                methodName, metrics.count(),
                formatMetric(metrics.accuracy()), formatMetric(metrics.precision()),
                formatMetric(metrics.recall()), formatMetric(metrics.f1()), formatMetric(metrics.auc()));
        System.out.printf(Locale.ROOT,
                "  TP=%d FP=%d TN=%d FN=%d; unpenalized log-loss=%s; Brier=%s%n",
                metrics.truePositive(), metrics.falsePositive(),
                metrics.trueNegative(), metrics.falseNegative(),
                formatMetric(metrics.logLoss()), formatMetric(metrics.brierScore()));
    }

    private static String formatMetric(double value) {
        if (!Double.isFinite(value)) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.5f", value);
    }

    private static void printRanking(
            TrainingSession session, OptimizationResult result, DecisionCosts decisionCosts) {
        if (result == null || result.stopReason() == StopReason.DIVERGED) {
            System.out.println("Train a usable model before displaying a ranking.");
            return;
        }
        System.out.println(
                "\nHeld-out record ranking. Costs are illustrative error costs, not measured intervention effects.");
        System.out.println("Threshold = unnecessaryOutreach / (unnecessaryOutreach + missedPositive).");
        Watchlist.print(
                session.testCohort(), session.objective(), result.point(),
                RANKING_LIMIT, decisionCosts.threshold());
    }

    private static void runQuadraticDemo() {
        QuadraticObjective objective = new QuadraticObjective();
        OptimizerConfig config = new OptimizerConfig(0.01, 1e-6, 10_000, 100);
        double[] initialPoint = {1.0, 1.0};

        System.out.println(
                "\nf(x,y)=0.5*(x^2+100*y^2); start=(1,1); minimum=(0,0); L=100; alpha=0.01.");
        OptimizationResult result = GradientDescent.minimize(objective, initialPoint, config);
        printTrainingResult("Classical GD", result);
    }

    private static void runQuadraticComparison() {
        QuadraticObjective objective = new QuadraticObjective(1000.0);
        OptimizerConfig config = new OptimizerConfig(0.001, 1e-6, 100_000, 25);
        double[] initialPoint = {1.0, 1.0};

        System.out.println(
                "\nf(x,y)=0.5*(x^2+1000*y^2); start=(1,1); minimum=(0,0); L=1000; mu=1.");
        ComparisonReport.Comparison comparison = ComparisonReport.compare(objective, initialPoint, config);
        ComparisonReport.print(comparison, config);
        AsciiContour.print(
                objective,
                comparison.classical().result().trace(),
                comparison.accelerated().result().trace(),
                70, 18);
    }

    private static void printTrainingResult(String methodName, OptimizationResult result) {
        System.out.printf(Locale.ROOT,
                "%s: %s; updates=%d; objective=%.12g; gradient norm=%.12g%n",
                methodName, result.stopReason(), result.iterations(), result.value(), result.gradientNorm());
        System.out.println("Point / weights: " + Arrays.toString(result.point()));
    }

    private static void runVerification() {
        SelfCheck.runAll();
        ModelCheck.runAll();
        AccelerationCheck.runAll();
        DataCheck.runAll();
        EvaluationCheck.runAll();
        verifyDecisionCosts();
        System.out.println("All project checks passed.");
    }

    private static void verifyDecisionCosts() {
        double lowerMissedCostThreshold = new DecisionCosts(9.0, 1.0).threshold();
        double higherMissedCostThreshold = new DecisionCosts(99.0, 1.0).threshold();
        boolean matchesExpectedThreshold = Math.abs(lowerMissedCostThreshold - 0.1) <= 1e-15;
        boolean higherMissedCostLowersThreshold = higherMissedCostThreshold < lowerMissedCostThreshold;

        if (!matchesExpectedThreshold || !higherMissedCostLowersThreshold) {
            throw new AssertionError("Cost threshold identity or direction failed.");
        }
        System.out.println("PASS: cost threshold identity and direction");

        double largeCostThreshold = new DecisionCosts(1e308, 1e308).threshold();
        if (!Double.isFinite(largeCostThreshold)) {
            throw new AssertionError("Cost threshold overflowed.");
        }
        System.out.println("PASS: cost threshold handles large finite values");
    }
}
