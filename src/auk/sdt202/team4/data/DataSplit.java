package auk.sdt202.team4.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import auk.sdt202.team4.data.SyntheticCohort.Cohort;

public final class DataSplit {
    private DataSplit() {
    }

    public static Split stratified(Cohort cohort, double testFraction, long seed) {
        validateSplitInputs(cohort, testFraction);
        double[][] features = cohort.features();
        int[] labels = cohort.labels();
        String[] studentIds = cohort.studentIds();
        List<Integer> negativeRowIndices = new ArrayList<>();
        List<Integer> positiveRowIndices = new ArrayList<>();
        groupRowsByLabel(labels, studentIds, negativeRowIndices, positiveRowIndices);

        Random random = new Random(seed);
        Collections.shuffle(negativeRowIndices, random);
        Collections.shuffle(positiveRowIndices, random);
        List<Integer> trainingIndices = new ArrayList<>();
        List<Integer> testIndices = new ArrayList<>();
        allocateClassRows(negativeRowIndices, testFraction, trainingIndices, testIndices);
        allocateClassRows(positiveRowIndices, testFraction, trainingIndices, testIndices);
        Collections.shuffle(trainingIndices, random);
        Collections.shuffle(testIndices, random);

        String[] featureNames = cohort.featureNames();
        double[] plantedWeights = cohort.plantedWeights();
        Cohort training = createSubset(
                trainingIndices, features, labels, studentIds, featureNames, plantedWeights);
        Cohort test = createSubset(testIndices, features, labels, studentIds, featureNames, plantedWeights);
        return new Split(training, test);
    }

    private static void validateSplitInputs(Cohort cohort, double testFraction) {
        if (cohort == null || !Double.isFinite(testFraction)
                || testFraction <= 0.0 || testFraction >= 1.0) {
            throw new IllegalArgumentException(
                    "A cohort and a test fraction strictly between zero and one are required.");
        }
    }

    private static void groupRowsByLabel(int[] labels, String[] studentIds,
            List<Integer> negativeRowIndices, List<Integer> positiveRowIndices) {
        Set<String> uniqueIds = new HashSet<>();
        for (int rowIndex = 0; rowIndex < labels.length; rowIndex++) {
            if (!uniqueIds.add(studentIds[rowIndex])) {
                throw new IllegalArgumentException("Student identifiers must be unique before splitting.");
            }
            if (labels[rowIndex] == 0) {
                negativeRowIndices.add(rowIndex);
            } else {
                positiveRowIndices.add(rowIndex);
            }
        }
        if (negativeRowIndices.size() < 2 || positiveRowIndices.size() < 2) {
            throw new IllegalArgumentException("Stratified splitting requires at least two students in each class.");
        }
    }

    private static void allocateClassRows(List<Integer> classIndices, double testFraction,
            List<Integer> trainingIndices, List<Integer> testIndices) {
        int requestedTestSize = (int) Math.round(classIndices.size() * testFraction);
        int maximumTestSize = classIndices.size() - 1;
        int limitedTestSize = Math.min(maximumTestSize, requestedTestSize);
        int testSize = Math.max(1, limitedTestSize);
        testIndices.addAll(classIndices.subList(0, testSize));
        trainingIndices.addAll(classIndices.subList(testSize, classIndices.size()));
    }

    private static Cohort createSubset(List<Integer> indices, double[][] features, int[] labels,
            String[] studentIds, String[] featureNames, double[] plantedWeights) {
        double[][] selectedFeatures = new double[indices.size()][];
        int[] selectedLabels = new int[indices.size()];
        String[] selectedIds = new String[indices.size()];
        for (int subsetRowIndex = 0; subsetRowIndex < indices.size(); subsetRowIndex++) {
            int sourceRowIndex = indices.get(subsetRowIndex);
            selectedFeatures[subsetRowIndex] = features[sourceRowIndex].clone();
            selectedLabels[subsetRowIndex] = labels[sourceRowIndex];
            selectedIds[subsetRowIndex] = studentIds[sourceRowIndex];
        }
        return new Cohort(selectedFeatures, selectedLabels, selectedIds, featureNames, plantedWeights);
    }

    public record Split(Cohort training, Cohort test) {
        public Split {
            if (training == null || test == null) {
                throw new IllegalArgumentException("Both training and test cohorts are required.");
            }
        }
    }
}
