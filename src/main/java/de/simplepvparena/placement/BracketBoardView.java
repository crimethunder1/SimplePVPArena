package de.simplepvparena.placement;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
public record BracketBoardView(Component header, int headerLineCount, List<Component> columns, int columnLineCount) {

    public BracketBoardView {
        Objects.requireNonNull(header, "header");
        columns = List.copyOf(columns);
    }
}
