package auk.sdt202.team4.verify;

import auk.sdt202.team4.optimization.GradientDescent;
import auk.sdt202.team4.optimization.ObjectiveFunction;
import auk.sdt202.team4.optimization.OptimizationResult;
import auk.sdt202.team4.optimization.OptimizerConfig;
import auk.sdt202.team4.optimization.StopReason;
import auk.sdt202.team4.optimization.VectorMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SelfCheck {
    private SelfCheck() {
    }

    public static void main(String[] arguments) {
        runAll();
    }

    public static void runAll() {
        List<NamedCheck> checks = List.of(
                new NamedCheck(
                        "Known quadratic minimum",
                        SelfCheck::knownQuadraticMinimum),
                new NamedCheck(
                        "Optimal start needs zero updates",
                        SelfCheck::unchangedOptimalStart),
                new NamedCheck(
                        "Iteration limit counts actual updates",
                        SelfCheck::actualUpdateLimit),
                new NamedCheck(
                        "Convergence on the final allowed update",
                        SelfCheck::convergenceOnFinalAllowedUpdate),
                new NamedCheck(
                        "All coordinates use the same gradient",
                        SelfCheck::simultaneousCoordinateUpdate),
                new NamedCheck(
                        "Nonfinite objective and gradient stop safely",
                        SelfCheck::nonfiniteObjectiveAndGradient),
                new NamedCheck(
                        "Overflowing update stops safely",
                        SelfCheck::overflowingCoordinateUpdate),
                new NamedCheck(
                        "Finite loss magnitude guard",
                        SelfCheck::finiteMagnitudeDivergenceGuard),
                new NamedCheck(
                        "History stride includes initial and final states",
                        SelfCheck::sampledIterationHistory),
                new NamedCheck(
                        "History can be disabled",
                        SelfCheck::disabledIterationHistory),
                new NamedCheck(
                        "Inputs and results do not share mutable arrays",
                        SelfCheck::defensiveCopyIsolation),
                new NamedCheck(
                        "Invalid configuration and dimensions are rejected",
                        SelfCheck::invalidOptimizerInputs),
                new NamedCheck(
                        "Objective exceptions remain visible",
                        SelfCheck::objectiveFailuresRemainVisible),
                new NamedCheck(
                        "Vector norm avoids premature square overflow",
                        SelfCheck::stableEuclideanNorm));
        System.out.println("Classical gradient descent self-check");
        for (NamedCheck check : checks) {
            check.action().run();
            System.out.println("PASS: " + check.name());
        }
        System.out.println("All " + checks.size() + " checks passed.");
    }

    private static void knownQuadraticMinimum() {
        double[] initialPoint = {1.0, 1.0};
        OptimizationResult result = GradientDescent.minimize(
                diagonalQuadraticObjective(1.0, 100.0), initialPoint, new OptimizerConfig(0.01, 1e-8, 5_000, 100));
        require(result.stopReason() == StopReason.CONVERGED, "Quadratic should converge.");
        require(result.gradientNorm() <= 1e-8, "Converged gradient must meet tolerance.");
        require(VectorMath.norm(result.point()) <= 1e-8, "Quadratic minimum should be near zero.");
        require(result.value() <= 1e-15, "Quadratic loss should be near zero.");
        require(result.iterations() > 0 && result.iterations() < 5_000,
                "Quadratic should converge within its budget.");
        require(Arrays.equals(initialPoint, new double[] {1.0, 1.0}), "Initial point must remain unchanged.");
    }

    private static void unchangedOptimalStart() {
        OptimizationResult result = GradientDescent.minimize(
                diagonalQuadraticObjective(1.0, 100.0),
                new double[] {0.0, 0.0},
                new OptimizerConfig(0.01, 1e-8, 0, 3));
        require(result.stopReason() == StopReason.CONVERGED, "Optimal start should converge.");
        require(result.iterations() == 0, "Optimal start must not perform an update.");
        require(Arrays.equals(result.point(), new double[] {0.0, 0.0}), "Optimal point must be unchanged.");
        require(result.trace().size() == 1 && result.trace().get(0).iteration() == 0,
                "An optimal start should have one history entry.");
    }

    private static void actualUpdateLimit() {
        ObjectiveFunction objective = diagonalQuadraticObjective(1.0);
        OptimizationResult zeroBudgetResult = GradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-14, 0, 1));
        require(zeroBudgetResult.stopReason() == StopReason.MAX_ITERATIONS && zeroBudgetResult.iterations() == 0,
                "A zero budget must return without updates.");
        requireClose(zeroBudgetResult.point()[0], 1.0, 0.0, "Zero-budget point");
        OptimizationResult twoUpdateResult = GradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-14, 2, 1));
        require(twoUpdateResult.stopReason() == StopReason.MAX_ITERATIONS && twoUpdateResult.iterations() == 2,
                "A two-update budget must report exactly two updates.");
        requireClose(twoUpdateResult.point()[0], 0.81, 1e-14, "Point after two updates");
        requireClose(twoUpdateResult.value(), objective.value(twoUpdateResult.point()), 0.0, "Final objective value");
        requireClose(twoUpdateResult.gradientNorm(), 0.81, 1e-14, "Final gradient norm");
    }

    private static void convergenceOnFinalAllowedUpdate() {
        OptimizationResult result = GradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {3.0}, new OptimizerConfig(1.0, 1e-10, 1, 7));
        require(result.stopReason() == StopReason.CONVERGED && result.iterations() == 1,
                "A final update reaching the minimum must report convergence.");
        require(result.trace().size() == 2 && result.trace().get(1).iteration() == 1,
                "Final converged state must be recorded even between stride positions.");
    }

    private static void simultaneousCoordinateUpdate() {
        ObjectiveFunction objective = new ObjectiveFunction() {
            public int dimension() {
                return 2;
            }

            public double value(double[] point) {
                double coordinateSum = point[0] + point[1];
                return 0.5 * coordinateSum * coordinateSum + 0.5 * point[1] * point[1];
            }

            public double[] gradient(double[] point) {
                return new double[] {point[0] + point[1], point[0] + 2.0 * point[1]};
            }
        };
        OptimizationResult result = GradientDescent.minimize(
                objective, new double[] {1.0, 2.0}, new OptimizerConfig(0.1, 1e-14, 1, 1));
        requireClose(result.point()[0], 0.7, 1e-14, "Simultaneous first coordinate");
        requireClose(result.point()[1], 1.5, 1e-14, "Simultaneous second coordinate");
    }

    private static void nonfiniteObjectiveAndGradient() {
        for (double invalidNumber : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            ObjectiveFunction nonfiniteValueObjective = constantObjective(invalidNumber, 1.0);
            OptimizationResult nonfiniteValueResult = GradientDescent.minimize(
                    nonfiniteValueObjective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-8, 10, 1));
            require(nonfiniteValueResult.stopReason() == StopReason.DIVERGED && nonfiniteValueResult.iterations() == 0,
                    "Nonfinite objective must stop before updating.");
            OptimizationResult nonfiniteGradientResult = GradientDescent.minimize(
                    constantObjective(0.0, invalidNumber), new double[] {1.0}, new OptimizerConfig(0.1, 1e-8, 10, 1));
            require(nonfiniteGradientResult.stopReason() == StopReason.DIVERGED
                            && nonfiniteGradientResult.iterations() == 0,
                    "Nonfinite gradient must stop before updating.");
        }
    }

    private static void overflowingCoordinateUpdate() {
        OptimizationResult result = GradientDescent.minimize(
                constantObjective(0.0, Double.MAX_VALUE), new double[] {1.0},
                new OptimizerConfig(4.0, 1e-8, 10, 1));
        require(result.stopReason() == StopReason.DIVERGED && result.iterations() == 1,
                "Overflowing update must return divergence after one update.");
        require(!Double.isFinite(result.point()[0]), "Overflow case must produce a nonfinite coordinate.");
        require(result.trace().get(result.trace().size() - 1).iteration() == 1,
                "Overflow state must appear in the final trace entry.");
    }

    private static void finiteMagnitudeDivergenceGuard() {
        OptimizationResult result = GradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {1.0}, new OptimizerConfig(10.0, 1e-8, 100, 0));
        require(result.stopReason() == StopReason.DIVERGED && result.iterations() == 4,
                "Large fixed steps must trigger the finite magnitude guard.");
        require(Double.isFinite(result.value()) && VectorMath.allFinite(result.point()),
                "Magnitude guard should fire before floating-point overflow.");
        require(result.value() > 1_000_000.0 * (0.5 + 1.0), "Magnitude threshold must be exceeded.");
    }

    private static void sampledIterationHistory() {
        ObjectiveFunction objective = diagonalQuadraticObjective(1.0);
        OptimizationResult result = GradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 7, 3));
        int[] expectedIterations = {0, 3, 6, 7};
        require(result.trace().size() == expectedIterations.length, "Stride should avoid storing every update.");
        for (int traceEntryIndex = 0; traceEntryIndex < expectedIterations.length; traceEntryIndex++) {
            OptimizationResult.TraceEntry entry = result.trace().get(traceEntryIndex);
            require(entry.iteration() == expectedIterations[traceEntryIndex], "Unexpected history iteration.");
            requireClose(entry.value(), objective.value(entry.point()), 0.0, "Recorded objective value");
            requireClose(entry.gradientNorm(), VectorMath.norm(objective.gradient(entry.point())),
                    0.0, "Recorded gradient norm");
        }
        OptimizationResult exactStride = GradientDescent.minimize(
                objective, new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 6, 3));
        require(exactStride.trace().size() == 3,
                "A final stride position must not create a duplicate entry.");
        require(Arrays.equals(result.point(), result.trace().get(3).point()),
                "Final trace point must equal the returned point.");
    }

    private static void disabledIterationHistory() {
        OptimizationResult result = GradientDescent.minimize(
                diagonalQuadraticObjective(1.0), new double[] {1.0}, new OptimizerConfig(0.1, 1e-30, 7, 0));
        require(result.trace().isEmpty(), "Disabled history must not retain any iterates.");
    }

    private static void defensiveCopyIsolation() {
        double[] initialPoint = {1.0};
        OptimizationResult result = GradientDescent.minimize(
                diagonalQuadraticObjective(1.0), initialPoint, new OptimizerConfig(0.1, 1e-30, 3, 1));
        double savedFinalCoordinate = result.point()[0];
        initialPoint[0] = -999.0;
        double[] exposedResultPoint = result.point();
        exposedResultPoint[0] = 999.0;
        double[] exposedTracePoint = result.trace().get(0).point();
        exposedTracePoint[0] = 999.0;
        requireClose(result.point()[0], savedFinalCoordinate, 0.0, "Returned point copy");
        requireClose(result.trace().get(0).point()[0], 1.0, 0.0, "Trace point copy");
        expect(UnsupportedOperationException.class, () -> result.trace().clear());

        double[] point = {3.0};
        List<OptimizationResult.TraceEntry> mutableTrace = new ArrayList<>();
        mutableTrace.add(new OptimizationResult.TraceEntry(0, point, 4.5, 3.0));
        OptimizationResult constructedResult = new OptimizationResult(
                point, 4.5, 3.0, 0, StopReason.MAX_ITERATIONS, mutableTrace);
        point[0] = 999.0;
        mutableTrace.clear();
        requireClose(constructedResult.point()[0], 3.0, 0.0, "Constructor point copy");
        requireClose(constructedResult.trace().get(0).point()[0], 3.0, 0.0, "Constructor trace copy");
    }

    private static void invalidOptimizerInputs() {
        for (double invalidNumber : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> new OptimizerConfig(invalidNumber, 1e-8, 10, 0));
            expect(IllegalArgumentException.class, () -> new OptimizerConfig(0.1, invalidNumber, 10, 0));
        }
        expect(IllegalArgumentException.class, () -> new OptimizerConfig(0.1, 1e-8, -1, 0));
        expect(IllegalArgumentException.class, () -> new OptimizerConfig(0.1, 1e-8, 10, -1));
        OptimizerConfig config = new OptimizerConfig(0.1, 1e-8, 10, 0);
        ObjectiveFunction objective = diagonalQuadraticObjective(1.0);
        expect(NullPointerException.class, () -> GradientDescent.minimize(null, new double[] {1.0}, config));
        expect(NullPointerException.class, () -> GradientDescent.minimize(objective, null, config));
        expect(NullPointerException.class, () -> GradientDescent.minimize(objective, new double[] {1.0}, null));
        expect(IllegalArgumentException.class,
                () -> GradientDescent.minimize(objective, new double[] {1.0, 2.0}, config));
        expect(IllegalArgumentException.class,
                () -> GradientDescent.minimize(objective, new double[] {Double.NaN}, config));
        expect(IllegalArgumentException.class,
                () -> GradientDescent.minimize(objective, new double[] {Double.POSITIVE_INFINITY}, config));
        expect(IllegalArgumentException.class,
                () -> GradientDescent.minimize(diagonalQuadraticObjective(), new double[0], config));
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
                () -> GradientDescent.minimize(wrongGradient, new double[] {1.0}, config));
        expect(IllegalArgumentException.class,
                () -> VectorMath.dot(new double[] {1.0}, new double[] {1.0, 2.0}));
    }

    private static void objectiveFailuresRemainVisible() {
        ObjectiveFunction failingObjective = new ObjectiveFunction() {
            public int dimension() {
                return 1;
            }

            public double value(double[] point) {
                throw new IllegalStateException("Deliberate objective failure.");
            }

            public double[] gradient(double[] point) {
                return new double[] {1.0};
            }
        };
        expect(IllegalStateException.class,
                () -> GradientDescent.minimize(
                        failingObjective,
                        new double[] {1.0},
                        new OptimizerConfig(0.1, 1e-8, 10, 0)));
    }

    private static void stableEuclideanNorm() {
        double euclideanNorm = VectorMath.norm(new double[] {3e200, 4e200});
        require(Double.isFinite(euclideanNorm), "Finite representable norm must not overflow from squaring.");
        requireClose(euclideanNorm / 1e200, 5.0, 1e-14, "Stable Euclidean norm");
        requireClose(VectorMath.dot(new double[] {1.0, 2.0}, new double[] {3.0, 4.0}),
                11.0, 0.0, "Dot product");
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

    private static ObjectiveFunction constantObjective(double constantValue, double constantGradient) {
        return new ObjectiveFunction() {
            public int dimension() {
                return 1;
            }

            public double value(double[] point) {
                return constantValue;
            }

            public double[] gradient(double[] point) {
                return new double[] {constantGradient};
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
