package auk.sdt202.team4.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import auk.sdt202.team4.data.DataSplit;
import auk.sdt202.team4.data.Standardizer;
import auk.sdt202.team4.data.StudentCsvLoader;
import auk.sdt202.team4.data.SyntheticCohort;
import auk.sdt202.team4.data.SyntheticCohort.Cohort;

public final class DataCheck {
    private static final String HEADER = "Admission grade;Age at enrollment;Tuition fees up to date;Scholarship holder;"
            + "Curricular units 1st sem (enrolled);Curricular units 1st sem (approved);"
            + "Curricular units 1st sem (grade);Target";
    private static final String VALID_ROWS = "150;20;1;0;6;4;12;Dropout\n"
            + "170;19;1;1;6;6;16;Graduate\n";

    private DataCheck() {
    }

    public static void main(String[] arguments) {
        runAll();
    }

    public static void runAll() {
        runCheck("CSV BOM, quoted fields, schema and outcome filtering", DataCheck::checkLoader);
        runCheck("CSV rejects invalid schema, malformed fields and bad data", DataCheck::checkInvalidCsv);
        runCheck("Stratified split is deterministic, disjoint and complete", DataCheck::checkSplit);
        runCheck("Standardization fits training rows and protects the bias and copies", DataCheck::checkStandardizer);
        System.out.println("All 4 data checks passed.");
    }

