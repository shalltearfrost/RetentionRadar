package auk.sdt202.team4.optimization;

public record OptimizerConfig(
        double learningRate,
        double tolerance,
        int maxIterations,
        int historyStride) {

    public OptimizerConfig {
        if (!Double.isFinite(learningRate) || learningRate <= 0.0) {
            throw new IllegalArgumentException("Learning rate must be positive and finite.");
        }
        if (!Double.isFinite(tolerance) || tolerance <= 0.0) {
            throw new IllegalArgumentException("Tolerance must be positive and finite.");
        }
        if (maxIterations < 0) {
            throw new IllegalArgumentException("Maximum iterations cannot be negative.");
        }
        if (historyStride < 0) {
            throw new IllegalArgumentException("History stride cannot be negative.");
        }
    }
}
