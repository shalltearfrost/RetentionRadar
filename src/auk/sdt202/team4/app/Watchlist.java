package auk.sdt202.team4.app;

import auk.sdt202.team4.data.SyntheticCohort.Cohort;
import auk.sdt202.team4.model.LogisticObjective;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Watchlist {
    private record Entry(String identifier, double probability,
            String firstFeature, double firstContribution,
            String secondFeature, double secondContribution) {
    }

    private Watchlist() {
    }

    public static void print(Cohort cohort, LogisticObjective objective,
            double[] weights, int limit) {
        print(cohort, objective, weights, limit, 0.0);
    }

    public static void print(Cohort cohort, LogisticObjective objective,
            double[] weights, int limit, double threshold) {
        if (limit < 0) {
            throw new IllegalArgumentException("Ranking limit must be nonnegative.");
        }
        if (!Double.isFinite(threshold) || threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be finite and between zero and one.");
        }

        List<Entry> entries = createEntries(cohort, objective, weights);
        entries.sort(Comparator.comparingDouble(Entry::probability).reversed()
                .thenComparing(Entry::identifier));
        entries.removeIf(entry -> entry.probability() < threshold);
        printEntries(entries, limit, threshold);
    }

    private static List<Entry> createEntries(Cohort cohort,
            LogisticObjective objective, double[] weights) {
        double[][] features = cohort.features();
        String[] identifiers = cohort.studentIds();
        String[] featureNames = cohort.featureNames();
        List<Entry> entries = new ArrayList<>();
        for (int row = 0; row < features.length; row++) {
            int largestContributionColumn = 1;
            int secondLargestContributionColumn = -1;
            for (int column = 2; column < weights.length; column++) {
                double contribution = weights[column] * features[row][column];
                if (contribution > weights[largestContributionColumn]
                        * features[row][largestContributionColumn]) {
                    secondLargestContributionColumn = largestContributionColumn;
                    largestContributionColumn = column;
                } else if (secondLargestContributionColumn < 0
                        || contribution > weights[secondLargestContributionColumn]
                        * features[row][secondLargestContributionColumn]) {
                    secondLargestContributionColumn = column;
                }
            }

            entries.add(new Entry(identifiers[row], objective.probability(weights, features[row]),
                    featureNames[largestContributionColumn],
                    weights[largestContributionColumn] * features[row][largestContributionColumn],
                    secondLargestContributionColumn >= 0
                            ? featureNames[secondLargestContributionColumn] : null,
                    secondLargestContributionColumn >= 0
                            ? weights[secondLargestContributionColumn]
                            * features[row][secondLargestContributionColumn] : 0.0));
        }
        return entries;
    }

    private static void printEntries(List<Entry> entries, int limit, double threshold) {
        System.out.printf(Locale.ROOT,
                "\nModel score ranking: %d records meet threshold %.4f; showing up to %d.%n",
                entries.size(), threshold, limit);
        System.out.println("Contributions change model log-odds; they are not causal explanations.");
        System.out.printf(Locale.ROOT, "%-12s %8s  %s%n",
                "Record", "Score", "Two largest signed feature contributions");
        int shownCount = Math.min(limit, entries.size());
        for (int index = 0; index < shownCount; index++) {
            printEntry(entries.get(index));
        }
    }

    private static void printEntry(Entry entry) {
        System.out.printf(Locale.ROOT, "%-12s %7.2f%%  %s=%+.3f",
                entry.identifier(), 100.0 * entry.probability(),
                entry.firstFeature(), entry.firstContribution());
        if (entry.secondFeature() != null) {
            System.out.printf(Locale.ROOT, "; %s=%+.3f",
                    entry.secondFeature(), entry.secondContribution());
        }
        System.out.println();
    }
}
