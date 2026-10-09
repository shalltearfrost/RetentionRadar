package auk.sdt202.team4.app;

import auk.sdt202.team4.optimization.ObjectiveFunction;
import auk.sdt202.team4.optimization.OptimizationResult.TraceEntry;
import java.util.List;
import java.util.Locale;

public final class AsciiContour {
    private AsciiContour() {
    }

    public static void print(ObjectiveFunction objective, List<TraceEntry> classical,
            List<TraceEntry> accelerated, int width, int height) {
        if (objective.dimension() != 2 || width < 10 || height < 5) {
            throw new IllegalArgumentException("Contour requires a two-dimensional objective and a useful grid size.");
        }

        PlotBounds bounds = findBounds(classical, accelerated);
        ObjectiveLevels levels = sampleObjective(objective, bounds, width, height);
        char[][] grid = shadeGrid(levels, width, height);
        overlay(grid, bounds, classical, 'G');
        overlay(grid, bounds, accelerated, 'N');
        markOriginAndStart(grid, bounds, classical);
        printGrid(grid, bounds);
    }

    private static PlotBounds findBounds(List<TraceEntry> classical,
            List<TraceEntry> accelerated) {
        PlotBounds bounds = new PlotBounds();
        include(bounds, classical);
        include(bounds, accelerated);
        double marginX = Math.max(0.1, 0.1 * (bounds.maximumX - bounds.minimumX));
        double marginY = Math.max(0.1, 0.1 * (bounds.maximumY - bounds.minimumY));
        bounds.minimumX -= marginX;
        bounds.maximumX += marginX;
        bounds.minimumY -= marginY;
        bounds.maximumY += marginY;
        return bounds;
    }

    private static void include(PlotBounds bounds, List<TraceEntry> trace) {
        for (TraceEntry entry : trace) {
            double[] point = entry.point();
            if (point.length != 2 || !Double.isFinite(point[0]) || !Double.isFinite(point[1])) {
                throw new IllegalArgumentException("Contour traces must contain finite two-dimensional points.");
            }
            bounds.minimumX = Math.min(bounds.minimumX, point[0]);
            bounds.maximumX = Math.max(bounds.maximumX, point[0]);
            bounds.minimumY = Math.min(bounds.minimumY, point[1]);
            bounds.maximumY = Math.max(bounds.maximumY, point[1]);
        }
    }

    private static ObjectiveLevels sampleObjective(ObjectiveFunction objective,
            PlotBounds bounds, int width, int height) {
        double[][] values = new double[height][width];
        double minimumLevel = Double.POSITIVE_INFINITY;
        double maximumLevel = Double.NEGATIVE_INFINITY;
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                double x = bounds.minimumX
                        + column * (bounds.maximumX - bounds.minimumX) / (width - 1);
                double y = bounds.maximumY
                        - row * (bounds.maximumY - bounds.minimumY) / (height - 1);
                double level = Math.log1p(Math.max(0.0, objective.value(new double[] {x, y})));
                if (!Double.isFinite(level)) {
                    throw new IllegalArgumentException("Contour objective must be finite on the displayed grid.");
                }
                values[row][column] = level;
                minimumLevel = Math.min(minimumLevel, level);
                maximumLevel = Math.max(maximumLevel, level);
            }
        }
        return new ObjectiveLevels(values, minimumLevel, maximumLevel);
    }

    private static char[][] shadeGrid(ObjectiveLevels levels, int width, int height) {
        String palette = " .:-=+*#%@";
        char[][] grid = new char[height][width];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int shade = 0;
                if (levels.maximum() != levels.minimum()) {
                    shade = (int) Math.round((levels.values()[row][column] - levels.minimum())
                            / (levels.maximum() - levels.minimum()) * (palette.length() - 1));
                }
                grid[row][column] = palette.charAt(shade);
            }
        }
        return grid;
    }

    private static void overlay(char[][] grid, PlotBounds bounds,
            List<TraceEntry> trace, char marker) {
        for (TraceEntry entry : trace) {
            double[] point = entry.point();
            int column = map(point[0], bounds.minimumX, bounds.maximumX, grid[0].length);
            int row = grid.length - 1 - map(point[1], bounds.minimumY, bounds.maximumY, grid.length);
            char previousMarker = grid[row][column];
            if (previousMarker == 'X' || (marker == 'N' && previousMarker == 'G')) {
                grid[row][column] = 'X';
            } else {
                grid[row][column] = marker;
            }
        }
    }

    private static void markOriginAndStart(char[][] grid, PlotBounds bounds,
            List<TraceEntry> classical) {
        int height = grid.length;
        int width = grid[0].length;
        int originColumn = map(0.0, bounds.minimumX, bounds.maximumX, width);
        int originRow = height - 1 - map(0.0, bounds.minimumY, bounds.maximumY, height);
        grid[originRow][originColumn] = 'O';
        if (!classical.isEmpty()) {
            double[] start = classical.get(0).point();
            int startRow = height - 1 - map(start[1], bounds.minimumY, bounds.maximumY, height);
            int startColumn = map(start[0], bounds.minimumX, bounds.maximumX, width);
            grid[startRow][startColumn] = 'S';
        }
    }

    private static void printGrid(char[][] grid, PlotBounds bounds) {
        System.out.println("\nObjective contours and sampled actual optimizer iterates");
        System.out.println("G=classical, N=Nesterov, X=both, O=origin, S=start; shades use log(1+objective).");
        System.out.printf(Locale.ROOT,
                "x: [%.3g, %.3g], left to right; y: [%.3g, %.3g], bottom to top%n",
                bounds.minimumX, bounds.maximumX, bounds.minimumY, bounds.maximumY);
        for (char[] row : grid) {
            System.out.println(new String(row));
        }
    }

    private static int map(double value, double minimum, double maximum, int size) {
        int position = (int) Math.round((value - minimum) / (maximum - minimum) * (size - 1));
        return Math.max(0, Math.min(size - 1, position));
    }

    private static final class PlotBounds {
        private double minimumX;
        private double maximumX;
        private double minimumY;
        private double maximumY;
    }

    private record ObjectiveLevels(double[][] values, double minimum, double maximum) {
    }
}
