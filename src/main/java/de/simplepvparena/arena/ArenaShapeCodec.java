package de.simplepvparena.arena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArenaShapeCodec {

    private static final String ROW_SEPARATOR = ":";
    private static final String RANGE_SEPARATOR = ",";
    private static final String BOUND_SEPARATOR = "..";

    private ArenaShapeCodec() {
    }

    public static List<String> encodeRows(long[] cells) {
        long[] sortedCells = cells.clone();
        Arrays.sort(sortedCells);
        List<String> rows = new ArrayList<>();
        int cellIndex = 0;
        while (cellIndex < sortedCells.length) {
            int rowX = HorizontalCellCodec.decodeX(sortedCells[cellIndex]);
            int rowEndIndex = cellIndex;
            while (rowEndIndex < sortedCells.length
                    && HorizontalCellCodec.decodeX(sortedCells[rowEndIndex]) == rowX) {
                rowEndIndex++;
            }
            rows.add(encodeRow(rowX, sortedCells, cellIndex, rowEndIndex));
            cellIndex = rowEndIndex;
        }
        return rows;
    }

    public static long[] decodeRows(List<String> rows) {
        List<Long> cells = new ArrayList<>();
        for (String row : rows) {
            decodeRow(row, cells);
        }
        long[] encodedCells = new long[cells.size()];
        for (int cellIndex = 0; cellIndex < encodedCells.length; cellIndex++) {
            encodedCells[cellIndex] = cells.get(cellIndex);
        }
        return encodedCells;
    }

    private static String encodeRow(int rowX, long[] sortedCells, int fromIndex, int toIndex) {
        StringBuilder row = new StringBuilder().append(rowX).append(ROW_SEPARATOR);
        int rangeStartZ = HorizontalCellCodec.decodeZ(sortedCells[fromIndex]);
        int rangeEndZ = rangeStartZ;
        boolean firstRange = true;
        for (int cellIndex = fromIndex + 1; cellIndex <= toIndex; cellIndex++) {
            int cellZ = cellIndex < toIndex ? HorizontalCellCodec.decodeZ(sortedCells[cellIndex]) : Integer.MAX_VALUE;
            if (cellIndex < toIndex && cellZ == rangeEndZ + 1) {
                rangeEndZ = cellZ;
                continue;
            }
            if (!firstRange) {
                row.append(RANGE_SEPARATOR);
            }
            row.append(rangeStartZ).append(BOUND_SEPARATOR).append(rangeEndZ);
            firstRange = false;
            rangeStartZ = cellZ;
            rangeEndZ = cellZ;
        }
        return row.toString();
    }

    private static void decodeRow(String row, List<Long> cells) {
        if (row == null) {
            throw new IllegalArgumentException("an arena shape row is missing");
        }
        int rowSeparatorIndex = row.indexOf(ROW_SEPARATOR);
        if (rowSeparatorIndex < 1) {
            throw new IllegalArgumentException("the arena shape row " + row + " has no x coordinate");
        }
        int rowX = parseCoordinate(row.substring(0, rowSeparatorIndex), row);
        String[] ranges = row.substring(rowSeparatorIndex + 1).split(RANGE_SEPARATOR, -1);
        for (String range : ranges) {
            int boundSeparatorIndex = range.indexOf(BOUND_SEPARATOR, 1);
            if (boundSeparatorIndex < 1) {
                throw new IllegalArgumentException("the arena shape row " + row + " holds the broken range " + range);
            }
            int fromZ = parseCoordinate(range.substring(0, boundSeparatorIndex), row);
            int toZ = parseCoordinate(range.substring(boundSeparatorIndex + BOUND_SEPARATOR.length()), row);
            if (toZ < fromZ) {
                throw new IllegalArgumentException("the arena shape row " + row + " holds the reversed range " + range);
            }
            for (int cellZ = fromZ; cellZ <= toZ; cellZ++) {
                cells.add(HorizontalCellCodec.encode(rowX, cellZ));
            }
        }
    }

    private static int parseCoordinate(String coordinate, String row) {
        try {
            return Integer.parseInt(coordinate.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("the arena shape row " + row + " holds the coordinate "
                    + coordinate + " which is not a number");
        }
    }
}
