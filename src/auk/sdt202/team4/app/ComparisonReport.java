package auk.sdt202.team4.app;

import auk.sdt202.team4.model.LogisticObjective;
import auk.sdt202.team4.optimization.GradientDescent;
import auk.sdt202.team4.optimization.NesterovGradientDescent;
import auk.sdt202.team4.optimization.ObjectiveFunction;
import auk.sdt202.team4.optimization.OptimizationResult;
import auk.sdt202.team4.optimization.OptimizationResult.TraceEntry;
import auk.sdt202.team4.optimization.OptimizerConfig;
import auk.sdt202.team4.optimization.StopReason;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ComparisonReport {
    public record Run(String name, OptimizationResult result,
            long objectiveCalls, long gradientCalls) {
    }

    public record Comparison(Run classical, Run accelerated) {
    }

    private ComparisonReport() {
    }

    public static Comparison compare(ObjectiveFunction objective,
            double[] initialPoint, OptimizerConfig config) {
        CountingObjective classicalObjective = new CountingObjective(objective);
        OptimizationResult classicalResult = GradientDescent.minimize(
                classicalObjective, initialPoint, config);

        CountingObjective acceleratedObjective = new CountingObjective(objective);
        OptimizationResult acceleratedResult = NesterovGradientDescent.minimize(
                acceleratedObjective, initialPoint, config);

        Run classicalRun = new Run("Classical GD", classicalResult,
                classicalObjective.objectiveCalls, classicalObjective.gradientCalls);
        Run acceleratedRun = new Run("Nesterov", acceleratedResult,
                acceleratedObjective.objectiveCalls, acceleratedObjective.gradientCalls);
        return new Comparison(classicalRun, acceleratedRun);
    }

    public static void print(Comparison comparison, OptimizerConfig config) {
        System.out.printf(Locale.ROOT,
                "\nSame objective/start/step: alpha=%.8g; gradient tolerance=%.2g; update cap=%d%n",
                config.learningRate(), config.tolerance(), config.maxIterations());
        System.out.printf(Locale.ROOT, "%-14s %-15s %9s %15s %14s %10s %10s%n",
                "Method", "Stop", "Updates", "Objective", "Gradient norm", "f calls", "grad calls");
        printRun(comparison.classical());
        printRun(comparison.accelerated());
        System.out.println("Stopping and history use ordinary iterates; Nesterov also evaluates look-ahead gradients.");
        System.out.println("Update counts alone are not equal computational work; no universal measured speedup is claimed.");
        if (config.historyStride() > 0) {
            printStoredSamples(comparison, config.historyStride());
        }
    }

    private static void printRun(Run run) {
        OptimizationResult result = run.result();
        System.out.printf(Locale.ROOT, "%-14s %-15s %9d %15.9g %14.5g %10d %10d%n",
                run.name(), result.stopReason(), result.iterations(), result.value(),
                result.gradientNorm(), run.objectiveCalls(), run.gradientCalls());
    }

    private static void printStoredSamples(Comparison comparison, int historyStride) {
        System.out.println("Stored objective samples (dash means no sample at that update):");
        int[] sampleMultipliers = {0, 1, 2, 4, 8};
        for (int multiplier : sampleMultipliers) {
            long update = (long) historyStride * multiplier;
            String classicalSample = sample(comparison.classical().result().trace(), update);
            String acceleratedSample = sample(comparison.accelerated().result().trace(), update);
            System.out.printf(Locale.ROOT, "  update %6d: GD=%s; Nesterov=%s%n",
                    update, classicalSample, acceleratedSample);
        }
    }

    private static String sample(List<TraceEntry> trace, long update) {
        for (TraceEntry entry : trace) {
            if (entry.iteration() == update) {
                return String.format(Locale.ROOT, "%.9g", entry.value());
            }
        }
        return "-";
    }

    public static void printProjectedContour(LogisticObjective objective,
            Comparison comparison, String[] names) {
        double[] referenceWeights = comparison.accelerated().result().point();
        int xCoefficient = referenceWeights.length - 2;
        int yCoefficient = referenceWeights.length - 1;
        if (xCoefficient < 1
                || comparison.classical().result().stopReason() == StopReason.DIVERGED
                || comparison.accelerated().result().stopReason() == StopReason.DIVERGED) {
            return;
        }

        ObjectiveFunction coefficientSlice = createCoefficientSlice(
                objective, referenceWeights, xCoefficient, yCoefficient);
        System.out.println("\nCoefficient projection: x=" + names[xCoefficient]
                + "; y=" + names[yCoefficient]);
        System.out.println("Contours hold other weights at Nesterov's final values; paths project actual full-vector iterates.");
        System.out.println("A projected path is not a sequence of two-dimensional slice-optimization updates.");
        List<TraceEntry> classicalProjection = project(
                comparison.classical().result().trace(), xCoefficient, yCoefficient);
        List<TraceEntry> acceleratedProjection = project(
                comparison.accelerated().result().trace(), xCoefficient, yCoefficient);
        AsciiContour.print(coefficientSlice, classicalProjection, acceleratedProjection, 70, 18);
    }

    private static ObjectiveFunction createCoefficientSlice(LogisticObjective objective,
            double[] referenceWeights, int xCoefficient, int yCoefficient) {
        return new ObjectiveFunction() {
            @Override
            public int dimension() {
                return 2;
            }

            @Override
            public double value(double[] projectedPoint) {
                return objective.value(fullWeights(projectedPoint));
            }

            @Override
            public double[] gradient(double[] projectedPoint) {
                double[] fullGradient = objective.gradient(fullWeights(projectedPoint));
                return new double[] {fullGradient[xCoefficient], fullGradient[yCoefficient]};
            }

            private double[] fullWeights(double[] projectedPoint) {
                double[] weights = referenceWeights.clone();
                weights[xCoefficient] = projectedPoint[0];
                weights[yCoefficient] = projectedPoint[1];
                return weights;
            }
        };
    }

    private static List<TraceEntry> project(List<TraceEntry> trace,
            int xCoefficient, int yCoefficient) {
        List<TraceEntry> projectedTrace = new ArrayList<>();
        for (TraceEntry entry : trace) {
            double[] fullPoint = entry.point();
            double[] projectedPoint = {fullPoint[xCoefficient], fullPoint[yCoefficient]};
            projectedTrace.add(new TraceEntry(entry.iteration(), projectedPoint,
                    entry.value(), entry.gradientNorm()));
        }
        return List.copyOf(projectedTrace);
    }

    private static final class CountingObjective implements ObjectiveFunction {
        private final ObjectiveFunction delegate;
        private long objectiveCalls;
        private long gradientCalls;

        private CountingObjective(ObjectiveFunction delegate) {
            this.delegate = delegate;
        }

        @Override
        public int dimension() {
            return delegate.dimension();
        }

        @Override
        public double value(double[] point) {
            objectiveCalls++;
            return delegate.value(point);
        }

        @Override
        public double[] gradient(double[] point) {
            gradientCalls++;
            return delegate.gradient(point);
        }
    }
}
