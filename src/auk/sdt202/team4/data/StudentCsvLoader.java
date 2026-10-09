package auk.sdt202.team4.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import auk.sdt202.team4.data.SyntheticCohort.Cohort;

public final class StudentCsvLoader {
    private static final int ENROLLED_LABEL = -1;
    private static final String[] SELECTED_COLUMNS = {
        "Admission grade",
        "Age at enrollment",
        "Tuition fees up to date",
        "Scholarship holder",
        "Curricular units 1st sem (enrolled)",
        "Curricular units 1st sem (approved)",
        "Curricular units 1st sem (grade)"
    };

    private StudentCsvLoader() {
    }

    private enum FieldState {
        UNQUOTED,
        QUOTED,
        AFTER_QUOTE
    }

    public static LoadedCohort load(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("A CSV path is required.");
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> records = parse(text);
        if (records.size() < 2) {
            throw new IllegalArgumentException("CSV must contain a header and student rows.");
        }
        List<String> header = records.get(0);
        Map<String, Integer> columns = indexHeaderColumns(header);
        int[] selectedColumns = findSelectedColumns(columns);
        int targetColumn = requireColumn(columns, "Target");
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        List<String> identifiers = new ArrayList<>();
        int omittedEnrolled = 0;
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> row = records.get(rowIndex);
            if (row.size() != header.size()) {
                throw new IllegalArgumentException("CSV data row " + rowIndex + " has an incorrect column count.");
            }
            double[] featureRow = readFeatureRow(row, selectedColumns, rowIndex);
            int label = readOutcomeLabel(row.get(targetColumn), rowIndex);
            if (label == ENROLLED_LABEL) {
                omittedEnrolled++;
                continue;
            }
            features.add(featureRow);
            labels.add(label);
            identifiers.add(String.format(Locale.ROOT, "UCI-row%04d", rowIndex));
        }
        if (features.size() < 2) {
            throw new IllegalArgumentException("CSV must contain at least two Dropout or Graduate rows.");
        }
        Cohort cohort = createCohort(features, labels, identifiers);
        return new LoadedCohort(cohort, records.size() - 1, omittedEnrolled);
    }

    private static Map<String, Integer> indexHeaderColumns(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
            String columnName = header.get(columnIndex).trim();
            if (columnName.isEmpty() || columns.putIfAbsent(columnName, columnIndex) != null) {
                throw new IllegalArgumentException("CSV headers must be nonempty and unique: " + columnName);
            }
        }
        return columns;
    }

    private static int[] findSelectedColumns(Map<String, Integer> columns) {
        int[] selectedColumns = new int[SELECTED_COLUMNS.length];
        for (int featureIndex = 0; featureIndex < SELECTED_COLUMNS.length; featureIndex++) {
            selectedColumns[featureIndex] = requireColumn(columns, SELECTED_COLUMNS[featureIndex]);
        }
        return selectedColumns;
    }

    private static double[] readFeatureRow(List<String> row, int[] selectedColumns, int rowIndex) {
        double[] featureRow = new double[selectedColumns.length + 1];
        featureRow[0] = 1.0;
        for (int featureIndex = 0; featureIndex < selectedColumns.length; featureIndex++) {
            String cell = row.get(selectedColumns[featureIndex]).trim();
            double value = readFiniteNumber(cell, SELECTED_COLUMNS[featureIndex], rowIndex);
            boolean binaryFeature = featureIndex == 2 || featureIndex == 3;
            if (binaryFeature && value != 0.0 && value != 1.0) {
                throw new IllegalArgumentException("Binary field " + SELECTED_COLUMNS[featureIndex]
                        + " must be zero or one in data row " + rowIndex + ".");
            }
            featureRow[featureIndex + 1] = value;
        }
        return featureRow;
    }

    private static double readFiniteNumber(String cell, String columnName, int rowIndex) {
        double value;
        try {
            value = Double.parseDouble(cell);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number in data row " + rowIndex
                    + ", column " + columnName + ".", exception);
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Nonfinite number in data row " + rowIndex
                    + ", column " + columnName + ".");
        }
        return value;
    }

    private static int readOutcomeLabel(String cell, int rowIndex) {
        String target = cell.trim();
        if (target.equals("Dropout")) {
            return 1;
        }
        if (target.equals("Graduate")) {
            return 0;
        }
        if (target.equals("Enrolled")) {
            return ENROLLED_LABEL;
        }
        throw new IllegalArgumentException("Unknown Target in data row " + rowIndex + ": " + target);
    }

    private static Cohort createCohort(
            List<double[]> features, List<Integer> labels, List<String> identifiers) {
        int[] labelArray = new int[labels.size()];
        for (int rowIndex = 0; rowIndex < labels.size(); rowIndex++) {
            labelArray[rowIndex] = labels.get(rowIndex);
        }
        String[] featureNames = new String[SELECTED_COLUMNS.length + 1];
        featureNames[0] = "bias";
        System.arraycopy(SELECTED_COLUMNS, 0, featureNames, 1, SELECTED_COLUMNS.length);
        double[][] featureArray = features.toArray(double[][]::new);
        String[] identifierArray = identifiers.toArray(String[]::new);
        double[] plantedWeights = new double[featureNames.length];
        return new Cohort(featureArray, labelArray, identifierArray, featureNames, plantedWeights);
    }

    private static int requireColumn(Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Missing required CSV column: " + name);
        }
        return index;
    }

    private static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        FieldState state = FieldState.UNQUOTED;
        for (int characterIndex = 0; characterIndex < text.length(); characterIndex++) {
            char character = text.charAt(characterIndex);
            if (state == FieldState.QUOTED) {
                if (character == '"') {
                    if (characterIndex + 1 < text.length() && text.charAt(characterIndex + 1) == '"') {
                        field.append('"');
                        characterIndex++;
                    } else {
                        state = FieldState.AFTER_QUOTE;
                    }
                } else {
                    field.append(character);
                }
                continue;
            }
            if (character == ';' || character == '\r' || character == '\n') {
                row.add(field.toString());
                field.setLength(0);
                state = FieldState.UNQUOTED;
                if (character != ';') {
                    records.add(row);
                    row = new ArrayList<>();
                    if (character == '\r' && characterIndex + 1 < text.length()
                            && text.charAt(characterIndex + 1) == '\n') {
                        characterIndex++;
                    }
                }
            } else if (state == FieldState.AFTER_QUOTE) {
                if (character != ' ' && character != '\t') {
                    throw new IllegalArgumentException("Unexpected character after a quoted CSV field.");
                }
            } else if (character == '"') {
                if (field.length() != 0) {
                    throw new IllegalArgumentException("Unexpected quote in an unquoted CSV field.");
                }
                state = FieldState.QUOTED;
            } else {
                field.append(character);
            }
        }
        if (state == FieldState.QUOTED) {
            throw new IllegalArgumentException("CSV contains an unterminated quoted field.");
        }
        if (!row.isEmpty() || field.length() != 0 || state == FieldState.AFTER_QUOTE) {
            row.add(field.toString());
            records.add(row);
        }
        return records;
    }

    public record LoadedCohort(Cohort cohort, int sourceRows, int omittedEnrolled) {
        public LoadedCohort {
            if (cohort == null || sourceRows < 2 || omittedEnrolled < 0
                    || sourceRows - omittedEnrolled != cohort.labels().length) {
                throw new IllegalArgumentException("Loaded cohort counts must agree with its labelled rows.");
            }
        }
    }
}
