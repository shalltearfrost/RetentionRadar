package auk.sdt202.team4.app;

public record DecisionCosts(double missedPositive, double unnecessaryOutreach) {
    public DecisionCosts {
        if (!Double.isFinite(missedPositive) || missedPositive <= 0.0
                || !Double.isFinite(unnecessaryOutreach) || unnecessaryOutreach <= 0.0) {
            throw new IllegalArgumentException("Both demonstration costs must be positive and finite.");
        }
    }

    public double threshold() {
        if (missedPositive >= unnecessaryOutreach) {
            double unnecessaryToMissedRatio = unnecessaryOutreach / missedPositive;
            return unnecessaryToMissedRatio / (1.0 + unnecessaryToMissedRatio);
        }
        double missedToUnnecessaryRatio = missedPositive / unnecessaryOutreach;
        return 1.0 / (1.0 + missedToUnnecessaryRatio);
    }
}