    private static void checkLoader() throws IOException {
        String csv = "\uFEFF\"Admission grade\";Age at enrollment;Tuition fees up to date;Scholarship holder;"
                + "Curricular units 1st sem (enrolled);Curricular units 1st sem (approved);"
                + "Curricular units 1st sem (grade);Target;Gender;Nationality;"
                + "Curricular units 2nd sem (grade);Notes\r\n"
                + "\"150\";20;1;0;6;4;12;\"Dropout\";1;1;99;\"semicolon; and \"\"quoted\"\" text\"\r\n"
                + "140;21;0;0;6;3;10;Enrolled;0;2;88;unused\r\n"
                + "170;19;1;1;6;6;16;Graduate;0;3;77;unused\r\n";
        Path fixture = Files.createTempFile("retention-data-check-", ".csv");
        try {
            Files.writeString(fixture, csv, StandardCharsets.UTF_8);
            byte[] original = Files.readAllBytes(fixture);
            StudentCsvLoader.LoadedCohort loaded = StudentCsvLoader.load(fixture);
            requireCondition(loaded.sourceRows() == 3 && loaded.omittedEnrolled() == 1,
                    "Source and omitted counts differ.");
            Cohort cohort = loaded.cohort();
            requireCondition(Arrays.equals(cohort.labels(), new int[] {1, 0}), "Target mapping differs.");
            String[] expectedIdentifiers = {"UCI-row0001", "UCI-row0003"};
            requireCondition(Arrays.equals(cohort.studentIds(), expectedIdentifiers),
                    "Source-row identifiers differ.");
            requireCondition(cohort.featureNames().length == 8,
                    "Selected schema must contain bias and seven features.");
            requireCondition(cohort.featureNames()[1].equals("Admission grade")
                    && cohort.featureNames()[7].equals("Curricular units 1st sem (grade)"),
                    "Selected feature order differs.");
            requireCondition(cohort.features()[0][0] == 1.0 && cohort.features()[0][1] == 150.0
                    && cohort.features()[1][7] == 16.0, "Selected feature values differ.");
            for (String featureName : cohort.featureNames()) {
                requireCondition(!featureName.equals("Gender") && !featureName.equals("Nationality")
                        && !featureName.contains("2nd sem"), "Excluded feature was selected.");
            }
            double[][] exposed = cohort.features();
            exposed[0][1] = -999.0;
            requireCondition(cohort.features()[0][1] == 150.0, "Loaded cohort exposed its stored feature matrix.");
            requireCondition(Arrays.equals(original, Files.readAllBytes(fixture)), "Loader modified its source file.");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void checkInvalidCsv() throws IOException {
        expectInvalidCsv(HEADER.replace("Admission grade", "Admission score") + "\n" + VALID_ROWS);
        expectInvalidCsv(HEADER + ";Target\n" + VALID_ROWS.replace("Dropout\n", "Dropout;Dropout\n")
                .replace("Graduate\n", "Graduate;Graduate\n"));
        expectInvalidCsv(HEADER + "\n150;20;1;0;6;4;Dropout\n" + VALID_ROWS);
        expectInvalidCsv(HEADER + "\n" + VALID_ROWS.replace("150;", "invalid;"));
        expectInvalidCsv(HEADER + "\n" + VALID_ROWS.replace("150;", "NaN;"));
        expectInvalidCsv(HEADER + "\n" + VALID_ROWS.replace("150;", "Infinity;"));
        expectInvalidCsv(HEADER + "\n" + VALID_ROWS.replace("20;1;0;", "20;2;0;"));
        expectInvalidCsv(HEADER + "\n" + VALID_ROWS.replace("20;1;0;", "20;1;2;"));
        expectInvalidCsv(HEADER + "\n" + VALID_ROWS.replace("Dropout", "Unknown"));
        expectInvalidCsv(HEADER + "\n\"150;20;1;0;6;4;12;Dropout\n" + VALID_ROWS);
        expectInvalidCsv(HEADER + "\n\"150\"x;20;1;0;6;4;12;Dropout\n" + VALID_ROWS);
    }

    private static void expectInvalidCsv(String csv) throws IOException {
        Path fixture = Files.createTempFile("retention-invalid-data-", ".csv");
        try {
            Files.writeString(fixture, csv, StandardCharsets.UTF_8);
            try {
                StudentCsvLoader.load(fixture);
            } catch (IllegalArgumentException expected) {
                return;
            }
            throw new AssertionError("Invalid CSV was accepted.");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void checkSplit() {
        Cohort cohort = SyntheticCohort.generate(200, 731L);
        DataSplit.Split firstSplit = DataSplit.stratified(cohort, 0.25, 149L);
        DataSplit.Split repeatedSplit = DataSplit.stratified(cohort, 0.25, 149L);
        requireCondition(Arrays.equals(firstSplit.training().studentIds(), repeatedSplit.training().studentIds())
                && Arrays.equals(firstSplit.test().studentIds(), repeatedSplit.test().studentIds()),
                "Split is not reproducible.");
        Set<String> assignedIdentifiers = new HashSet<>();
        for (String identifier : firstSplit.training().studentIds()) {
            requireCondition(assignedIdentifiers.add(identifier), "Training subset contains a duplicate row.");
        }
        for (String identifier : firstSplit.test().studentIds()) {
            requireCondition(assignedIdentifiers.add(identifier), "Train and test rows overlap.");
        }
        Set<String> originalIdentifiers = new HashSet<>(Arrays.asList(cohort.studentIds()));
        requireCondition(assignedIdentifiers.equals(originalIdentifiers), "Split did not conserve every source row.");
        requireBothClasses(firstSplit.training());
        requireBothClasses(firstSplit.test());
        int originalPositiveCount = countPositiveLabels(cohort.labels());
        int trainingPositiveCount = countPositiveLabels(firstSplit.training().labels());
        int testPositiveCount = countPositiveLabels(firstSplit.test().labels());
        requireCondition(trainingPositiveCount + testPositiveCount == originalPositiveCount,
                "Split changed class labels.");
        double[][] exposedFeatures = firstSplit.training().features();
        exposedFeatures[0][1] = 999.0;
        requireCondition(firstSplit.training().features()[0][1] != 999.0,
                "Split exposes a mutable feature matrix.");
        Cohort undersizedCohort = new Cohort(new double[][] {{1, 0}, {1, 1}}, new int[] {0, 1},
                new String[] {"a", "b"}, new String[] {"bias", "feature"}, new double[] {0, 0});
        expectInvalidInput(() -> DataSplit.stratified(undersizedCohort, 0.25, 149L));
        expectInvalidInput(() -> DataSplit.stratified(cohort, 0.0, 149L));
        expectInvalidInput(() -> DataSplit.stratified(cohort, Double.NaN, 149L));
    }

    private static void checkStandardizer() {
        double[][] training = {{1, 2, 5}, {1, 4, 5}};
        Standardizer standardizer = Standardizer.fit(training);
        double[][] transformed = standardizer.transform(training);
        requireCondition(standardizer.means()[1] == 3.0 && standardizer.scales()[1] == 1.0,
                "Training mean or scale differs.");
        requireCondition(standardizer.means()[0] == 0.0 && standardizer.scales()[0] == 1.0,
                "Bias was included in centering.");
        requireCondition(standardizer.scales()[2] == 1.0 && transformed[0][2] == 0.0
                && transformed[1][2] == 0.0, "Constant feature was not handled safely.");
        requireCondition(transformed[0][0] == 1.0 && transformed[1][0] == 1.0
                && transformed[0][1] == -1.0 && transformed[1][1] == 1.0, "Training transformation differs.");
        double[][] test = {{1, 100, 5}};
        requireCondition(standardizer.transform(test)[0][1] == 97.0, "Test data influenced the fitted training mean.");
        requireCondition(training[0][1] == 2.0 && test[0][1] == 100.0, "Transformation changed input data.");
        training[0][1] = 999.0;
        double[] means = standardizer.means();
        double[] scales = standardizer.scales();
        means[1] = 1000.0;
        scales[1] = 1000.0;
        requireCondition(standardizer.transform(test)[0][1] == 97.0, "Fitted state is externally mutable.");
        transformed[0][1] = 999.0;
        requireCondition(standardizer.transform(test)[0][1] == 97.0, "Transformed data aliases fitted state.");
        double[][] decimalConstant = new double[100][2];
        for (double[] row : decimalConstant) {
            row[0] = 1.0;
            row[1] = 0.1;
        }
        Standardizer constantStandardizer = Standardizer.fit(decimalConstant);
        requireCondition(constantStandardizer.scales()[1] == 1.0
                && constantStandardizer.transform(decimalConstant)[0][1] == 0.0,
                "Rounding gave a constant decimal feature a spurious variance.");
        expectInvalidInput(() -> standardizer.transform(new double[][] {{1, 2}}));
        expectInvalidInput(() -> Standardizer.fit(new double[][] {{0, 2}, {1, 3}}));
        expectInvalidInput(() -> Standardizer.fit(new double[][] {{1, Double.NaN}, {1, 3}}));
    }

    private static void requireBothClasses(Cohort cohort) {
        int positiveCount = countPositiveLabels(cohort.labels());
        requireCondition(positiveCount > 0 && positiveCount < cohort.labels().length,
                "Subset does not contain both classes.");
    }

    private static int countPositiveLabels(int[] labels) {
        int result = 0;
        for (int label : labels) {
            result += label;
        }
        return result;
    }

    private static void expectInvalidInput(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid data input was accepted.");
    }

    private static void requireCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void runCheck(String name, Check check) {
        try {
            check.run();
        } catch (Exception | AssertionError failure) {
            throw new AssertionError("FAIL: " + name + ": " + failure.getMessage(), failure);
        }
        System.out.println("PASS: " + name);
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }
}
