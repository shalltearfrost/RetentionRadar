package auk.sdt202.team4.data;

public final class Standardizer {
    private final double[] means;
    private final double[] scales;

    private Standardizer(double[] means, double[] scales) {
        this.means = means.clone();
        this.scales = scales.clone();
    }

    public static Standardizer fit(double[][] training) {
        int dimension = requireRows(training, -1);
        double[] means = new double[dimension];
        double[] scales = new double[dimension];
        scales[0] = 1.0;
        for (int featureIndex = 1; featureIndex < dimension; featureIndex++) {
            if (isConstantFeature(training, featureIndex)) {
                means[featureIndex] = training[0][featureIndex];
                scales[featureIndex] = 1.0;
                continue;
            }
            means[featureIndex] = featureMean(training, featureIndex);
            double variance = featureVariance(training, featureIndex, means[featureIndex]);
            scales[featureIndex] = Math.sqrt(variance);
            if (scales[featureIndex] == 0.0) {
                scales[featureIndex] = 1.0;
            }
            if (!Double.isFinite(means[featureIndex]) || !Double.isFinite(scales[featureIndex])) {
                throw new IllegalArgumentException("Training feature magnitudes are too large to standardize.");
            }
        }
        return new Standardizer(means, scales);
    }

    public double[][] transform(double[][] rows) {
        requireRows(rows, means.length);
        double[][] transformed = new double[rows.length][means.length];
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            transformed[rowIndex][0] = 1.0;
            for (int featureIndex = 1; featureIndex < means.length; featureIndex++) {
                transformed[rowIndex][featureIndex] =
                        (rows[rowIndex][featureIndex] - means[featureIndex]) / scales[featureIndex];
                if (!Double.isFinite(transformed[rowIndex][featureIndex])) {
                    throw new IllegalArgumentException("Feature magnitudes are too large for this standardizer.");
                }
            }
        }
        return transformed;
    }

    public double[] means() {
        return means.clone();
    }

    public double[] scales() {
        return scales.clone();
    }

    private static boolean isConstantFeature(double[][] rows, int featureIndex) {
        double firstValue = rows[0][featureIndex];
        for (double[] row : rows) {
            if (row[featureIndex] != firstValue) {
                return false;
            }
        }
        return true;
    }

    private static double featureMean(double[][] rows, int featureIndex) {
        double mean = 0.0;
        for (double[] row : rows) {
            mean += row[featureIndex] / rows.length;
        }
        return mean;
    }

    private static double featureVariance(double[][] rows, int featureIndex, double mean) {
        double variance = 0.0;
        for (double[] row : rows) {
            double deviation = row[featureIndex] - mean;
            variance += deviation * deviation / rows.length;
        }
        return variance;
    }

    private static int requireRows(double[][] rows, int expectedDimension) {
        if (rows == null || rows.length == 0 || rows[0] == null || rows[0].length < 2) {
            throw new IllegalArgumentException("Standardization requires rows with a bias and at least one feature.");
        }
        int dimension = rows[0].length;
        if (expectedDimension >= 0 && dimension != expectedDimension) {
            throw new IllegalArgumentException("Feature dimension does not match the fitted standardizer.");
        }
        for (double[] row : rows) {
            if (row == null || row.length != dimension || row[0] != 1.0) {
                throw new IllegalArgumentException("Feature rows must have matching dimensions and a bias of one.");
            }
            for (double value : row) {
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("Standardizer features must be finite.");
                }
            }
        }
        return dimension;
    }
}
