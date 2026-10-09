package auk.sdt202.team4.verify;

import auk.sdt202.team4.optimization.GradientDescent;
import auk.sdt202.team4.optimization.NesterovGradientDescent;
import auk.sdt202.team4.optimization.ObjectiveFunction;
import auk.sdt202.team4.optimization.OptimizationResult;
import auk.sdt202.team4.optimization.OptimizerConfig;
import auk.sdt202.team4.optimization.StopReason;
import auk.sdt202.team4.optimization.VectorMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public final class AccelerationCheck {
    private AccelerationCheck() {
    }

    public static void main(String[] arguments) {
        runAll();
    }

    public static void runAll() {
        List<NamedCheck> checks = List.of(
                new NamedCheck(
                        "Nesterov reaches a known quadratic minimum",
                        AccelerationCheck::knownQuadraticMinimum),
                new NamedCheck(
                        "Nesterov preserves an optimal start",
                        AccelerationCheck::unchangedOptimalStart),
                new NamedCheck(
                        "Nesterov reports actual updates and the final stop",
                        AccelerationCheck::actualUpdateLimit),
                new NamedCheck(
                        "Nesterov uses the gradient at lookahead",
                        AccelerationCheck::lookaheadGradient),
                new NamedCheck(
                        "Momentum uses floating-point division",
                        AccelerationCheck::floatingPointMomentumSchedule),
                new NamedCheck(
                        "Acceleration differs from classical GD",
                        AccelerationCheck::trajectoryDiffersFromClassical),
                new NamedCheck(
                        "Quadratic iterates satisfy the convex convergence bound",
                        AccelerationCheck::convexConvergenceBound),
                new NamedCheck(
                        "Finite objective increases remain allowed",
                        AccelerationCheck::finiteObjectiveIncreasesAllowed),
                new NamedCheck(
                        "Nesterov handles nonfinite ordinary and lookahead states",
                        AccelerationCheck::nonfiniteOrdinaryAndLookaheadStates),
                new NamedCheck(
                        "Nesterov handles overflowing updates and lookahead",
                        AccelerationCheck::overflowingOrdinaryAndLookaheadStates),
                new NamedCheck(
                        "Nesterov uses the finite magnitude guard",
                        AccelerationCheck::finiteMagnitudeDivergenceGuard),
                new NamedCheck(
                        "Nesterov records ordinary iterates with the requested stride",
                        AccelerationCheck::sampledOrdinaryIterationHistory),
                new NamedCheck(
                        "Nesterov preserves input and result copies",
                        AccelerationCheck::defensiveCopyIsolation),
                new NamedCheck(
                        "Nesterov rejects invalid inputs",
                        AccelerationCheck::invalidOptimizerInputs),
                new NamedCheck(
                        "Nesterov propagates objective failures",
                        AccelerationCheck::objectiveFailuresRemainVisible));
        System.out.println("Nesterov accelerated gradient descent self-check");
        for (NamedCheck check : checks) {
            check.action().run();
            System.out.println("PASS: " + check.name());
        }
        System.out.println("All " + checks.size() + " acceleration checks passed.");
    }

    private static void knownQuadraticMinimum() {
        OptimizationResult result = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0, 100.0),
                new double[] {1.0, 1.0},
                new OptimizerConfig(0.01, 1e-8, 10_000, 20));
        require(result.stopReason() == StopReason.CONVERGED, "Known quadratic should converge.");
        require(result.gradientNorm() <= 1e-8 && VectorMath.norm(result.point()) <= 1e-8,
                "Returned ordinary iterate must satisfy the tolerance.");
        require(result.value() <= 1e-15, "Known quadratic value should be near zero.");
    }

    private static void unchangedOptimalStart() {
        OptimizationResult result = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0, 100.0),
                new double[] {0.0, 0.0},
                new OptimizerConfig(0.01, 1e-8, 0, 3));
        require(result.stopReason() == StopReason.CONVERGED && result.iterations() == 0,
                "An optimal start requires no updates, including at a zero cap.");
        require(result.trace().size() == 1 && result.trace().get(0).iteration() == 0,
                "Optimal initial and final states must not be duplicated.");
    }

    private static void actualUpdateLimit() {
        OptimizationResult zeroBudgetResult = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 0, 1));
        require(zeroBudgetResult.stopReason() == StopReason.MAX_ITERATIONS && zeroBudgetResult.iterations() == 0,
                "A zero cap must not update the initial point.");
        requireClose(zeroBudgetResult.point()[0], 1.0, 0.0, "Zero-cap point");
        OptimizationResult cappedResult = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 3, 1));
        require(cappedResult.stopReason() == StopReason.MAX_ITERATIONS && cappedResult.iterations() == 3,
                "The cap must count actual ordinary updates.");
        requireClose(cappedResult.point()[0], 0.70875, 1e-14, "Capped ordinary point");
        requireClose(cappedResult.gradientNorm(), 0.70875, 1e-14, "Capped ordinary gradient norm");
        OptimizationResult convergedResult = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {3.0}, new OptimizerConfig(1.0, 1e-10, 1, 7));
        require(convergedResult.stopReason() == StopReason.CONVERGED && convergedResult.iterations() == 1,
                "Convergence on the final allowed update must be reported.");
        require(convergedResult.trace().size() == 2 && convergedResult.trace().get(1).iteration() == 1,
                "A final converged state between stride positions must be retained.");
    }

    private static void lookaheadGradient() {
        List<Double> evaluatedPoints = new ArrayList<>();
        ObjectiveFunction objective = singleCoordinateObjective(point -> 0.5 * point * point, point -> {
            evaluatedPoints.add(point);
            return point;
        });
        OptimizationResult result = NesterovGradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 3, 1));
        require(evaluatedPoints.stream().anyMatch(point -> Math.abs(point - 0.7875) < 1e-14),
                "The third update must evaluate the extrapolated point.");
        requireClose(result.point()[0], 0.70875, 1e-14, "Update from the lookahead gradient");
        requireClose(result.trace().get(2).point()[0], 0.81, 1e-14,
                "History must contain x2 rather than its lookahead point");
    }

    private static void floatingPointMomentumSchedule() {
        requireClose(NesterovGradientDescent.momentumCoefficient(1), 0.0, 0.0, "First momentum coefficient");
        requireClose(NesterovGradientDescent.momentumCoefficient(2), 0.25, 0.0, "Second momentum coefficient");
        double momentum = NesterovGradientDescent.momentumCoefficient(30);
        require(momentum > 0.9 && momentum < 1.0, "Momentum at update 30 must exceed 0.9.");
        requireClose(momentum, 29.0 / 32.0, 0.0, "Floating-point momentum schedule");
    }

    private static void trajectoryDiffersFromClassical() {
        ObjectiveFunction objective = diagonalQuadraticObjective(1.0);
        OptimizerConfig config = new OptimizerConfig(0.1, 1e-30, 3, 0);
        OptimizationResult classicalResult = GradientDescent.minimize(objective, new double[] {1.0}, config);
        OptimizationResult acceleratedResult = NesterovGradientDescent.minimize(objective, new double[] {1.0}, config);
        requireClose(classicalResult.point()[0], 0.729, 1e-14, "Classical third update");
        require(Math.abs(classicalResult.point()[0] - acceleratedResult.point()[0]) > 0.01,
                "Nonzero momentum must alter the actual algorithm trajectory.");
    }

    private static void convexConvergenceBound() {
        double[] initialPoint = {1.0, 1.0};
        double learningRate = 0.01;
        OptimizationResult result = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0, 100.0),
                initialPoint,
                new OptimizerConfig(learningRate, 1e-100, 800, 1));
        require(result.stopReason() != StopReason.DIVERGED, "A safe quadratic step must remain finite.");
        double distanceSquared = VectorMath.dot(initialPoint, initialPoint);
        for (OptimizationResult.TraceEntry entry : result.trace()) {
            double iterationOffset = entry.iteration() + 1.0;
            double bound = 2.0 * distanceSquared / (learningRate * iterationOffset * iterationOffset);
            require(entry.value() <= bound * (1.0 + 1e-12) + 1e-14,
                    "Convex theorem bound failed at ordinary update " + entry.iteration() + ".");
        }
        require(result.trace().size() > 100, "Bound check must cover many actual iterates.");
    }

    private static void finiteObjectiveIncreasesAllowed() {
        OptimizationResult result = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0, 100.0),
                new double[] {1.0, 1.0},
                new OptimizerConfig(0.01, 1e-100, 180, 1));
        boolean foundIncrease = false;
        for (int traceEntryIndex = 1; traceEntryIndex < result.trace().size(); traceEntryIndex++) {
            double previousLoss = result.trace().get(traceEntryIndex - 1).value();
            double currentLoss = result.trace().get(traceEntryIndex).value();
            if (currentLoss > previousLoss * (1.0 + 1e-10) + 1e-20) {
                foundIncrease = true;
                break;
            }
        }
        require(foundIncrease, "Fixture must demonstrate an actual finite objective increase.");
        require(result.stopReason() == StopReason.MAX_ITERATIONS && result.iterations() == 180,
                "Finite increases alone must not terminate the optimizer.");
    }

    private static void nonfiniteOrdinaryAndLookaheadStates() {
        OptimizerConfig config = new OptimizerConfig(0.1, 1e-30, 10, 1);
        for (double invalidNumber : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            OptimizationResult nonfiniteObjectiveResult = NesterovGradientDescent.minimize(
                    singleCoordinateObjective(point -> invalidNumber, point -> 1.0), new double[] {1.0}, config);
            require(nonfiniteObjectiveResult.stopReason() == StopReason.DIVERGED
                            && nonfiniteObjectiveResult.iterations() == 0,
                    "Nonfinite ordinary objective must stop before updating.");
            OptimizationResult nonfiniteGradientResult = NesterovGradientDescent.minimize(
                    singleCoordinateObjective(point -> 0.0, point -> invalidNumber), new double[] {1.0}, config);
            require(nonfiniteGradientResult.stopReason() == StopReason.DIVERGED
                            && nonfiniteGradientResult.iterations() == 0,
                    "Nonfinite ordinary gradient must stop before updating.");
        }
        OptimizationResult lookaheadValue = NesterovGradientDescent.minimize(
                singleCoordinateObjective(point -> point > 0.78 && point < 0.79 ? Double.NaN : 0.0, point -> point),
                new double[] {1.0}, config);
        require(lookaheadValue.stopReason() == StopReason.DIVERGED && lookaheadValue.iterations() == 2,
                "Nonfinite lookahead objective must stop before the third update.");
        requireClose(lookaheadValue.point()[0], 0.81, 1e-14, "Ordinary point after lookahead objective failure");
        OptimizationResult lookaheadGradient = NesterovGradientDescent.minimize(
                singleCoordinateObjective(point -> 0.0, point -> point > 0.78 && point < 0.79 ? Double.NaN : point),
                new double[] {1.0}, config);
        require(lookaheadGradient.stopReason() == StopReason.DIVERGED && lookaheadGradient.iterations() == 2,
                "Nonfinite lookahead gradient must stop before the third update.");
    }

    private static void overflowingOrdinaryAndLookaheadStates() {
        OptimizationResult overflowingUpdateResult = NesterovGradientDescent.minimize(
                singleCoordinateObjective(point -> 0.0, point -> Double.MAX_VALUE), new double[] {1.0},
                new OptimizerConfig(4.0, 1e-8, 10, 1));
        require(overflowingUpdateResult.stopReason() == StopReason.DIVERGED
                        && overflowingUpdateResult.iterations() == 1,
                "Overflowing ordinary update must return divergence.");
        double largeFiniteMagnitude = 0.9 * Double.MAX_VALUE;
        OptimizationResult lookahead = NesterovGradientDescent.minimize(
                singleCoordinateObjective(
                        point -> 0.0,
                        point -> point == 0.0 ? -largeFiniteMagnitude : largeFiniteMagnitude),
                new double[] {largeFiniteMagnitude},
                new OptimizerConfig(1.0, 1e-8, 10, 1));
        require(lookahead.stopReason() == StopReason.DIVERGED && lookahead.iterations() == 2,
                "Overflowing lookahead must stop before another update.");
        require(Double.isFinite(lookahead.point()[0]) && lookahead.value() == 0.0,
                "A lookahead failure must return the last ordinary iterate.");
    }

    private static void finiteMagnitudeDivergenceGuard() {
        OptimizationResult result = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {1.0}, new OptimizerConfig(10.0, 1e-8, 100, 0));
        require(result.stopReason() == StopReason.DIVERGED && result.iterations() > 0 && result.iterations() < 100,
                "An unstable large step should exceed the magnitude limit.");
        require(Double.isFinite(result.value()) && VectorMath.allFinite(result.point()),
                "Finite magnitude guard should stop before coordinate overflow.");
    }

    private static void sampledOrdinaryIterationHistory() {
        ObjectiveFunction objective = diagonalQuadraticObjective(1.0);
        OptimizationResult result = NesterovGradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 7, 3));
        int[] expectedIterations = {0, 3, 6, 7};
        require(result.trace().size() == expectedIterations.length, "History must respect its stride.");
        for (int traceEntryIndex = 0; traceEntryIndex < expectedIterations.length; traceEntryIndex++) {
            OptimizationResult.TraceEntry entry = result.trace().get(traceEntryIndex);
            require(entry.iteration() == expectedIterations[traceEntryIndex], "Unexpected recorded update number.");
            requireClose(entry.value(), objective.value(entry.point()), 0.0, "Recorded ordinary objective");
            requireClose(entry.gradientNorm(), VectorMath.norm(objective.gradient(entry.point())),
                    0.0, "Recorded ordinary gradient norm");
        }
        require(Arrays.equals(result.point(), result.trace().get(3).point()),
                "Final history point must equal the returned ordinary point.");
        OptimizationResult exactStride = NesterovGradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 6, 3));
        require(exactStride.trace().size() == 3, "Final stride position must not be duplicated.");
        OptimizationResult disabled = NesterovGradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 7, 0));
        require(disabled.trace().isEmpty(), "Disabled history must retain no iterates.");
    }

    private static void defensiveCopyIsolation() {
        double[] initialPoint = {1.0};
        OptimizationResult result = NesterovGradientDescent.minimize(
                diagonalQuadraticObjective(1.0), initialPoint, new OptimizerConfig(0.1, 1e-30, 3, 1));
        requireClose(initialPoint[0], 1.0, 0.0, "Initial point must remain unchanged");
        double finalPoint = result.point()[0];
        initialPoint[0] = 999.0;
        result.point()[0] = 999.0;
        result.trace().get(0).point()[0] = 999.0;
        requireClose(result.point()[0], finalPoint, 0.0, "Result copy isolation");
        requireClose(result.trace().get(0).point()[0], 1.0, 0.0, "Trace copy isolation");
        expect(UnsupportedOperationException.class, () -> result.trace().clear());
    }

    private static void invalidOptimizerInputs() {
        ObjectiveFunction objective = diagonalQuadraticObjective(1.0);
        OptimizerConfig config = new OptimizerConfig(0.1, 1e-8, 10, 0);
        expect(NullPointerException.class, () -> NesterovGradientDescent.minimize(null, new double[] {1.0}, config));
        expect(NullPointerException.class, () -> NesterovGradientDescent.minimize(objective, null, config));
        expect(NullPointerException.class,
                () -> NesterovGradientDescent.minimize(objective, new double[] {1.0}, null));
        expect(IllegalArgumentException.class,
                () -> NesterovGradientDescent.minimize(objective, new double[] {1.0, 2.0}, config));
        expect(IllegalArgumentException.class,
                () -> NesterovGradientDescent.minimize(diagonalQuadraticObjective(), new double[0], config));
        expect(IllegalArgumentException.class,
                () -> NesterovGradientDescent.minimize(objective, new double[] {Double.NaN}, config));
        expect(IllegalArgumentException.class,
                () -> NesterovGradientDescent.minimize(objective, new double[] {Double.POSITIVE_INFINITY}, config));
        expect(IllegalArgumentException.class, () -> NesterovGradientDescent.momentumCoefficient(0));
        ObjectiveFunction wrongGradient = new ObjectiveFunction() {
            public int dimension() {
                return 1;
            }

            public double value(double[] point) {
                return 0.0;
            }

            public double[] gradient(double[] point) {
                return new double[2];
            }
        };
        expect(IllegalArgumentException.class,
                () -> NesterovGradientDescent.minimize(wrongGradient, new double[] {1.0}, config));
        for (double invalidNumber : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> new OptimizerConfig(invalidNumber, 1e-8, 10, 0));
            expect(IllegalArgumentException.class, () -> new OptimizerConfig(0.1, invalidNumber, 10, 0));
        }
    }

    private static void objectiveFailuresRemainVisible() {
        ObjectiveFunction failingObjective = singleCoordinateObjective(point -> {
            throw new IllegalStateException("Deliberate objective failure.");
        }, point -> 1.0);
        expect(IllegalStateException.class, () -> NesterovGradientDescent.minimize(
                failingObjective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-8, 10, 0)));
    }

    private static ObjectiveFunction singleCoordinateObjective(
            DoubleUnaryOperator valueFunction,
            DoubleUnaryOperator gradientFunction) {
        return new ObjectiveFunction() {
            public int dimension() {
                return 1;
            }

            public double value(double[] point) {
                return valueFunction.applyAsDouble(point[0]);
            }

            public double[] gradient(double[] point) {
                return new double[] {gradientFunction.applyAsDouble(point[0])};
            }
        };
    }

    private static ObjectiveFunction diagonalQuadraticObjective(double... diagonalCoefficients) {
        double[] curvatureCoefficients = diagonalCoefficients.clone();
        return new ObjectiveFunction() {
            public int dimension() {
                return curvatureCoefficients.length;
            }

            public double value(double[] point) {
                double objectiveValue = 0.0;
                for (int coordinateIndex = 0; coordinateIndex < curvatureCoefficients.length; coordinateIndex++) {
                    objectiveValue += 0.5 * curvatureCoefficients[coordinateIndex]
                            * point[coordinateIndex] * point[coordinateIndex];
                }
                return objectiveValue;
            }

            public double[] gradient(double[] point) {
                double[] gradient = new double[curvatureCoefficients.length];
                for (int coordinateIndex = 0; coordinateIndex < curvatureCoefficients.length; coordinateIndex++) {
                    gradient[coordinateIndex] = curvatureCoefficients[coordinateIndex] * point[coordinateIndex];
                }
                return gradient;
            }
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireClose(double actualValue, double expectedValue, double tolerance, String label) {
        require(Double.isFinite(actualValue) && Math.abs(actualValue - expectedValue) <= tolerance,
                label + ": expected " + expectedValue + ", received " + actualValue + ".");
    }

    private static void expect(Class<? extends Throwable> expectedExceptionType, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (expectedExceptionType.isInstance(error)) {
                return;
            }
            throw new AssertionError("Expected " + expectedExceptionType.getSimpleName() + ", received "
                    + error.getClass().getSimpleName() + ".", error);
        }
        throw new AssertionError("Expected " + expectedExceptionType.getSimpleName() + " to be thrown.");
    }

    private record NamedCheck(String name, Runnable action) {
    }
}
