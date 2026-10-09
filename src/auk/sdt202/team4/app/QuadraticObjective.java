package auk.sdt202.team4.app;

import auk.sdt202.team4.optimization.ObjectiveFunction;

public final class QuadraticObjective implements ObjectiveFunction {
    private final double curvature;

    public QuadraticObjective() {
        this(100.0);
    }

    public QuadraticObjective(double curvature) {
        if (!Double.isFinite(curvature) || curvature < 1.0) {
            throw new IllegalArgumentException("Curvature must be finite and at least one.");
        }
        this.curvature = curvature;
    }

    @Override
    public int dimension() {
        return 2;
    }

    @Override
    public double value(double[] point) {
        return 0.5 * (point[0] * point[0] + curvature * point[1] * point[1]);
    }

    @Override
    public double[] gradient(double[] point) {
        return new double[] {point[0], curvature * point[1]};
    }
}
