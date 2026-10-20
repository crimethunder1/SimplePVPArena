package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ArenaShapeCalculation {

    public enum Status {
        FILLED,
        SEED_INSIDE_BORDER,
        CELL_LIMIT_REACHED
    }

    private final Status status;
    private final ArenaShape shape;
    private final String rejectionMessage;
    private final ArenaFillDiagnostics diagnostics;

    private ArenaShapeCalculation(Status status, ArenaShape shape, String rejectionMessage,
                                  ArenaFillDiagnostics diagnostics) {
        this.status = status;
        this.shape = shape;
        this.rejectionMessage = rejectionMessage;
        this.diagnostics = diagnostics;
    }

    public static ArenaShapeCalculation filled(ArenaShape shape) {
        return new ArenaShapeCalculation(Status.FILLED, shape, null, null);
    }

    public static ArenaShapeCalculation seedInsideBorder() {
        return new ArenaShapeCalculation(Status.SEED_INSIDE_BORDER, null,
                Messages.text(ArenaGapMessage.SEED_INSIDE_BORDER), null);
    }

    public static ArenaShapeCalculation stoppedAtCellLimit(int maximumCellCount,
                                                           ArenaFillDiagnostics diagnostics) {
        Placeholder path = Placeholder.of("path", ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH);
        Placeholder calculateAction = Placeholder.of("action", Arena.describeCalculateAction());
        Placeholder boundingBox = Placeholder.of("boundingBox", diagnostics.describeBoundingBox());
        Placeholder front = Placeholder.of("front", diagnostics.describeFront());
        List<String> lines = new ArrayList<>();
        lines.add(Messages.plural(ArenaGapMessage.STOPPED_AT_CELL_LIMIT, maximumCellCount, path));
        describeHeightMismatch(diagnostics, calculateAction).ifPresent(lines::add);
        lines.add(Messages.text(ArenaGapMessage.GAP_OR_TOO_LARGE));
        if (diagnostics.hasSpread()) {
            lines.add(Messages.text(ArenaGapMessage.SPREAD,
                    Placeholder.of("direction", diagnostics.describeEscapeDirection()), boundingBox, front));
            lines.add(Messages.text(ArenaGapMessage.WALK_ALONG_TOWARDS,
                    Placeholder.of("direction", diagnostics.getEscapeDirectionName()), path, calculateAction));
        } else {
            lines.add(Messages.text(ArenaGapMessage.NOT_SPREAD, boundingBox, front));
            lines.add(Messages.text(ArenaGapMessage.WALK_ALONG, path, calculateAction));
        }
        lines.add(Messages.plural(ArenaGapMessage.DIAGNOSE_HINT, ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT,
                Placeholder.of("action", Arena.describeDiagnoseAction())));
        return new ArenaShapeCalculation(Status.CELL_LIMIT_REACHED, null,
                String.join(TournamentMessages.LINE_BREAK, lines), diagnostics);
    }

    private static Optional<String> describeHeightMismatch(ArenaFillDiagnostics diagnostics,
                                                           Placeholder calculateAction) {
        if (!diagnostics.hasHeightMismatch()) {
            return Optional.empty();
        }
        int mismatchCount = diagnostics.getHeightMismatchColumnCount();
        int listedCount = Math.min(ArenaFillDiagnostics.REPORTED_HEIGHT_MISMATCH_COUNT, mismatchCount);
        Placeholder columns = Placeholder.of("columns", diagnostics.describeHeightMismatchColumns());
        String finding = listedCount == mismatchCount
                ? Messages.plural(ArenaGapMessage.HEIGHT_MISMATCH_ALL_LISTED, mismatchCount, columns)
                : Messages.text(ArenaGapMessage.HEIGHT_MISMATCH_NEAREST_LISTED, Placeholder.of("count", mismatchCount),
                        Placeholder.of("listedCount", listedCount), columns);
        return Optional.of(finding + TournamentMessages.LINE_BREAK
                + Messages.text(ArenaGapMessage.HEIGHT_MISMATCH_EXPLANATION, calculateAction));
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccessful() {
        return status == Status.FILLED;
    }

    public Optional<ArenaShape> getShape() {
        return Optional.ofNullable(shape);
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }

    public Optional<ArenaFillDiagnostics> getDiagnostics() {
        return Optional.ofNullable(diagnostics);
    }
}
