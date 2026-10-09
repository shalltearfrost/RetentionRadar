package auk.sdt202.team4.optimization;

import java.util.List;
import java.util.Objects;

public final class OptimizationResult {
    private final double[] point;
    private final double value;
    private final double gradientNorm;
    private final int iterations;
    private final StopReason stopReason;
    private final List<TraceEntry> trace;

    public OptimizationResult(
            double[] point,
            double value,
            double gradientNorm,
            int iterations,
            StopReason stopReason,
            List<TraceEntry> trace) {
        this.point = Objects.requireNonNull(point, "Point cannot be null.").clone();
        if (iterations < 0) {
            throw new IllegalArgumentException("Iterations cannot be negative.");
        }
        this.value = value;
        this.gradientNorm = gradientNorm;
        this.iterations = iterations;
        this.stopReason = Objects.requireNonNull(stopReason, "Stop reason cannot be null.");
        this.trace = List.copyOf(Objects.requireNonNull(trace, "Trace cannot be null."));
    }

    public double[] point() {
        return point.clone();
    }

    public double value() {
        return value;
    }

    public double gradientNorm() {
        return gradientNorm;
    }

    public int iterations() {
        return iterations;
    }

    public StopReason stopReason() {
        return stopReason;
    }

    public List<TraceEntry> trace() {
        return trace;
    }

    public static final class TraceEntry {
        private final int iteration;
        private final double[] point;
        private final double value;
        private final double gradientNorm;

        public TraceEntry(
                int iteration,
                double[] point,
                double value,
                double gradientNorm) {
            if (iteration < 0) {
                throw new IllegalArgumentException("Iteration cannot be negative.");
            }
            this.iteration = iteration;
            this.point = Objects.requireNonNull(point, "Point cannot be null.").clone();
            this.value = value;
            this.gradientNorm = gradientNorm;
        }

        public int iteration() {
            return iteration;
        }

        public double[] point() {
            return point.clone();
        }

        public double value() {
            return value;
        }

        public double gradientNorm() {
            return gradientNorm;
        }
    }
}
