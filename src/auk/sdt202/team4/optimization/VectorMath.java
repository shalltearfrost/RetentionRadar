package auk.sdt202.team4.optimization;

import java.util.Objects;

public final class VectorMath {
    private VectorMath() {
    }

    public static double dot(double[] left, double[] right) {
        requireMatchingDimensions(left, right);
        double dotProduct = 0.0;
        for (int coordinateIndex = 0; coordinateIndex < left.length; coordinateIndex++) {
            dotProduct += left[coordinateIndex] * right[coordinateIndex];
        }
        return dotProduct;
    }

    public static double norm(double[] vector) {
        Objects.requireNonNull(vector, "Vector cannot be null.");
        double euclideanNorm = 0.0;
        for (double value : vector) {
            euclideanNorm = Math.hypot(euclideanNorm, value);
        }
        return euclideanNorm;
    }

    public static double[] axpy(double scale, double[] scaledVector, double[] baseVector) {
        requireMatchingDimensions(scaledVector, baseVector);
        double[] updatedVector = new double[scaledVector.length];
        for (int coordinateIndex = 0; coordinateIndex < scaledVector.length; coordinateIndex++) {
            updatedVector[coordinateIndex] = baseVector[coordinateIndex] + scale * scaledVector[coordinateIndex];
        }
        return updatedVector;
    }

    public static boolean allFinite(double[] vector) {
        Objects.requireNonNull(vector, "Vector cannot be null.");
        for (double value : vector) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static void requireMatchingDimensions(double[] left, double[] right) {
        Objects.requireNonNull(left, "Left vector cannot be null.");
        Objects.requireNonNull(right, "Right vector cannot be null.");
        if (left.length != right.length) {
            throw new IllegalArgumentException("Vector dimensions must match.");
        }
    }
}
