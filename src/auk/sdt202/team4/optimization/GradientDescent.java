package auk.sdt202.team4.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GradientDescent {
    private GradientDescent() {
    }

    public static OptimizationResult minimize(
            ObjectiveFunction objective,
            double[] initialPoint,
            OptimizerConfig config) {
        Objects.requireNonNull(objective, "Objective cannot be null.");
        Objects.requireNonNull(initialPoint, "Initial point cannot be null.");
        Objects.requireNonNull(config, "Configuration cannot be null.");
        int dimension = objective.dimension();
        if (dimension <= 0 || initialPoint.length != dimension) {
            throw new IllegalArgumentException("Initial point must match a positive objective dimension.");
        }
        if (!VectorMath.allFinite(initialPoint)) {
            throw new IllegalArgumentException("Initial coordinates must be finite.");
        }

        double[] currentPoint = initialPoint.clone();
        double initialObjectiveValue = objective.value(currentPoint);
        double objectiveMagnitudeLimit = 1_000_000.0 * (Math.abs(initialObjectiveValue) + 1.0);
        List<OptimizationResult.TraceEntry> history = new ArrayList<>();
        int completedUpdates = 0;
        double objectiveValue = initialObjectiveValue;

        while (true) {
            if (!VectorMath.allFinite(currentPoint)
                    || !Double.isFinite(objectiveValue)
                    || objectiveValue > objectiveMagnitudeLimit) {
                return finishOptimization(
                        currentPoint, objectiveValue, Double.NaN,
                        completedUpdates, StopReason.DIVERGED, config, history);
            }

            double[] currentGradient = objective.gradient(currentPoint);
            if (currentGradient == null || currentGradient.length != dimension) {
                throw new IllegalArgumentException("Gradient must match the objective dimension.");
            }
            double currentGradientNorm = VectorMath.norm(currentGradient);
            if (!VectorMath.allFinite(currentGradient) || !Double.isFinite(currentGradientNorm)) {
                return finishOptimization(
                        currentPoint, objectiveValue, currentGradientNorm,
                        completedUpdates, StopReason.DIVERGED, config, history);
            }

            if (config.historyStride() > 0 && completedUpdates % config.historyStride() == 0) {
                recordTraceEntry(history, completedUpdates, currentPoint, objectiveValue, currentGradientNorm);
            }

            if (currentGradientNorm <= config.tolerance()) {
                return finishOptimization(
                        currentPoint, objectiveValue, currentGradientNorm,
                        completedUpdates, StopReason.CONVERGED, config, history);
            }
            if (completedUpdates == config.maxIterations()) {
                return finishOptimization(
                        currentPoint, objectiveValue, currentGradientNorm,
                        completedUpdates, StopReason.MAX_ITERATIONS, config, history);
            }

            currentPoint = VectorMath.axpy(-config.learningRate(), currentGradient, currentPoint);
            completedUpdates++;
            objectiveValue = VectorMath.allFinite(currentPoint) ? objective.value(currentPoint) : Double.NaN;
        }
    }

    private static OptimizationResult finishOptimization(
            double[] currentPoint,
            double objectiveValue,
            double currentGradientNorm,
            int completedUpdates,
            StopReason stopReason,
            OptimizerConfig config,
            List<OptimizationResult.TraceEntry> history) {
        if (config.historyStride() > 0) {
            recordTraceEntry(history, completedUpdates, currentPoint, objectiveValue, currentGradientNorm);
        }
        return new OptimizationResult(
                currentPoint, objectiveValue, currentGradientNorm,
                completedUpdates, stopReason, history);
    }

    private static void recordTraceEntry(
            List<OptimizationResult.TraceEntry> history,
            int iteration,
            double[] currentPoint,
            double objectiveValue,
            double currentGradientNorm) {
        if (history.isEmpty() || history.get(history.size() - 1).iteration() != iteration) {
            history.add(new OptimizationResult.TraceEntry(
                    iteration, currentPoint, objectiveValue, currentGradientNorm));
        }
    }
}
